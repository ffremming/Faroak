package resources.generation.interior;

import java.awt.Point;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.object.GameObject;
import resources.domain.object.Portal;
import resources.domain.tile.Tile;
import resources.generation.WorldGenerator;
import resources.generation.dimension.DimensionRegistry;

/**
 * Generates a sparse "interior building" dimension: rooms sit on a chunk-grid,
 * each with a stone perimeter wall and a wood floor, surrounded by void.
 *
 * Layout is deterministic per (worldX, worldY): tile coords are converted to
 * chunk coords, a per-chunk room template is looked up (currently only chunk
 * (0,0) hosts a 10x10 starter room), and the tile/entity contribution for that
 * cell is derived from the template. Outside any room the tile is void_floor
 * and no entity is placed.
 *
 * The chunk (0,0) room spans tile coords (-5,-5)..(4,4) in tile units, i.e.
 * pixels (-320,-320)..(256,256) with TILE = 64. A door sits at tile (0, +4)
 * (the south wall) as a Portal that returns the player to the overworld.
 */
public final class InteriorGenerator implements WorldGenerator {

    private static final int TILE       = 64;
    private static final int ROOM_HALF  = 5;   // 10x10 -> tiles [-5, 4]
    private static final int DOOR_TX    = 0;
    private static final int DOOR_TY    = ROOM_HALF - 1; // 4, south wall

    private static final long SALT_PROP   = 11L;
    private static final long SALT_PICK   = 13L;
    private static final double PROP_DENSITY = 0.06;

    private final GamePanel panel;
    private final long seed;
    private final Point arrival; // overworld point the door returns to

    public InteriorGenerator(GamePanel panel, long seed) {
        this(panel, seed, new Point(0, 0));
    }

    public InteriorGenerator(GamePanel panel, long seed, Point overworldArrival) {
        this.panel = panel;
        this.seed = seed;
        this.arrival = new Point(overworldArrival);
        InteriorTileTypes.bootstrap();
    }

    // ---- WorldGenerator ----

    @Override
    public Tile getTile(int worldX, int worldY) {
        RoomCell cell = cellAt(worldX, worldY);
        String name;
        int altitude;
        if (cell == RoomCell.OUTSIDE) {
            name = InteriorTileTypes.VOID_FLOOR;
            altitude = 0;
        } else if (cell == RoomCell.WALL) {
            name = InteriorTileTypes.WALL_INDOOR;
            altitude = 300;
        } else {
            name = InteriorTileTypes.FLOOR_WOOD;
            altitude = 100;
        }
        return new Tile(panel, name, worldX, worldY, altitude);
    }

    @Override
    public BaseEntity getEntity(int worldX, int worldY) {
        RoomCell cell = cellAt(worldX, worldY);
        if (cell != RoomCell.FLOOR) return null;

        if (isDoorCell(worldX, worldY)) {
            return new Portal(panel, "door", worldX, worldY,
                DimensionRegistry.INTERIOR, DimensionRegistry.OVERWORLD, arrival);
        }

        double spawn = rollAt(worldX, worldY, SALT_PROP);
        if (spawn >= PROP_DENSITY) return null;

        double pick = rollAt(worldX, worldY, SALT_PICK);
        return makeProp(pick, worldX, worldY);
    }

    // ---- room layout ----

    private RoomCell cellAt(int worldX, int worldY) {
        int tx = Math.floorDiv(worldX, TILE);
        int ty = Math.floorDiv(worldY, TILE);
        if (!hasRoomFor(tx, ty)) return RoomCell.OUTSIDE;
        boolean perimeter = tx == -ROOM_HALF || tx == ROOM_HALF - 1
                         || ty == -ROOM_HALF || ty == ROOM_HALF - 1;
        if (!perimeter) return RoomCell.FLOOR;
        if (tx == DOOR_TX && ty == DOOR_TY) return RoomCell.FLOOR; // doorway
        return RoomCell.WALL;
    }

    private boolean hasRoomFor(int tx, int ty) {
        // Currently only chunk (0,0) has a room. Future: room registry lookup.
        return tx >= -ROOM_HALF && tx <= ROOM_HALF - 1
            && ty >= -ROOM_HALF && ty <= ROOM_HALF - 1;
    }

    private boolean isDoorCell(int worldX, int worldY) {
        int tx = Math.floorDiv(worldX, TILE);
        int ty = Math.floorDiv(worldY, TILE);
        return tx == DOOR_TX && ty == DOOR_TY;
    }

    private GameObject makeProp(double pick, int worldX, int worldY) {
        String name;
        boolean solid;
        if (pick < 0.4) {
            name = "table"; solid = true;
        } else if (pick < 0.75) {
            name = "chair"; solid = false;
        } else {
            name = "crate"; solid = true;
        }
        return new GameObject(panel, name, worldX, worldY,
            TILE, TILE, TILE, TILE, 0, 0, solid);
    }

    // ---- deterministic rng ----

    /** [0,1) hash of (x,y,salt) under this generator's seed. */
    private double rollAt(int worldX, int worldY, long salt) {
        long h = (long) worldX * 73856093L
               ^ (long) worldY * 19349663L
               ^ seed         * 83492791L
               ^ salt         * 2654435761L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return (h & 0x7fffffffffffffffL) / (double) Long.MAX_VALUE;
    }

    private enum RoomCell { OUTSIDE, WALL, FLOOR }
}
