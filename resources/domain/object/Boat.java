package resources.domain.object;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.ai.BoatPatrolBehavior;
import resources.domain.entity.component.AIComponent;
import resources.domain.entity.component.TerrainSpeedComponent;
import resources.domain.player.Moveable;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;
import resources.input.InputHandlingSystem;

/**
 * Self-propelled vessel that drifts across water tiles. Logically a
 * {@link Moveable}, but rendering bypasses the playable sheet — the boat has
 * its own 8-direction sprite set (N, NE, E, SE, S, SW, W, NW) loaded from
 * resources/images/objects/boat/<dir>.png with procedural fallbacks when no
 * art exists yet.
 *
 * Two operating modes:
 *   - Unmanned: {@link AIComponent} runs {@link BoatPatrolBehavior}, which
 *     drifts the boat over water at random.
 *   - Ridden by the player: AI is detached, the boat reads
 *     {@link InputHandlingSystem} every tick, supports all 8 directions, and
 *     refuses to step onto non-water tiles.
 *
 * Player position is slaved to the boat while ridden; pressing SPACE
 * dismounts to an adjacent land tile if one is reachable, otherwise on the
 * nearest beach/grass cell scanned outward.
 */
public final class Boat extends Moveable {

    // Pirate-ship art is roughly square (bow points up by default), so the
    // sprite footprint is square too. Hitbox is a bit smaller to forgive
    // pixel-fringe collisions against shorelines.
    private static final short WIDTH         = 192;
    private static final short HEIGHT        = 192;
    private static final short HITBOX_WIDTH  = 144;
    private static final short HITBOX_HEIGHT = 144;
    private static final short HITBOX_REL_X  = (short) ((WIDTH  - HITBOX_WIDTH)  / 2);
    private static final short HITBOX_REL_Y  = (short) ((HEIGHT - HITBOX_HEIGHT) / 2);

    private static final double BOAT_SPEED        = 4.0;
    /** Maximum tile distance from the boat at which a shore tile counts as
     *  "adjacent" for dismount. Two tiles ≈ a player's body length out from
     *  the boat — close enough that hopping off looks like stepping onto the
     *  beach, far enough that you don't need to be pixel-perfect against the
     *  shore. Anything beyond this means "you're in open water" and dismount
     *  is refused. */
    private static final int DISMOUNT_SHORE_RADIUS_TILES = 2;
    /** Once a shoreline tile is found, push the player this many additional
     *  tiles further inland (in the same direction) so they land on solid
     *  ground rather than the surf-line — dropping the player right at the
     *  shore edge often left their hitbox straddling water. */
    private static final int DISMOUNT_INLAND_STEPS = 2;
    private static final double DIAGONAL_FACTOR   = 0.7071; // 1/sqrt(2)
    private static final double BOARDING_RADIUS   = 192.0;  // pixels — "close proximity" for clicks
    private static final String[] DIR_NAMES       = {"e","se","s","sw","w","nw","n","ne"};

    /**
     * Mapping from compass direction → starter-ship filename in
     * resources/images/objects/ships/starterShip/. Numbers correspond to the
     * "(N).png" suffix on the ChatGPT-generated sprites. South and SW have no
     * dedicated art, so the loader derives them by vertically flipping the
     * north / NE variants — see {@link #loadDirectionalImages()}.
     */
    private static final String SHIP_DIR = "resources/images/objects/ships/starterShip/";
    private static final String[] SHIP_FILES = {
        "ChatGPT Image 26. mai 2026, 20_35_01 (3).png", // 0 e
        "ChatGPT Image 26. mai 2026, 20_35_04 (7).png", // 1 se
        null,                                            // 2 s  — flip north vertically
        null,                                            // 3 sw — flip ne vertically
        "ChatGPT Image 26. mai 2026, 20_35_01 (4).png", // 4 w
        "ChatGPT Image 26. mai 2026, 20_35_04 (6).png", // 5 nw
        "ChatGPT Image 26. mai 2026, 20_35_00 (1).png", // 6 n
        "ChatGPT Image 26. mai 2026, 20_35_03 (5).png"  // 7 ne
    };

    /** Shared sprite cache — all Boat instances reuse the same 8 images. */
    private static volatile ArrayList<BufferedImage> SHARED_DIRECTIONAL_IMAGES;

