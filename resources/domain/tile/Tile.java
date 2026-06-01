package resources.domain.tile;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.AnimationComponent;
import resources.presentation.animation.AnimationClip;
import resources.presentation.animation.Animations;

/**
 * One terrain cell. Holds altitude/floor data, links to four neighbours, and
 * delegates border / animation resolution to {@link TileBorderResolver}.
 *
 * Stays focused on data + neighbour wiring; rendering rules live in the
 * resolver so the strategy can evolve (fence connections, biome-aware borders,
 * cliff edges) without touching this class.
 */
public class Tile extends BaseEntity {

    static final int NORTH = 0;
    static final int EAST  = 1;
    static final int SOUTH = 2;
    static final int WEST  = 3;

    private Tile north, south, east, west;

    public BufferedImage image;
    final ArrayList<BufferedImage> images = new ArrayList<>();

    int altitude;
    int floor;
    boolean cliff = false;
    private int lastBuiltFrame = -1;

    private final TileBorderResolver borderResolver;

    public Tile(GamePanel panel, String name, int worldX, int worldY, int altitude) {
        super(panel, name, worldX, worldY,
            panel.tileSize, panel.tileSize, panel.tileSize, panel.tileSize, (short) 0, (short) 0);
        this.altitude = altitude;
        this.borderResolver = new TileBorderResolver(this, panel.images());
        setupSpecialBehaviour();
    }

    public Tile(GamePanel panel, String name, int worldX, int worldY, int altitude, boolean cliff) {
        this(panel, name, worldX, worldY, altitude);
        this.cliff = cliff;
    }

    private void setupSpecialBehaviour() {
        floor = altitude / 300;
        if ("ocean".equals(getName())) {
            solid = true;
            lightSource = true;
            AnimationClip waves = panel.animations().require(Animations.OCEAN_WAVES);
            addComponent(new AnimationComponent(waves, panel.clock()));
        } else if ("shallowWater".equals(getName())) {
            solid = true;
            lightSource = true;
            AnimationClip waves = panel.animations().require(Animations.SHALLOW_WATER_WAVES);
            addComponent(new AnimationComponent(waves, panel.clock()));
        } else if ("midWater".equals(getName())) {
            solid = true;
            lightSource = true;
            AnimationClip waves = panel.animations().require(Animations.MID_WATER_WAVES);
            addComponent(new AnimationComponent(waves, panel.clock()));
        } else if ("mediumWater".equals(getName())) {
            solid = true;
            lightSource = true;
            AnimationClip waves = panel.animations().require(Animations.MEDIUM_WATER_WAVES);
            addComponent(new AnimationComponent(waves, panel.clock()));
        }
    }

    // ---- comparators (used by TileManager) ----

    public boolean compareTo(Tile other)        { return this.getName().equals(other.getName()); }
    public boolean isLowerThan(Tile other)      { return panel.tiles().isHigher(this, other); }
    public boolean isCliffDifference(Tile other){ return panel.tiles().cliffDifference(other, this); }

    public int getAltitude() { return altitude; }
    public int getFloor()    { return floor; }
    public boolean isCliff() { return cliff; }
    public void setAnimated(boolean v) { animated = v; }

    /** Toggle blocking. Used by generators that build solid wall tiles. */
    public void setSolid(boolean v) { this.solid = v; }

    // ---- animation ----

    @Override
    public void update() {
        super.update();
        AnimationComponent anim = getComponent(AnimationComponent.class);
        if (anim == null) return;
        int idx = anim.currentFrameIndex();
        if (idx != lastBuiltFrame) {
            rebuildImageStack(idx);
            lastBuiltFrame = idx;
        }
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        if (images.isEmpty()) rebuildImageStack(0);
        return images;
    }

    private void rebuildImageStack(int frame) {
        borderResolver.resolveInto(images, frame);
    }

    /**
     * Swap this tile's sprite in place: change the name and force the image
     * stack to re-resolve from the new name on the next draw. Used by in-place
     * tile mutation (e.g. hoeing grass into farmland). Callers that mutate a
     * tile inside a loaded chunk must also invalidate that chunk's render bake
     * (see {@link resources.world.WorldInteraction}) — the static tile layer is
     * cached per chunk and won't pick up the change otherwise.
     */
    public void retexture(String newName) {
        setName(newName);
        images.clear();
        lastBuiltFrame = -1;
    }

    // ---- neighbours ----

    public Tile[] getNeighbors() { return new Tile[]{north, east, south, west}; }

    public boolean hasCompleteNeighbors() {
        return north != null && south != null && west != null && east != null;
    }

    /** Look up and wire all four neighbours from the world's tile index. */
    public void setNeighBors() {
        if (north != null) return;
        north = neighborAt(NORTH);
        east  = neighborAt(EAST);
        south = neighborAt(SOUTH);
        west  = neighborAt(WEST);
        if (computeCliff()) cliff = true;
    }

    /**
     * Re-wire all four neighbour links from the world index and drop the cached
     * border image stack so it re-resolves on the next draw. Unlike
     * {@link #setNeighBors()} (which only wires once, then early-returns), this
     * forces a refresh — call it on a tile whose neighbour set changed because an
     * adjacent cell was replaced in the grid by in-place tile mutation (e.g. a
     * neighbour was hoed into farmland). The owning chunk's render bake must also
     * be invalidated (see {@link resources.world.WorldInteraction}).
     */
    public void refreshBorders() {
        north = neighborAt(NORTH);
        east  = neighborAt(EAST);
        south = neighborAt(SOUTH);
        west  = neighborAt(WEST);
        images.clear();
        lastBuiltFrame = -1;
    }

    private Tile neighborAt(int direction) {
        Point p = neighborPoint(direction);
        return panel.world().getTile(p);
    }

    private Point neighborPoint(int direction) {
        int half = width / 2;
        switch (direction) {
            case NORTH: return new Point((int) worldX + half,             (int) worldY - half);
            case EAST:  return new Point((int) worldX + width * 3 / 2,    (int) worldY + half);
            case SOUTH: return new Point((int) worldX + half,             (int) worldY + width * 3 / 2);
            case WEST:  return new Point((int) worldX - half,             (int) worldY + half);
            default:    return null;
        }
    }

    private boolean computeCliff() {
        for (Tile n : getNeighbors()) {
            if (n != null && n.floor < floor && n.floor >= 0) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "class " + getClass().getSimpleName()
            + "\nname: " + getName()
            + "\nsolid: " + solid
            + "\nanimated: " + animated
            + "\nlightSource: " + lightSource
            + "\naltitude: " + altitude
            + "\nfloor: " + floor
            + "\ncliff: " + cliff
            + neighborNames();
    }

    private String neighborNames() {
        return "\nnorth: " + nameOf(north) + " east: " + nameOf(east)
             + " south: " + nameOf(south) + " west: " + nameOf(west);
    }

    private static String nameOf(Tile t) { return t == null ? "-" : t.getName(); }
}
