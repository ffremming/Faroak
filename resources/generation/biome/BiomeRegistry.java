package resources.generation.biome;

import java.util.Arrays;
import java.util.Collections;

/**
 * All known biomes + the classifier that picks one based on noise samples.
 * Thresholds match (and slightly adapt) the boundaries originally laid out in TerrainGenSimplex.
 *
 * Tile names align with files in resources/images/tile/ (with ImageContainer fallbacks for any gaps).
 * Vegetation object names align with folders in resources/images/objects/.
 */
public final class BiomeRegistry {

    public static final double OCEAN_LVL       = -0.30;
    public static final double BEACH_LVL       = -0.22;
    public static final double MOUNTAIN_LVL    =  0.55;
    public static final double RIVER_THRESHOLD =  0.04;
    public static final double RIVERBANK_THRESHOLD = 0.07;

    private static final int T = 64;

    // Tall, solid vegetation
    private static final VegetationRule OAK    = rule("oak_M",          0.08, T,    (int)(T * 1.6),  T - 16, T / 2, true);
    private static final VegetationRule BIRCH  = rule("birch_M",        0.06, T,    (int)(T * 1.5),  T - 20, T / 2, true);
    private static final VegetationRule SPRUCE = rule("spruce_M",       0.12, T,    (int)(T * 1.7),  T - 20, T / 2, true);
    private static final VegetationRule PALM   = rule("palm_M",         0.05, T,    (int)(T * 1.6),  T - 24, T / 2, true);

    // Shrub layer
    private static final VegetationRule SHRUB  = rule("shrub_M",        0.10, T,    T,               T - 16, T - 16, false);
    private static final VegetationRule DSHRUB = rule("desert_shrub_S", 0.08, T / 2, T / 2,          T / 2,  T / 2,  false);

    // Ground decor
    private static final VegetationRule GRASS  = rule("wildGrass",      0.18, T,    T,               T,      T,      false);

    public static final Biome OCEAN            = new Biome("ocean",           "ocean",           true,  Collections.emptyList());
    public static final Biome RIVER            = new Biome("river",           "ocean",           true,  Collections.emptyList());
    public static final Biome BEACH            = new Biome("beach",           "beach",           false, Collections.emptyList());
    public static final Biome RIVERBANK        = new Biome("riverbank",       "beach",           false, Collections.emptyList());
    public static final Biome MOUNTAIN         = new Biome("mountain",        "mountain",        true,  Collections.emptyList());

    public static final Biome ICY_PLAINS       = new Biome("icy plains",      "snowy Tundra",    false, Collections.emptyList());
    public static final Biome SNOWY_TAIGA      = new Biome("snowy taiga",     "snowy taiga",     false, Arrays.asList(SPRUCE));

    public static final Biome PLAINS           = new Biome("plains",          "plains",          false, Arrays.asList(GRASS, SHRUB, OAK));
    public static final Biome FOREST           = new Biome("forest",          "forest",          false, Arrays.asList(OAK, BIRCH, SHRUB, GRASS));
    public static final Biome SEASONAL_FOREST  = new Biome("seasonal forest", "seasonal forest", false, Arrays.asList(BIRCH, OAK, GRASS));
    public static final Biome SWAMP            = new Biome("swamp",           "swamp",           false, Arrays.asList(SHRUB, GRASS));

    public static final Biome SAVANNA          = new Biome("savanna",         "savanna",         false, Arrays.asList(OAK, GRASS));
    public static final Biome DESERT           = new Biome("desert",          "desert",          false, Arrays.asList(DSHRUB));
    public static final Biome RAIN_FOREST      = new Biome("rain forest",     "forest",          false, Arrays.asList(OAK, PALM, SHRUB, GRASS));

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

    private static VegetationRule rule(String name, double density,
                                       int w, int h, int hbW, int hbH, boolean solid) {
        return new VegetationRule(name, density, w, h, hbW, hbH, solid);
    }
}
