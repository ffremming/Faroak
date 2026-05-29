package resources.generation.cave;

import java.awt.Point;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.object.GameObject;
import resources.domain.object.Portal;
import resources.domain.tile.MountainTile;
import resources.domain.tile.Tile;
import resources.generation.WorldGenerator;
import resources.generation.dimension.DimensionRegistry;
import resources.generation.noise.ProceduralGen;

/**
 * Procedural cave generator. Per-coordinate (so the existing
 * {@link WorldGenerator} per-coord chunk-load contract still holds) but the
 * algorithm combines three deterministic channels to get rooms + connecting
 * tunnels + a guaranteed open spawn zone:
 *
 *   1. {@code height(x,y)} drives the large-scale room/wall mosaic — values
 *      below {@link #FLOOR_THRESHOLD} are floor, everything else defaults to
 *      wall (solid).
 *   2. A deterministic 1D tunnel hash on each axis carves horizontal and
 *      vertical corridors so isolated rooms get linked up — caves are
 *      walkable end-to-end instead of fragmented islands.
 *   3. A small disc around the cave spawn (0,0) is always floor so the player
 *      lands in open space when they portal in.
 *
 * Walls are {@link MountainTile} instances — the 8-bit auto-tiler reads the
 * full neighbour mask and picks proper outer corners, inner concave corners,
 * edges, and varied interior body sprites from the rockCliff atlas. Floors
 * are vanilla {@link Tile}s painted with a winner-take-all blend over three
 * stone variants (see {@link #pickStoneVariant}), giving large organic blobs
 * with hard, height-driven borders between adjacent stones.
 */
public final class CaveGenerator implements WorldGenerator {

    private static final int TILE = 64;

    /** Anything below this in the height channel becomes cave floor. */
    private static final double FLOOR_THRESHOLD = 0.05;

    /** Tile-units. Below this distance from cave spawn (0,0), force floor. */
    private static final int SPAWN_CLEARING_TILES = 4;

    /** Tile-units between tunnel corridors on each axis (rough cell pitch). */
    private static final int TUNNEL_PITCH_TILES = 12;

    /** Floor altitude < wall altitude so the cliff border kicks in. */
    private static final int FLOOR_ALTITUDE = 100;
    private static final int WALL_ALTITUDE  = 600;

    /**
     * Stone variants painted across the cave floor as large blobs. These are
     * cave-specific sprites (warm cobblestone, cool flagstone slab, mossy
     * fitted brick) with soft low-contrast B1/C0 overlays so neighbour
     * blobs cross-fade instead of slamming together with a cliff edge.
     * TileManager registers distinct tileHeights (101/102/103) so two
     * adjacent variants still get an ordered border via the overlay.
     */
    private static final String[] FLOOR_STONE_VARIANTS = {
        "caveCobble", "caveSlab", "caveMossStone"
    };

    /**
     * Patch frequency for stone-variant selection. Halved from the previous
     * 0.0018 so each stone blob covers roughly 2x the area — large calm
     * regions instead of frequent variant swaps in the cramped cavern space.
     */
    private static final double STONE_PATCH_FREQ = 0.0009;

    // Salts keep independent decisions independent under the same seed.
    private static final long SALT_STONE_VARIANT = 11; // base; +i*977 per variant
    private static final long SALT_DECOR      = 21;
    private static final long SALT_DECOR_PICK = 22;
    private static final long SALT_PORTAL     = 31;
    private static final long SALT_TUNNEL_H   = 41;
    private static final long SALT_TUNNEL_V   = 42;

    private static final double DECOR_DENSITY  = 0.04;
    private static final double PORTAL_DENSITY = 0.00010;

    private final GamePanel panel;
    private final ProceduralGen proceduralGen;
    private final Point overworldReturn;

    public CaveGenerator(GamePanel panel, long seed) {
        this(panel, seed, new Point(0, 0));
    }

    public CaveGenerator(GamePanel panel, long seed, Point overworldReturn) {
        this.panel = panel;
        this.proceduralGen = new ProceduralGen(seed);
        this.overworldReturn = new Point(overworldReturn);
    }

    @Override
    public Tile getTile(int worldX, int worldY) {
        if (isFloor(worldX, worldY)) {
            String name = FLOOR_STONE_VARIANTS[pickStoneVariant(worldX, worldY)];
            return new Tile(panel, name, worldX, worldY, FLOOR_ALTITUDE);
        }
        // Walls: MountainTile auto-tiles against the 8-neighbour mask so the
        // cavern gets proper outer corners, edges, inner concave corners, and
        // a varied interior body — uses the rockCliff atlas already shipped.
        return new MountainTile(panel, worldX, worldY, WALL_ALTITUDE);
    }

