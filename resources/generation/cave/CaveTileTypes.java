package resources.generation.cave;

import resources.core.id.Identifier;
import resources.domain.tile.TileType;
import resources.domain.tile.TileTypeRegistry;

/**
 * Tile-type definitions for the cave dimension. Kept separate from the
 * overworld {@link TileTypeRegistry} bootstrap so cave content can be added
 * (and one day stripped out for a sand-box build) without touching the core
 * registry class.
 *
 * Touch any static field — e.g. {@code CaveTileTypes.CAVE_STONE} — to force
 * class-init and guarantee the registry contains these entries before the
 * cave generator runs.
 */
public final class CaveTileTypes {

    public static final TileType CAVE_STONE = register("cave_stone", 50, false);
    public static final TileType CAVE_WALL  = register("cave_wall",   0, true);
    public static final TileType CAVE_DIRT  = register("cave_dirt",  50, false);

    private CaveTileTypes() {}

    /** No-op accessor whose only job is to trigger static init from bootstrap code. */
    public static void init() { /* class-load only */ }

    private static TileType register(String name, int altitudeBucket, boolean solid) {
        Identifier id = Identifier.of(name);
        TileType type = TileType.builder(id, name)
            .altitudeBucket(altitudeBucket)
            .water(false)
            .solid(solid)
            .animated(false)
            .build();
        TileTypeRegistry.instance().register(id, type);
        return type;
    }
}
