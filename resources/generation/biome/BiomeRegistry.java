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
    public static final double OCEAN_LVL          = -0.175;
    public static final double SHALLOW_WATER_LVL  = -0.10;
    public static final double TIDAL_SAND_LVL     = -0.09;
    public static final double WET_BEACH_LVL      = -0.025;
    public static final double BEACH_LVL          =  0.05;
    public static final double MOUNTAIN_LVL       =  0.55;
    public static final double RIVER_THRESHOLD    =  0.04;
    public static final double RIVERBANK_THRESHOLD = 0.07;

    // Vegetation rules — only density lives here; everything else (size, hitbox,
    // solid) is read from the catalog by name.
    private static final VegetationRule OAK    = density("oak_M",          0.08);
    private static final VegetationRule BIRCH  = density("birch_M",        0.06);
    // Half-density birch for plains — scattered trees, not a forest.
    private static final VegetationRule PLAINS_BIRCH = density("birch_M",   0.03);
    private static final VegetationRule SPRUCE = density("spruce_M",       0.12);
    // Reduced-density spruce for plains — 3/4 of the standard rate, scattered not clustered.
    private static final VegetationRule PLAINS_SPRUCE = density("spruce_M", 0.09);
    // Palms now come from the plant pack (plant_palm_tree / plant_palm_frond);
    // the old standalone "palm_M" sprite was removed, like driftwood below.

    private static final VegetationRule SHRUB  = density("shrub_M",        0.10);
    private static final VegetationRule BUSH   = density("bushTree",       0.07);
    private static final VegetationRule DSHRUB = density("desert_shrub_S", 0.08);

    private static final VegetationRule GRASS  = density("wildGrass",      0.18);

    private static final VegetationRule STONE     = density("stone",      0.03);
    private static final VegetationRule STONE_HI  = density("stone",      0.08); // denser near mountains
    // "Driftwood" beach debris now uses the new plant-pack long log instead of
    // the old standalone driftwood sprite.
    private static final VegetationRule DRIFTWOOD = density("plant_log_long",  0.04);

    // Rock variant pack — densities are low so the world doesn't read as a
    // quarry; mineral rocks (crystal/iron) are rarer than the plain stones.
    private static final VegetationRule R_BOULDER_S       = density("rock_boulder_S",     0.05);
    private static final VegetationRule R_CLUSTER_S    = density("rock_cluster_S",     0.04);
    private static final VegetationRule R_BOULDER_M    = density("rock_boulder_M",     0.03);
    private static final VegetationRule R_BOULDER_L    = density("rock_boulder_L",     0.02);
    private static final VegetationRule R_SPIRE        = density("rock_spire",         0.03);
    private static final VegetationRule R_MOSSY        = density("rock_mossy",         0.03);
    private static final VegetationRule R_CRACKED      = density("rock_cracked",       0.03);
    private static final VegetationRule R_RIVER_SMOOTH = density("rock_river_smooth",  0.04);
    private static final VegetationRule R_CRYSTAL      = density("rock_crystal",       0.015);
    private static final VegetationRule R_IRON_ORE     = density("rock_iron_ore",      0.015);

    // Hand-authored plant pack (sliced from plants_green.png). Sizing and
    // solidity live in ObjectCatalog; densities here are tuned so each biome
    // reads as a mix, not a monoculture. The "mega" trees are deliberately
    // very rare so the player notices them as landmarks.
    private static final VegetationRule P_CLOVER           = density("plant_clover",           0.08);
    private static final VegetationRule P_FERN             = density("plant_fern",             0.06);
    private static final VegetationRule P_GRASS_TUFT       = density("plant_grass_tuft",       0.08);
    private static final VegetationRule P_BUSH_SMALL       = density("plant_bush_small",       0.06);
    private static final VegetationRule P_BUSH_LEAFY       = density("plant_bush_leafy",       0.05);
    private static final VegetationRule P_BUSH_BERRY       = density("plant_bush_berry",       0.03);
    private static final VegetationRule P_PALM_FROND       = density("plant_palm_frond",       0.04);
    private static final VegetationRule P_SAPLING          = density("plant_sapling",          0.05);
    private static final VegetationRule P_OAK_ROUND        = density("plant_oak_round",        0.06);
    private static final VegetationRule P_CONIFER          = density("plant_conifer",          0.07);
    private static final VegetationRule P_OAK_LARGE        = density("plant_oak_large",        0.04);
    private static final VegetationRule P_PALM_TREE        = density("plant_palm_tree",        0.04);
    private static final VegetationRule P_BONSAI           = density("plant_bonsai",           0.008);
    private static final VegetationRule P_WILLOW_LARGE     = density("plant_willow_large",     0.006);
    private static final VegetationRule P_OAK_MEGA         = density("plant_oak_mega",         0.004);
    private static final VegetationRule P_MUSHROOM_RED     = density("plant_mushroom_red",     0.03);
    private static final VegetationRule P_MUSHROOM_TALL    = density("plant_mushroom_tall",    0.02);
    private static final VegetationRule P_MUSHROOM_CLUSTER = density("plant_mushroom_cluster", 0.025);
    private static final VegetationRule P_LOG_SHORT        = density("plant_log_short",        0.02);
    private static final VegetationRule P_LOG_LONG         = density("plant_log_long",         0.01);

    public static final Biome OCEAN            = new Biome("ocean",           "ocean",           true,  Collections.emptyList());
    public static final Biome SHALLOW_WATER    = new Biome("shallowWater",    "shallowWater",    true,  Collections.emptyList());
    public static final Biome RIVER            = new Biome("river",           "ocean",           true,  Collections.emptyList());
    public static final Biome WET_BEACH        = new Biome("wetBeach",        "wetBeach",        false, Collections.emptyList());
    public static final Biome TIDAL_SAND       = new Biome("tidalSand",       "tidalSand",       false, Collections.emptyList());
    public static final Biome BEACH            = new Biome("beach",           "beach",           false,
            Arrays.asList(DRIFTWOOD, P_PALM_FROND, P_PALM_TREE, P_LOG_SHORT, STONE, R_BOULDER_S, R_RIVER_SMOOTH));
    public static final Biome RIVERBANK        = new Biome("riverbank",       "beach",           false,
            Arrays.asList(DRIFTWOOD, P_LOG_SHORT, P_GRASS_TUFT, STONE, R_BOULDER_S, R_RIVER_SMOOTH, R_CLUSTER_S));
    public static final Biome MOUNTAIN         = new Biome("mountain",        "mountain",
            Arrays.asList("mountain", "cobbleRock", "crackedStone", "boulderField", "slateRock"),
            true,  Collections.emptyList());

    public static final Biome ICY_PLAINS       = new Biome("icy plains",      "snowy Tundra",    false,
            Arrays.asList(STONE, R_SPIRE, R_CRYSTAL, R_BOULDER_S, P_CLOVER));
    public static final Biome SNOWY_TAIGA      = new Biome("snowy taiga",     "snowy taiga",     false,
            Arrays.asList(SPRUCE, P_CONIFER, P_SAPLING, P_BUSH_SMALL, STONE, R_CRYSTAL, R_BOULDER_M, P_LOG_SHORT));

    public static final Biome PLAINS           = new Biome("plains",          "plains",
            Arrays.asList("plains", "meadowGrass", "lushGrass"),
            false, Arrays.asList(PLAINS_BIRCH, PLAINS_SPRUCE, SHRUB, BUSH,
                    P_GRASS_TUFT, P_CLOVER, P_BUSH_SMALL, P_BUSH_BERRY, P_OAK_ROUND, P_SAPLING, P_OAK_LARGE,
                    STONE, R_BOULDER_S, R_CLUSTER_S, R_BOULDER_M, R_IRON_ORE));
    public static final Biome FOREST           = new Biome("forest",          "forest",
            Arrays.asList("forest", "mossyGrass", "lushGrass"),
            false, Arrays.asList(OAK, BIRCH, BUSH, SHRUB, GRASS,
                    P_OAK_ROUND, P_OAK_LARGE, P_CONIFER, P_SAPLING, P_BUSH_LEAFY, P_BUSH_SMALL,
                    P_FERN, P_CLOVER, P_MUSHROOM_RED, P_MUSHROOM_CLUSTER, P_LOG_SHORT, P_LOG_LONG, P_OAK_MEGA,
                    STONE, R_MOSSY, R_BOULDER_M, R_BOULDER_L, R_CLUSTER_S));
    public static final Biome SEASONAL_FOREST  = new Biome("seasonal forest", "seasonal forest",
            Arrays.asList("seasonal forest", "meadowGrass", "dryGrassTile", "burnedGrass"),
            false, Arrays.asList(BIRCH, OAK, BUSH, GRASS,
                    P_OAK_ROUND, P_SAPLING, P_BONSAI, P_BUSH_BERRY, P_BUSH_SMALL, P_GRASS_TUFT, P_CLOVER, P_LOG_SHORT,
                    STONE, R_BOULDER_S, R_BOULDER_M, R_MOSSY));
    public static final Biome SWAMP            = new Biome("swamp",           "swamp",
            Arrays.asList("mud", "swampMud", "peatMud", "wetMud"),
            false, Arrays.asList(SHRUB, GRASS,
                    P_FERN, P_BUSH_LEAFY, P_WILLOW_LARGE, P_MUSHROOM_TALL, P_MUSHROOM_CLUSTER, P_MUSHROOM_RED,
                    P_LOG_LONG, P_LOG_SHORT,
                    STONE, R_MOSSY, R_RIVER_SMOOTH));

    public static final Biome SAVANNA          = new Biome("savanna",         "savanna",
            Arrays.asList("savanna", "dryGrassTile", "burnedGrass"),
            false, Arrays.asList(OAK, GRASS,
                    P_GRASS_TUFT, P_BUSH_SMALL, P_BONSAI, P_OAK_LARGE,
                    STONE_HI, R_CRACKED, R_BOULDER_L, R_IRON_ORE));
    public static final Biome DESERT           = new Biome("desert",          "desert",
            Arrays.asList("desert", "paleSand", "redSand", "dustyDesert", "desertGravel"),
            false, Arrays.asList(DSHRUB,
                    P_BUSH_SMALL,
                    STONE_HI, R_CRACKED, R_SPIRE, R_IRON_ORE));
    public static final Biome RAIN_FOREST      = new Biome("rain forest",     "forest",
            Arrays.asList("forest", "mossyGrass", "lushGrass"),
            false, Arrays.asList(OAK, BUSH, SHRUB, GRASS,
                    P_PALM_TREE, P_PALM_FROND, P_OAK_LARGE, P_OAK_MEGA, P_WILLOW_LARGE,
                    P_FERN, P_BUSH_LEAFY, P_BUSH_BERRY, P_MUSHROOM_RED, P_MUSHROOM_CLUSTER, P_LOG_LONG,
                    R_MOSSY, R_BOULDER_L));

    private BiomeRegistry() {}

    /** Pick a biome from noise samples in [-1,1] for height/temperature/humidity and [0,1] for river. */
    public static Biome classify(double height, double temperature, double humidity, double river) {
        if (height <= OCEAN_LVL)         return OCEAN;
        if (height <= SHALLOW_WATER_LVL) return SHALLOW_WATER;
        if (height <= TIDAL_SAND_LVL)    return TIDAL_SAND;
        if (height <= WET_BEACH_LVL)     return WET_BEACH;
        if (river  <  RIVER_THRESHOLD)   return RIVER;
        if (height <= BEACH_LVL)         return BEACH;
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
