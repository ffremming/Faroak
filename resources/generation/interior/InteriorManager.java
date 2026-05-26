package resources.generation.interior;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.object.GameObject;
import resources.domain.object.Portal;
import resources.domain.tile.Tile;
import resources.generation.dimension.DimensionRegistry;

/**
 * Owns every placed interior in the world and answers per-tile/per-entity
 * queries from {@link InteriorGenerator}. Interiors are predefined layouts
 * pulled from {@link InteriorRegistry} and anchored at fixed slots in the
 * interior dimension. No procedural generation happens here.
 *
 * Slot model: the interior dimension is laid out as a coarse grid of
 * {@link #SLOT_TILES}-wide slots so different interiors don't overlap. Slot
 * (sx, sy) has its top-left tile at (sx * SLOT_TILES, sy * SLOT_TILES). One
 * interior may be placed per slot via {@link #place(int, int, Interior)}.
 *
 * Adding a new placement: call {@link #place} during bootstrap (see
 * {@link InteriorBootstrap}). Adding a new interior layout: define it in
 * {@link InteriorRegistry}.
 */
public final class InteriorManager {

    private static final int TILE = 64;

    /** Wide enough to fit the largest interior with room to spare. */
    public static final int SLOT_TILES = 20;

    private final GamePanel panel;
    private Point overworldArrival;
    private final Map<Long, Placement> placements = new HashMap<>();

    public InteriorManager(GamePanel panel, Point overworldArrival) {
        this.panel = panel;
        this.overworldArrival = new Point(overworldArrival);
    }

    /** Update where doors send the player back to in the overworld. Used by
     *  {@code GenerationManager} after the player's spawn is known. */
    public void setOverworldArrival(Point arrival) {
        this.overworldArrival = new Point(arrival);
    }

    /** Anchor an Interior at slot (slotX, slotY). Overwrites any prior placement. */
    public void place(int slotX, int slotY, Interior interior) {
        placements.put(key(slotX, slotY), new Placement(slotX, slotY, interior));
    }

    /** Stable list of placements (for diagnostics / tests). */
    public List<Placement> placements() {
        return Collections.unmodifiableList(new ArrayList<>(placements.values()));
    }

    /** World-tile point of the door tile for a given interior placement. */
    public Point doorWorldTile(Placement p) {
        return new Point(p.slotX * SLOT_TILES + p.interior.doorTx(),
                         p.slotY * SLOT_TILES + p.interior.doorTy());
    }

    /** Tile at the given world pixel; null means "no opinion, caller should default to void". */
    public Tile tileAt(int worldX, int worldY) {
        Lookup l = lookup(worldX, worldY);
        if (l == null) return voidTile(worldX, worldY);
        switch (l.cell) {
            case WALL:
                return new Tile(panel, InteriorTileTypes.WALL_INDOOR, worldX, worldY, 300);
            case FLOOR_WOOD:
            case TABLE:
            case CHAIR:
            case CRATE:
            case DOOR:
                return new Tile(panel, InteriorTileTypes.FLOOR_WOOD, worldX, worldY, 100);
            case FLOOR_STONE:
                return new Tile(panel, InteriorTileTypes.FLOOR_STONE, worldX, worldY, 100);
            case VOID:
            default:
                return voidTile(worldX, worldY);
        }
    }

    /** Entity at the given world pixel, or null if none. */
    public BaseEntity entityAt(int worldX, int worldY) {
        Lookup l = lookup(worldX, worldY);
        if (l == null) return null;
        switch (l.cell) {
            case DOOR:
                return new Portal(panel, "door", worldX, worldY,
                    DimensionRegistry.INTERIOR, DimensionRegistry.OVERWORLD,
                    overworldArrival);
            case TABLE:
                return new GameObject(panel, "table", worldX, worldY,
                    TILE, TILE, TILE, TILE, 0, 0, true);
            case CHAIR:
                return new GameObject(panel, "chair", worldX, worldY,
                    TILE, TILE, TILE, TILE, 0, 0, false);
            case CRATE:
                return new GameObject(panel, "crate", worldX, worldY,
                    TILE, TILE, TILE, TILE, 0, 0, true);
            default:
                return null;
        }
    }

    // ---- internals ----

    private Tile voidTile(int worldX, int worldY) {
        return new Tile(panel, InteriorTileTypes.VOID_FLOOR, worldX, worldY, 0);
    }

    private Lookup lookup(int worldX, int worldY) {
        int tx = Math.floorDiv(worldX, TILE);
        int ty = Math.floorDiv(worldY, TILE);
        int slotX = Math.floorDiv(tx, SLOT_TILES);
        int slotY = Math.floorDiv(ty, SLOT_TILES);
        Placement p = placements.get(key(slotX, slotY));
        if (p == null) return null;
        int localX = tx - slotX * SLOT_TILES;
        int localY = ty - slotY * SLOT_TILES;
        Interior.Cell cell = p.interior.cellAt(localX, localY);
        if (cell == Interior.Cell.VOID) return null;
        return new Lookup(cell);
    }

    private static long key(int sx, int sy) {
        return (((long) sx) << 32) ^ (sy & 0xffffffffL);
    }

    /** A single Interior anchored at one slot in the interior dimension. */
    public static final class Placement {
        public final int slotX;
        public final int slotY;
        public final Interior interior;
        Placement(int slotX, int slotY, Interior interior) {
            this.slotX = slotX;
            this.slotY = slotY;
            this.interior = interior;
        }
    }

    private static final class Lookup {
        final Interior.Cell cell;
        Lookup(Interior.Cell c) { this.cell = c; }
    }
}
