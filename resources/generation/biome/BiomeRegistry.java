package resources.generation.biome;

import java.util.Arrays;
import java.util.Collections;

import resources.generation.factory.ObjectCatalog;
import resources.generation.factory.ObjectCatalog.ObjectSpec;

/**
 * All known biomes + the classifier that picks one based on noise samples.
 * Thresholds match (and slightly adapt) the boundaries originally laid out in TerrainGenSimplex.
 *
 * Tile names align with files in resources/images/tile/ (with ImageContainer fallbacks for any gaps).
 * Vegetation rules reference {@link ObjectCatalog} — sprite size, hitbox and solidity all live
 * there, so adjusting how an object renders only requires touching the catalog.
 */
public final class BiomeRegistry {

    // OCEAN_LVL pulled toward 0 to give the seed more open water (large oceans),
    // and the gap between OCEAN_LVL and BEACH_LVL widened so islands have broad
    // sandy belts instead of a one-tile rim.
    public static final double OCEAN_LVL       = -0.10;
    public static final double BEACH_LVL       =  0.05;
    public static final double MOUNTAIN_LVL    =  0.55;
    public static final double RIVER_THRESHOLD =  0.04;
    public static final double RIVERBANK_THRESHOLD = 0.07;

    // Vegetation rules — only density lives here; everything else (size, hitbox,
    // solid) is read from the catalog by name.
    private static final VegetationRule OAK    = density("oak_M",          0.08);
    private static final VegetationRule BIRCH  = density("birch_M",        0.06);
    private static final VegetationRule SPRUCE = density("spruce_M",       0.12);
    private static final VegetationRule PALM   = density("palm_M",         0.05);

    private static final VegetationRule SHRUB  = density("shrub_M",        0.10);
    private static final VegetationRule BUSH   = density("bushTree",       0.07);
    private static final VegetationRule DSHRUB = density("desert_shrub_S", 0.08);

    private static final VegetationRule GRASS  = density("wildGrass",      0.18);

    private static final VegetationRule STONE     = density("stone",      0.03);
    private static final VegetationRule STONE_HI  = density("stone",      0.08); // denser near mountains
    private static final VegetationRule DRIFTWOOD = density("driftwood",  0.04);
    private static final VegetationRule BEACH_PALM = density("palm_M",    0.03);

    public static final Biome OCEAN            = new Biome("ocean",           "ocean",           true,  Collections.emptyList());
    public static final Biome RIVER            = new Biome("river",           "ocean",           true,  Collections.emptyList());
    public static final Biome BEACH            = new Biome("beach",           "beach",           false, Arrays.asList(DRIFTWOOD, BEACH_PALM, STONE));
    public static final Biome RIVERBANK        = new Biome("riverbank",       "beach",           false, Arrays.asList(DRIFTWOOD, STONE));
    public static final Biome MOUNTAIN         = new Biome("mountain",        "mountain",        true,  Collections.emptyList());

    public static final Biome ICY_PLAINS       = new Biome("icy plains",      "snowy Tundra",    false, Arrays.asList(STONE));
    public static final Biome SNOWY_TAIGA      = new Biome("snowy taiga",     "snowy taiga",     false, Arrays.asList(SPRUCE, STONE));

    public static final Biome PLAINS           = new Biome("plains",          "plains",          false, Arrays.asList(GRASS, SHRUB, BUSH, OAK, STONE));
    public static final Biome FOREST           = new Biome("forest",          "forest",          false, Arrays.asList(OAK, BIRCH, BUSH, SHRUB, GRASS, STONE));
    public static final Biome SEASONAL_FOREST  = new Biome("seasonal forest", "seasonal forest", false, Arrays.asList(BIRCH, OAK, BUSH, GRASS, STONE));
    public static final Biome SWAMP            = new Biome("swamp",           "swamp",           false, Arrays.asList(SHRUB, GRASS, STONE));

    public static final Biome SAVANNA          = new Biome("savanna",         "savanna",         false, Arrays.asList(OAK, GRASS, STONE_HI));
    public static final Biome DESERT           = new Biome("desert",          "desert",          false, Arrays.asList(DSHRUB, STONE_HI));
    public static final Biome RAIN_FOREST      = new Biome("rain forest",     "forest",          false, Arrays.asList(OAK, PALM, BUSH, SHRUB, GRASS));

    private BiomeRegistry() {}

    /** Pick a biome from noise samples in [-1,1] for height/temperature/humidity and [0,1] for river. */
    public static Biome classify(double height, double temperature, double humidity, double river) {
        if (height <= OCEAN_LVL) return OCEAN;
        if (river  <  RIVER_THRESHOLD)     return RIVER;
        if (height <= BEACH_LVL) return BEACH;
        if (river  <  RIVERBANK_THRESHOLD) return RIVERBANK;
        if (height >= MOUNTAIN_LVL) return MOUNTAIN;

        if (temperature < -0.55) {
            return humidity > 0.0 ? SNOWY_TAIGA : ICY_PLAINS;
        }
        if (temperature < -0.15) {
            return humidity > 0.05 ? FOREST : PLAINS;
        }
        if (temperature < 0.30) {
            if (humidity > 0.45)  return SWAMP;
            if (humidity > 0.05)  return SEASONAL_FOREST;
            if (humidity > -0.35) return PLAINS;
            return SAVANNA;
        }
        if (humidity > 0.30)  return RAIN_FOREST;
        if (humidity > -0.10) return SAVANNA;
        return DESERT;
    }

    /** Build a VegetationRule that mirrors the catalog spec for the named object. */
    private static VegetationRule density(String objectName, double density) {
        ObjectSpec s = ObjectCatalog.get(objectName);
        return new VegetationRule(objectName, density,
                s.width, s.height, s.hitBoxWidth, s.hitBoxHeight, s.solid);
    }
}