    /**
     * Winner-take-all over one independent low-frequency noise field per stone
     * variant. The variant whose field samples highest at (x,y) wins, giving
     * each stone roughly equal coverage and crisp organic blob boundaries
     * where two fields cross — never tiny scatter. Same shape used by the
     * overworld EntityFactory for biome tile variants.
     */
    private int pickStoneVariant(int worldX, int worldY) {
        int bestIdx = 0;
        double bestVal = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < FLOOR_STONE_VARIANTS.length; i++) {
            double v = proceduralGen.patchNoise(worldX, worldY,
                    SALT_STONE_VARIANT + (long) i * 977L, STONE_PATCH_FREQ);
            if (v > bestVal) { bestVal = v; bestIdx = i; }
        }
        return bestIdx;
    }

    @Override
    public BaseEntity getEntity(int worldX, int worldY) {
        if (!isFloor(worldX, worldY)) return null;

        if (isPortalCell(worldX, worldY)) {
            return new Portal(panel, "cave_portal", worldX, worldY,
                DimensionRegistry.CAVE, DimensionRegistry.OVERWORLD, overworldReturn);
        }

        if (proceduralGen.rollAt(worldX, worldY, SALT_DECOR) >= DECOR_DENSITY) return null;

        String decor = pickDecor(worldX, worldY);
        boolean solid = !"cave_mushroom".equals(decor);
        return new GameObject(panel, decor,
            worldX, worldY,
            TILE, TILE, TILE, TILE,
            0, 0,
            solid);
    }

    // ---- carving logic ----

    /**
     * A coord is floor if any of: inside the spawn clearing, lies on a tunnel
     * corridor (axis-aligned), or height-noise puts it in a room.
     */
    private boolean isFloor(int worldX, int worldY) {
        if (insideSpawnClearing(worldX, worldY)) return true;
        if (onTunnel(worldX, worldY)) return true;
        return proceduralGen.height(worldX, worldY) < FLOOR_THRESHOLD;
    }

    private boolean insideSpawnClearing(int worldX, int worldY) {
        int tx = Math.floorDiv(worldX, TILE);
        int ty = Math.floorDiv(worldY, TILE);
        return Math.abs(tx) <= SPAWN_CLEARING_TILES && Math.abs(ty) <= SPAWN_CLEARING_TILES;
    }

    /**
     * Tunnels run along axis-aligned strips: every TUNNEL_PITCH_TILES, one
     * tile-wide row/column is carved out so rooms aren't islands. The exact
     * row chosen per band is hashed so they aren't perfectly straight.
     */
    private boolean onTunnel(int worldX, int worldY) {
        int tx = Math.floorDiv(worldX, TILE);
        int ty = Math.floorDiv(worldY, TILE);
        int bandY = Math.floorDiv(ty, TUNNEL_PITCH_TILES);
        int bandX = Math.floorDiv(tx, TUNNEL_PITCH_TILES);
        int chosenRow = (int) Math.floor(proceduralGen.rollAt(bandY, 0, SALT_TUNNEL_H) * TUNNEL_PITCH_TILES);
        int chosenCol = (int) Math.floor(proceduralGen.rollAt(0, bandX, SALT_TUNNEL_V) * TUNNEL_PITCH_TILES);
        int localY = Math.floorMod(ty, TUNNEL_PITCH_TILES);
        int localX = Math.floorMod(tx, TUNNEL_PITCH_TILES);
        return localY == chosenRow || localX == chosenCol;
    }

    /** Sparse exit portals; never inside the spawn clearing. */
    private boolean isPortalCell(int worldX, int worldY) {
        if (insideSpawnClearing(worldX, worldY)) return false;
        return proceduralGen.rollAt(worldX, worldY, SALT_PORTAL) < PORTAL_DENSITY;
    }

    private String pickDecor(int worldX, int worldY) {
        double pick = proceduralGen.rollAt(worldX, worldY, SALT_DECOR_PICK);
        if (pick < 0.40) return "stalagmite";
        if (pick < 0.70) return "stone_rubble";
        if (pick < 0.90) return "cave_mushroom";
        return "iron_ore_vein";
    }
}
