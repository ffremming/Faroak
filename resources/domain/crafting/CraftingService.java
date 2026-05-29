package resources.domain.crafting;

import resources.app.GamePanel;
import resources.domain.inventory.Stack;

/**
 * Coordinator for a single crafting station's runtime state. Owns the
 * input {@link CraftingGrid}, the preview output {@link Stack}, and the
 * lookup against {@link RecipeRegistry}.
 *
 * Lifecycle:
 *   1. Constructor wires the grid → onChange → {@link #refresh()} pipeline,
 *      so any UI interaction that mutates the grid automatically updates the
 *      output preview.
 *   2. The UI reads {@link #output()} to render the result slot.
 *   3. {@link #craft()} commits the recipe: hands back the produced stack
 *      and consumes one of each ingredient from the grid.
 */
public final class CraftingService {

    private final GamePanel panel;
    private final CraftingGrid grid;
    private final RecipeRegistry registry;

    private Stack output;          // current preview, or "empty" if no match
    private Recipe matched;        // cached so craft() doesn't search twice

    public CraftingService(GamePanel panel, CraftingGrid grid) {
        this(panel, grid, RecipeRegistry.instance());
    }

    public CraftingService(GamePanel panel, CraftingGrid grid, RecipeRegistry registry) {
        this.panel = panel;
        this.grid = grid;
        this.registry = registry;
        this.output = new Stack(panel, "empty");
        grid.setOnChange(this::refresh);
        refresh();
    }

    public CraftingGrid grid() { return grid; }

    /** Current preview output stack. Always non-null; "empty" when no recipe matches. */
    public Stack output() { return output; }

    /**
     * Commit the current recipe: returns the produced stack and consumes one
     * item from every non-empty grid slot. Returns null (and mutates nothing)
     * if no recipe is currently matched.
     */
    public Stack craft() {
        if (matched == null) return null;
        Stack produced = matched.produce(grid);
        grid.consumeOne();   // triggers refresh() via onChange
        return produced;
    }

    /** Re-evaluate which recipe matches the current grid and rebuild the output preview. */
    public void refresh() {
        matched = registry.firstMatch(grid);
        if (matched == null) {
            output = new Stack(panel, "empty");
        } else {
            output = matched.produce(grid);
        }
    }
}
