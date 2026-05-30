package resources.domain.object;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.ai.BoatPatrolBehavior;
import resources.domain.entity.Tickable;
import resources.domain.entity.component.AIComponent;
import resources.domain.entity.component.EntityComponent;
import resources.domain.entity.component.TerrainSpeedComponent;
import resources.domain.player.Moveable;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.world.placement.TileRules;
import resources.geometry.HitBox;
import resources.input.InputHandlingSystem;

/**
 * Self-propelled vessel that drifts across water tiles. Logically a
 * {@link Moveable}, but rendering bypasses the playable sheet — the boat has
 * its own 8-direction sprite set (N, NE, E, SE, S, SW, W, NW) loaded from
 * resources/images/objects/vehicles/watercraft/boat/<dir>.png with procedural
 * fallbacks when no art exists yet.
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
    // Direction vectors aligned to DIR_NAMES (x right+, y down+).
    private static final int[][] DIR_VECTORS = {
        { 1,  0}, // e
        { 1,  1}, // se
        { 0,  1}, // s
        {-1,  1}, // sw
        {-1,  0}, // w
        {-1, -1}, // nw
        { 0, -1}, // n
        { 1, -1}  // ne
    };

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

        directionalImages = BoatSprites.directionalImages();
        // Mirror images into the entity's image list so legacy paths still find one.
        this.images = directionalImages;

        addComponent(new TerrainSpeedComponent(buildTerrainTable()));
        addComponent(new BoatCombatComponent());
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
        m.put("ocean",        1.0);
        m.put("shallowWater", 1.0);
        m.put("river",        1.0);
        m.put("beach",        0.3);
        m.put("wetBeach",     0.3);
        m.put("tidalSand",    0.3);
        m.put("riverbank",    0.3);
        return m;
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
     * While unmanned, AI patrol is ticked from attached components.
     */
    @Override
    public void update() {
        if (isDestroyed()) return;
        tickComponents();
        if (rider != null) {
            steerByInput();
        }
        // Skip Moveable.update(): boats bypass the velocity/path pipeline.
        getHitBox().updateCoords();
    }

    private void tickComponents() {
        for (EntityComponent c : components().all()) {
            if (c instanceof Tickable) ((Tickable) c).update();
        }
    }

    private void steerByInput() {
        InputHandlingSystem in = panel.input();
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

    public boolean fireBroadside() {
        BoatCombatComponent combat = combat();
        return combat != null && combat.fireBroadside();
    }

    public void takeBoatDamage(int amount, double hitX, double hitY) {
        BoatCombatComponent combat = combat();
        if (combat != null) combat.takeDamage(amount, hitX, hitY);
    }

    public boolean isDestroyed() {
        BoatCombatComponent combat = combat();
        return combat != null && combat.isDestroyed();
    }

    /** Direction vector for one of the 8 facing slots in {@link #DIR_NAMES}. */
    static int[] directionVectorForIndex(int index) {
        return DIR_VECTORS[Math.floorMod(index, DIR_VECTORS.length)];
    }

    int facingIndex() { return facingIndex; }

    void clearRiderForSink() { rider = null; }

    private BoatCombatComponent combat() {
        return getComponent(BoatCombatComponent.class);
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
                Tile t = panel.world().getTile(new Point(x, y));
                if (t == null) return false;
                String name = t.getName();
                if (!TileRules.isWater(name)) return false;
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
        if (player == null || isDestroyed()) return;
        if (rider == player) { dismount(); return; }
        if (rider != null) return; // someone else is steering
        if (!withinBoardingRange(player)) return;
        board(player);
    }

    /** Click-to-board entry point; called from Mouse when the click lands on this boat. */
    public boolean tryBoardFromClick(Playable player) {
        if (player == null || rider != null || isDestroyed()) return false;
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
        if (isDestroyed()) return;
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
            map.put("shallowWater", 1.0);
            map.put("river", 1.0);
            map.put("beach", 1.0);
            map.put("wetBeach", 1.0);
            map.put("tidalSand", 1.0);
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
        if (isDestroyed()) return;
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
        Tile t = panel.world().getTile(new Point(worldX, worldY));
        if (t == null) return false;
        String n = t.getName();
        return !TileRules.isWater(n);
    }
}