    private final ArrayList<BufferedImage> directionalImages;
    /** Last non-zero heading index into DIR_NAMES so a stopped boat keeps facing where it was. */
    private int facingIndex = 0;
    private Playable rider;

    /**
     * Default constructor: a wild, drifting boat — used by {@link resources.domain.spawn.BoatSpawner}
     * for the patrol boats that decorate ocean tiles at world-gen.
     */
    public Boat(GamePanel panel, int worldX, int worldY) {
        this(panel, worldX, worldY, true);
    }

    /**
     * @param autoPatrol attach a {@link BoatPatrolBehavior} so the boat
     *     drifts on its own. Set to false for player-placed boats (so a
     *     freshly placed boat sits still until the player boards it) and
     *     for the inventory/preview templates (which never tick).
     */
    public Boat(GamePanel panel, int worldX, int worldY, boolean autoPatrol) {
        super(panel, "boat", worldX, worldY,
            WIDTH, HEIGHT, HITBOX_WIDTH, HITBOX_HEIGHT,
            HITBOX_REL_X, HITBOX_REL_Y);
        this.solid = true;
        setMovementSpeed(BOAT_SPEED);

        directionalImages = loadDirectionalImages();
        // Mirror images into the entity's image list so legacy paths still find one.
        this.images = directionalImages;

        addComponent(new TerrainSpeedComponent(buildTerrainTable()));
        if (autoPatrol) {
            addComponent(new AIComponent((GameContext) panel, new BoatPatrolBehavior(System.nanoTime())));
        }
    }

    /**
     * Cheap pre-flight: would a Boat placed at {@code (worldX, worldY)} fit
     * here without colliding with anything solid? Mirrors what
     * {@code placeEntity} checks, but without constructing the Boat (which
     * costs nothing now that sprites are cached, but the hitbox/component
     * setup is still wasted work if placement would fail).
     */
    public static boolean canPlaceAt(GameContext ctx, int worldX, int worldY) {
        HitBox hb = new HitBox(worldX + HITBOX_REL_X, worldY + HITBOX_REL_Y,
                               HITBOX_WIDTH, HITBOX_HEIGHT);
        return !ctx.world().solidCollision(hb);
    }

    private static Map<String, Double> buildTerrainTable() {
        Map<String, Double> m = new HashMap<>();
        m.put("ocean",     1.0);
        m.put("river",     1.0);
        m.put("beach",     0.3);
        m.put("riverbank", 0.3);
        return m;
    }

    /**
     * Load the 8 directional sprites once, then hand out the same list to every
     * Boat instance. Decoding the artist PNGs costs ~190 ms per Boat, so
     * spawning N boats was paying N× that cost; now it's paid once. The images
     * are treated as immutable — render code only reads them.
     *
     * We first try the artist-provided pirate ship set under {@link #SHIP_DIR};
     * entries with no dedicated PNG (south and southwest) are derived by
     * vertically flipping their north counterparts. Anything still missing
     * falls back to a procedurally drawn arrow placeholder.
     */
    private static ArrayList<BufferedImage> loadDirectionalImages() {
        ArrayList<BufferedImage> cached = SHARED_DIRECTIONAL_IMAGES;
        if (cached != null) return cached;
        synchronized (Boat.class) {
            if (SHARED_DIRECTIONAL_IMAGES != null) return SHARED_DIRECTIONAL_IMAGES;
            ArrayList<BufferedImage> out = new ArrayList<>(8);
            for (int i = 0; i < DIR_NAMES.length; i++) {
                BufferedImage img = loadShipSprite(i);
                if (img == null) img = readBoatImage(DIR_NAMES[i]);
                if (img == null) img = placeholderBoat(i);
                out.add(img);
            }
            SHARED_DIRECTIONAL_IMAGES = out;
            return out;
        }
    }

    /** Look up the starter-ship file for direction index {@code i}, deriving south/SW by vertical flip. */
    private static BufferedImage loadShipSprite(int i) {
        String file = SHIP_FILES[i];
        if (file != null) return readAbs(SHIP_DIR + file);
        // i==2 (south) ← flip north (i=6); i==3 (sw) ← flip ne (i=7)
        if (i == 2) {
            BufferedImage src = readAbs(SHIP_DIR + SHIP_FILES[6]);
            return src == null ? null : flipVertical(src);
        }
        if (i == 3) {
            BufferedImage src = readAbs(SHIP_DIR + SHIP_FILES[7]);
            return src == null ? null : flipVertical(src);
        }
        return null;
    }

