package resources.domain.tile;

import resources.core.id.Identifier;
import resources.core.registry.Registry;

/**
 * Central registry for {@link TileType} definitions. Pre-populated with the
 * built-in biome tiles so that {@code TileTypeRegistry.PLAINS} / {@code OCEAN}
 * etc. are available without manual setup.
 *
 * New tile kinds (e.g. mod tiles, farmland, cave floor) call
 * {@link Registry#register(Identifier, Object)} on the shared instance returned
 * by {@link #instance()}.
 */
public final class TileTypeRegistry {

    private static final Registry<TileType> REG = new Registry<>("tile_type");

    public static final TileType PLAINS          = define("plains",          100, false, false, false);
    public static final TileType FOREST          = define("forest",          101, false, false, false);
    public static final TileType SEASONAL_FOREST = define("seasonal_forest", 50,  false, false, false);
    public static final TileType SAVANNA         = define("savanna",         15,  false, false, false);
    public static final TileType DESERT          = define("desert",          10,  false, false, false);
    public static final TileType BEACH           = define("beach",           7,   false, false, false);
    public static final TileType WET_BEACH       = define("wetBeach",        5,   false, false, false);
    public static final TileType TIDAL_SAND      = define("tidalSand",       4,   false, false, false);
    public static final TileType SWAMP           = define("swamp",           70,  false, false, false);
    public static final TileType OCEAN           = define("ocean",           0,   true,  true,  true);
    public static final TileType SHALLOW_WATER   = define("shallowWater",    3,   true,  true,  true);
    public static final TileType MEDIUM_WATER    = define("mediumWater",     1,   true,  true,  true);
    public static final TileType MOUNTAIN        = define("mountain",        200, false, true,  false);
    public static final TileType SNOWY_TAIGA     = define("snowy taiga",     102, false, false, false);
    public static final TileType SNOWY_TUNDRA    = define("snowy Tundra",    95,  false, false, false);
    public static final TileType RAIN_FOREST     = define("rain forest",     103, false, false, false);
    public static final TileType RIVERBANK       = define("riverbank",       8,   false, false, false);
    public static final TileType RIVER           = define("river",           0,   true,  true,  true);

    private TileTypeRegistry() {}

    public static Registry<TileType> instance() { return REG; }

    private static TileType define(String name, int altitudeBucket, boolean water, boolean solid, boolean animated) {
        Identifier id = Identifier.of(name.replace(' ', '_'));
        TileType type = TileType.builder(id, name)
            .altitudeBucket(altitudeBucket)
            .water(water)
            .solid(solid)
            .animated(animated)
            .build();
        REG.register(id, type);
        return type;
    }
}
