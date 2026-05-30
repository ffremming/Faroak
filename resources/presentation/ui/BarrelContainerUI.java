package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

/**
 * 3x3 item-container UI bound to a {@link resources.domain.object.BarrelInventory}.
 *
 * Reuses the regular {@link ItemContainer} grid/slot rendering and the shared
 * chest house style ({@link ItemContainer#PANEL_BG} etc.), so it looks like
 * every other inventory panel — only the grid size differs.
 */
public class BarrelContainerUI extends ItemContainer {

    private static final int ROWS = 3;
    private static final int COLS = 3;

    public BarrelContainerUI(GamePanel panel, Inventory barrelInv, int x, int y) {
        super(panel, ROWS, COLS, x, y, barrelInv);
    }
}