    private static BufferedImage readAbs(String path) {
        File f = new File(path);
        if (!f.exists()) return null;
        try { return ImageIO.read(f); }
        catch (Exception e) { return null; }
    }

    private static BufferedImage flipVertical(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, src.getHeight(), src.getWidth(), -src.getHeight(), null);
        g.dispose();
        return out;
    }

    private static BufferedImage readBoatImage(String dir) {
        File f = new File("resources/images/objects/boat/" + dir + ".png");
        if (!f.exists()) return null;
        try { return ImageIO.read(f); }
        catch (Exception e) { return null; }
    }

    /** Procedurally-painted boat that visually points along the given direction index. */
    private static BufferedImage placeholderBoat(int dirIndex) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Hull (brown) + deck (lighter)
        g.setColor(new Color(110, 70, 35));
        Polygon hull = new Polygon();
        hull.addPoint(8, HEIGHT/2);
        hull.addPoint(20, HEIGHT - 6);
        hull.addPoint(WIDTH - 20, HEIGHT - 6);
        hull.addPoint(WIDTH - 8, HEIGHT/2);
        hull.addPoint(WIDTH - 20, 10);
        hull.addPoint(20, 10);
        g.fillPolygon(hull);
        g.setColor(new Color(165, 120, 75));
        g.fillRect(WIDTH/2 - 14, HEIGHT/2 - 8, 28, 16);

        // Direction arrow pointing along DIR_NAMES[dirIndex]
        double angle = -Math.PI / 4.0 * dirIndex; // e=0, ccw
        // DIR_NAMES uses screen y-down so for "e" arrow points right (+x).
        double dx = Math.cos(angle);
        double dy = -Math.sin(angle);

        int cx = WIDTH / 2;
        int cy = HEIGHT / 2;
        int len = 24;
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(240, 230, 200));
        int tipX = (int) (cx + dx * len);
        int tipY = (int) (cy + dy * len);
        g.drawLine(cx, cy, tipX, tipY);
        // Arrowhead
        double perpX = -dy;
        double perpY = dx;
        int barbX1 = (int) (tipX - dx * 8 + perpX * 5);
        int barbY1 = (int) (tipY - dy * 8 + perpY * 5);
        int barbX2 = (int) (tipX - dx * 8 - perpX * 5);
        int barbY2 = (int) (tipY - dy * 8 - perpY * 5);
        g.drawLine(tipX, tipY, barbX1, barbY1);
        g.drawLine(tipX, tipY, barbX2, barbY2);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g.setColor(new Color(60, 40, 20));
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(hull);

        g.dispose();
        return img;
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> out = new ArrayList<>(1);
        out.add(directionalImages.get(facingIndex));
        return out;
    }

    /** True while a player is steering this boat. */
    public boolean isRidden() { return rider != null; }

    public Playable rider() { return rider; }

    /**
     * Per-tick boat update. While ridden, we read input keys, compute the
     * 8-direction step, refuse to enter non-water, and drag the rider with us.
     * While unmanned, defer to the AI behavior (handled by AIComponent).
     */
    @Override
    public void update() {
        if (rider != null) {
            steerByInput();
        }
        // Skip Moveable.update(): the boat does not use the velocity/path pipeline.
        // AIComponent (when attached) ticks via the component update path.
        getHitBox().updateCoords();
    }

    private void steerByInput() {
        InputHandlingSystem in = panel.inputHandlingSystem;
        if (in == null) return;
        // Defensive: a dead rider should not steer. PlayerLifecycle.damage
        // already force-detaches on the death tick, but if anything else
        // ever flips the dead flag without going through there we still
        // want the boat to drift instead of being driven by a corpse.
        if (rider.lifecycle() != null && rider.lifecycle().isDead()) return;

        int ix = (in.isRight() ? 1 : 0) - (in.isLeft() ? 1 : 0);
        int iy = (in.isDown()  ? 1 : 0) - (in.isUp()   ? 1 : 0);

        if (ix == 0 && iy == 0) return; // idle, keep last facing

        double scale = (ix != 0 && iy != 0) ? DIAGONAL_FACTOR : 1.0;
        double stepX = ix * BOAT_SPEED * scale;
        double stepY = iy * BOAT_SPEED * scale;

        facingIndex = directionIndexFor(ix, iy);

        double targetX = getWorldX() + stepX;
        double targetY = getWorldY() + stepY;

        if (canEnter(targetX, targetY)) {
            setWorldX(targetX);
            setWorldY(targetY);
            syncRider();
        } else if (canEnter(targetX, getWorldY())) {
            setWorldX(targetX);
            syncRider();
        } else if (canEnter(getWorldX(), targetY)) {
            setWorldY(targetY);
            syncRider();
        }
    }

    /** Map an 8-direction input (-1/0/+1 in each axis) onto DIR_NAMES. */
    private static int directionIndexFor(int ix, int iy) {
        // DIR_NAMES = {"e","se","s","sw","w","nw","n","ne"}
        if (ix == 1  && iy == 0)  return 0; // e
        if (ix == 1  && iy == 1)  return 1; // se
        if (ix == 0  && iy == 1)  return 2; // s
        if (ix == -1 && iy == 1)  return 3; // sw
        if (ix == -1 && iy == 0)  return 4; // w
        if (ix == -1 && iy == -1) return 5; // nw
        if (ix == 0  && iy == -1) return 6; // n
        if (ix == 1  && iy == -1) return 7; // ne
        return 0;
    }

    /** Boat may enter a cell iff every hitbox corner sits on a water tile. */
    private boolean canEnter(double newWorldX, double newWorldY) {
        HitBox candidate = new HitBox(
            (int) (newWorldX + HITBOX_REL_X),
            (int) (newWorldY + HITBOX_REL_Y),
            HITBOX_WIDTH, HITBOX_HEIGHT);
        int[] xs = { candidate.x, candidate.x + candidate.width - 1 };
        int[] ys = { candidate.y, candidate.y + candidate.height - 1 };
        for (int x : xs) {
            for (int y : ys) {
                Tile t = panel.world.getTile(new Point(x, y));
                if (t == null) return false;
                String name = t.getName();
                if (!("ocean".equals(name) || "river".equals(name))) return false;
            }
        }
        return true;
    }

    private void syncRider() {
        if (rider == null) return;
        rider.setWorldX(getWorldX() + (WIDTH - rider.getWidth()) / 2.0);
        rider.setWorldY(getWorldY() + (HEIGHT - rider.getHeight()) / 2.0 - 16);
        rider.getHitBox().updateCoords();
    }

    @Override
    public void interact() {
        // Interaction from a Playable goes through the overload below.
    }

    /**
     * Boarding handler invoked by Playable.interact() when the player's
     * interaction box overlaps the boat. Boards if the player is within
     * {@link #BOARDING_RADIUS} pixels of the boat's center; otherwise no-op.
     * If the boat is already being ridden by this player, the call dismounts.
     */
    public void interact(Playable player) {
        if (player == null) return;
        if (rider == player) { dismount(); return; }
        if (rider != null) return; // someone else is steering
        if (!withinBoardingRange(player)) return;
        board(player);
    }

    /** Click-to-board entry point; called from Mouse when the click lands on this boat. */
    public boolean tryBoardFromClick(Playable player) {
        if (player == null || rider != null) return false;
        if (!withinBoardingRange(player)) return false;
        board(player);
        return true;
    }

    private boolean withinBoardingRange(Playable player) {
        double dx = (player.getWorldX() + player.getWidth()/2.0)
                  - (getWorldX() + WIDTH/2.0);
        double dy = (player.getWorldY() + player.getHeight()/2.0)
                  - (getWorldY() + HEIGHT/2.0);
        return Math.hypot(dx, dy) <= BOARDING_RADIUS;
    }

    private void board(Playable player) {
        this.rider = player;
        // Zero any residual walking velocity so the player doesn't slide off
        // the deck during the first few ticks after boarding.
        player.getVelocity().set(0, 0);
        player.nullPath();
        // Suspend AI patrol while ridden so player input is sole driver.
        if (hasComponent(AIComponent.class)) {
            components().remove(AIComponent.class);
        }
        // Give the rider water-traversal permission so the movement controller
        // wouldn't otherwise pin them. They're slaved to the boat anyway, but
        // this keeps any incidental queries (e.g. terrain multiplier on the
        // hidden player) from misbehaving.
        if (!player.hasComponent(TerrainSpeedComponent.class)) {
            Map<String, Double> map = new HashMap<>();
            map.put("ocean", 1.0);
            map.put("river", 1.0);
            map.put("beach", 1.0);
            map.put("riverbank", 1.0);
            player.addComponent(new TerrainSpeedComponent(map));
        }
        player.addComponent(new BoatRideComponent(this));
        syncRider();
    }

    /**
     * Severs the rider link without teleporting the player. Used by death /
     * respawn flows where the player is being moved by a different system
     * and dismount's shore-only contract would block the cleanup.
     */
    public void forceDetachRider() {
        rider = null;
        if (!hasComponent(AIComponent.class)) {
            addComponent(new AIComponent((GameContext) panel, new BoatPatrolBehavior(System.nanoTime())));
        }
    }

    /**
     * Dismount the rider onto the nearest reachable shore tile within
     * {@link #DISMOUNT_SHORE_RADIUS_TILES} tiles of the boat. If no shore
     * exists in that window — i.e. the boat is sitting in open water —
     * the call is rejected and the player stays aboard. Returns true on
     * a successful dismount.
     */
    public boolean dismount() {
        if (rider == null) return false;
        Point shore = findShoreNear();
        if (shore == null) return false; // open water: can't disembark
        Playable p = rider;
        p.setWorldX(shore.x);
        p.setWorldY(shore.y);
        p.getHitBox().updateCoords();
        if (p.hasComponent(BoatRideComponent.class)) {
            p.components().remove(BoatRideComponent.class);
        }
        if (p.hasComponent(TerrainSpeedComponent.class)) {
            p.components().remove(TerrainSpeedComponent.class);
        }
        rider = null;
        // Re-arm patrol so the abandoned boat drifts again.
        if (!hasComponent(AIComponent.class)) {
            addComponent(new AIComponent((GameContext) panel, new BoatPatrolBehavior(System.nanoTime())));
        }
        return true;
    }

    /**
     * Pick a safe land tile to drop the rider on. We do a ring scan outward
     * from the boat for the closest non-water tile (the shoreline). Then we
     * step {@link #DISMOUNT_INLAND_STEPS} tiles further in the same direction
     * to land the player solidly on land instead of right at the surf-line —
     * dropping on the shoreline edge tends to leave the player hitbox
     * straddling water, and the next step they take can be back into the
     * ocean. Each inland step is verified to also be non-water; we fall back
     * to the deepest verified land tile along the path. Returns null when
     * nothing within the search window is land at all.
     */
    private Point findShoreNear() {
        int ts = panel.tileSize;
        int cx = (int) (getWorldX() + WIDTH/2.0);
        int cy = (int) (getWorldY() + HEIGHT/2.0);
        for (int r = 1; r <= DISMOUNT_SHORE_RADIUS_TILES; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = cx + dx * ts;
                    int y = cy + dy * ts;
                    if (!isLand(x, y)) continue;
                    return walkInland(cx, cy, dx, dy, ts);
                }
            }
        }
        return null;
    }

    /** Step from the shoreline tile {@code (cx+dx*ts, cy+dy*ts)} another
     *  DISMOUNT_INLAND_STEPS tiles in the same direction, returning the
     *  deepest tile that is still land. The minimum return is the shoreline
     *  tile itself (we never go back over water). */
    private Point walkInland(int cx, int cy, int dx, int dy, int ts) {
        // Normalise the direction so the inland step is exactly one tile
        // along the closest cardinal/diagonal — important when the shore
        // hit lies on the outer ring (|dx|=|dy|=DISMOUNT_SHORE_RADIUS_TILES).
        int sx = Integer.signum(dx);
        int sy = Integer.signum(dy);
        // Default fallback: the shoreline tile itself.
        int bestX = cx + dx * ts;
        int bestY = cy + dy * ts;
        int nx = bestX;
        int ny = bestY;
        for (int step = 1; step <= DISMOUNT_INLAND_STEPS; step++) {
            nx += sx * ts;
            ny += sy * ts;
            if (!isLand(nx, ny)) break;
            bestX = nx;
            bestY = ny;
        }
        return new Point(bestX, bestY);
    }

    private boolean isLand(int worldX, int worldY) {
        Tile t = panel.world.getTile(new Point(worldX, worldY));
        if (t == null) return false;
        String n = t.getName();
        return !("ocean".equals(n) || "river".equals(n));
    }
}
