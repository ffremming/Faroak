package resources.generation.plant;

import java.util.Arrays;
import java.util.List;

/**
 * Slug list for the hand-authored plant pack sliced from
 * {@code resources/images/objects/_spritesheets/plants_green.png}.
 *
 * Sizing, hitboxes and solidity for these slugs live in
 * {@link resources.generation.factory.ObjectCatalog}; biome density rules
 * live in {@link resources.generation.biome.BiomeRegistry}. This class only
 * exists so the post-spawn demo grid in
 * {@link resources.app.GenerationManager} has a single place to enumerate the
 * pack without each caller hard-coding twenty names.
 *
 * The earlier procedural pipeline (PlantSpec / PlantFeatureLibrary /
 * PlantImageGenerator) is retained in this package for reference but is no
 * longer wired into bootstrap — once an artist supplied the spritesheet the
 * procedural sprites only added drift.
 */
public final class PlantCatalog {

    private static final List<String> SLUGS = Arrays.asList(
        // Ground decor (small, non-solid)
        "plant_clover",
        "plant_fern",
        "plant_grass_tuft",
        "plant_bush_small",
        "plant_bush_leafy",
        "plant_bush_berry",
        "plant_palm_frond",
        // Small/medium trees
        "plant_sapling",
        "plant_oak_round",
        "plant_conifer",
        "plant_oak_large",
        "plant_palm_tree",
        // Large showcase trees
        "plant_bonsai",
        "plant_willow_large",
        "plant_oak_mega",
        // Misc ground items
        "plant_mushroom_red",
        "plant_mushroom_tall",
        "plant_mushroom_cluster",
        "plant_log_short",
        "plant_log_long"
    );

    private PlantCatalog() {}

    /** Backwards-compatible no-op — kept so existing call sites in
     *  {@link resources.app.GenerationManager} don't need to be reordered. */
    public static void bootstrap() {}

    /** Slugs in the order the demo grid wants to display them. */
    public static List<String> slugs() { return SLUGS; }
}
