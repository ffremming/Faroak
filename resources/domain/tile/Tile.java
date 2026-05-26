package resources.domain.tile;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;

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

    private final TileBorderResolver borderResolver;

    public Tile(GamePanel panel, String name, int worldX, int worldY, int altitude) {
        super(panel, name, worldX, worldY,
            panel.tileSize, panel.tileSize, panel.tileSize, panel.tileSize, (short) 0, (short) 0);
        this.altitude = altitude;
        this.borderResolver = new TileBorderResolver(this, panel.imageContainer);
        setupSpecialBehaviour();
    }

    public Tile(GamePanel panel, String name, int worldX, int worldY, int altitude, boolean cliff) {
        this(panel, name, worldX, worldY, altitude);
        this.cliff = cliff;
    }

    private void setupSpecialBehaviour() {
        floor = altitude / 300;
        if ("ocean".equals(getName())) {
            animated = true;
            solid = true;
            lightSource = true;
        }
    }

    // ---- comparators (used by TileManager) ----

    public boolean compareTo(Tile other)        { return this.getName().equals(other.getName()); }
    public boolean isLowerThan(Tile other)      { return panel.tileM.isHigher(this, other); }
    public boolean isCliffDifference(Tile other){ return panel.tileM.cliffDifference(other, this); }

    public int getAltitude() { return altitude; }
    public int getFloor()    { return floor; }
    public boolean isCliff() { return cliff; }
    public void setAnimated(boolean v) { animated = v; }

    // ---- animation ----

    @Override
    public void animate(int value) { borderResolver.resolveInto(images, value); }

    @Override
    public ArrayList<BufferedImage> getImages() {
        if (images.isEmpty()) borderResolver.resolveInto(images, 0);
        return images;
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

    private Tile neighborAt(int direction) {
        Point p = neighborPoint(direction);
        return panel.world.getTile(p);
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
