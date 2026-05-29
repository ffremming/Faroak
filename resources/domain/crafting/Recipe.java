package resources.domain.crafting;

import resources.domain.inventory.Stack;

/**
 * Strategy contract for a crafting recipe. Implementations decide whether a
 * grid state satisfies the recipe and what the produced stack looks like.
 *
 * Two flavors ship today:
 *  - {@link ShapelessRecipe}: ingredient bag, position-agnostic
 *  - {@link ShapedRecipe}: positional pattern relative to a 4x4 grid
 *
 * Custom recipes can be added by implementing this interface and registering
 * with {@link RecipeRegistry#register(Recipe)}.
 */
public interface Recipe {

    /** Stable identifier ("planks_from_log"). Used for lookup + debugging. */
    String id();

    /** Does this grid satisfy the recipe? */
    boolean matches(CraftingGrid grid);

    /**
     * Produce a fresh result stack. Called only when {@link #matches} is true.
     * The returned stack is owned by the caller; recipes must not cache it.
     */
    Stack produce(CraftingGrid grid);
}
