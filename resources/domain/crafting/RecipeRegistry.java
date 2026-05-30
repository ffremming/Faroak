package resources.domain.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry of {@link Recipe}s. First-match wins on lookup, so order
 * matters when registering — more specific recipes should be registered before
 * looser shapeless ones if they share ingredients.
 *
 * Singleton pattern, mirroring
 * {@link resources.domain.inventory.ItemTypeRegistry}. Initialised via the
 * "initialization-on-demand holder" idiom: the holder class is loaded the
 * first time {@link #instance()} runs, and JVM class-loading guarantees
 * make that load atomic — so the default recipe seeding can never race
 * even if multiple threads (e.g. server tick + UI thread) hit instance()
 * simultaneously. Gameplay or mods can append further recipes via
 * {@link #register}; that list is itself not thread-safe, so registrations
 * should still happen during startup before crafting threads are running.
 */
public final class RecipeRegistry {

    private static final class Holder {
        static final RecipeRegistry INSTANCE = new RecipeRegistry();
        static {
            INSTANCE.seedDefaults();
        }
    }

    private final List<Recipe> recipes = new ArrayList<>();

    private RecipeRegistry() {}

    public static RecipeRegistry instance() {
        return Holder.INSTANCE;
    }

    public void register(Recipe r) {
        if (r == null) return;
        recipes.add(r);
    }

    public List<Recipe> all() {
        return Collections.unmodifiableList(recipes);
    }

    /** First recipe whose {@link Recipe#matches} returns true, or null. */
    public Recipe firstMatch(CraftingGrid grid) {
        for (Recipe r : recipes) {
            if (r.matches(grid)) return r;
        }
        return null;
    }

    /**
     * Default recipe book. Kept intentionally small; the assignment is to
     * build the system, not exhaustively populate it. Add more by calling
     * {@link #register} after this seeds — recipes registered later are tried
     * later (first-match-wins).
     */
    private void seedDefaults() {

        // Stone toolset. Each recipe MUST be uniquely identifiable by its
        // ingredient set — first-match-wins on the registry means two recipes
        // that share the same ingredient bag would leave the second one
        // permanently uncraftable. Distinct "binder" ingredient per tool:
        //   axe     = stone + wheat  (stick-stand-in)
        //   pickaxe = stone + iron   (iron head)
        //   hammer  = stone + hide   (wrapped grip)
        register(ShapelessRecipe.of("axe_from_stone")
            .ingredient("stone", 3)
            .ingredient("wheat", 2)
            .produces("axe", 1)
            .build());

        register(ShapelessRecipe.of("pickaxe_from_stone")
            .ingredient("stone", 3)
            .ingredient("iron_ore", 1)
            .produces("pickaxe", 1)
            .build());

        register(ShapelessRecipe.of("hammer_from_stone")
            .ingredient("stone", 4)
            .ingredient("hide", 1)
            .produces("hammer", 1)
            .build());

        // Placeables
        register(ShapelessRecipe.of("barrel_from_wheat")
            .ingredient("wheat", 6)
            .produces("barrel", 1)
            .build());

        register(ShapelessRecipe.of("fence_from_wheat")
            .ingredient("wheat", 2)
            .produces("fence", 2)
            .build());

        register(ShapelessRecipe.of("torch_from_meat")
            .ingredient("meat", 1)
            .ingredient("wheat", 1)
            .produces("torch", 4)
            .build());

        // Iron pathway
        register(ShapelessRecipe.of("sword_from_iron")
            .ingredient("iron_ore", 2)
            .ingredient("wheat", 1)
            .produces("sword", 1)
            .build());

        register(ShapelessRecipe.of("crafting_table_from_wheat")
            .ingredient("wheat", 4)
            .produces("crafting_table", 1)
            .build());
    }
}
