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
 * {@link resources.domain.inventory.ItemTypeRegistry}. Seeded once on first
 * access; gameplay or mods can append further recipes via {@link #register}.
 */
public final class RecipeRegistry {

    private static final RecipeRegistry INSTANCE = new RecipeRegistry();

    private final List<Recipe> recipes = new ArrayList<>();
    private boolean seeded = false;

    private RecipeRegistry() {}

    public static RecipeRegistry instance() {
        if (!INSTANCE.seeded) INSTANCE.seedDefaults();
        return INSTANCE;
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
        seeded = true;

        // Stone toolset
        register(ShapelessRecipe.of("axe_from_stone")
            .ingredient("stone", 3)
            .ingredient("wheat", 2)        // wheat doubling as "stick"-stand-in
            .produces("axe", 1)
            .build());

        register(ShapelessRecipe.of("pickaxe_from_stone")
            .ingredient("stone", 3)
            .ingredient("wheat", 2)
            .produces("pickaxe", 1)
            .build());

        register(ShapelessRecipe.of("hammer_from_stone")
            .ingredient("stone", 4)
            .ingredient("wheat", 1)
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
