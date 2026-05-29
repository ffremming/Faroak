package resources.domain.crafting;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Stack;

/**
 * 4x4 input grid for the crafting system. Extends {@link Inventory} so it can
 * be rendered by the existing {@link resources.presentation.ui.ItemContainer}
 * machinery and click/drag handlers — the base class always allocates its
 * default 36-slot backing list, but {@link #getSize()} exposes only the 16
 * cells the crafting UI is allowed to touch (slots 0..15).
 *
 * The grid is intentionally dumb storage: it does not know about recipes or
 * outputs. A {@link CraftingService} subscribes via {@link #setOnChange(Runnable)}
 * and re-evaluates the output slot whenever the grid mutates.
 */
public class CraftingGrid extends Inventory {

    public static final int COLS = 4;
    public static final int ROWS = 4;
    public static final int GRID_SLOTS = COLS * ROWS;

    private Runnable onChange;

    public CraftingGrid(GamePanel panel) {
        super(panel);
    }

    @Override
    public int getSize() {
        return GRID_SLOTS;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public void setStack(int number, Stack stack) {
        super.setStack(number, stack);
        fireChange();
    }

    /** Consume one item from every non-empty grid slot — used after a successful craft. */
    public void consumeOne() {
        for (int i = 0; i < GRID_SLOTS; i++) {
            Stack s = getStack(i);
            if (s != null && !s.isEmpty()) {
                s.removeOneItem();
            }
        }
        fireChange();
    }

    private void fireChange() {
        if (onChange != null) onChange.run();
    }
}
