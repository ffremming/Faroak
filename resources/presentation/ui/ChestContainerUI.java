package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

/**
 * 3x9 item-container UI bound to a {@link resources.domain.object.ChestInventory}.
 * The chest's wood palette was promoted to the shared house style
 * ({@link ItemContainer#PANEL_BG}/{@code PANEL_FG}/{@code PANEL_PADDING}), so
 * this class now only supplies the grid dimensions — the look comes for free
 * from {@link ItemContainer}.
 */
public class ChestContainerUI extends ItemContainer {

    private static final int ROWS = 3;
    private static final int COLS = 9;

    public ChestContainerUI(GamePanel panel, Inventory chestInv, int x, int y) {
        super(panel, ROWS, COLS, x, y, chestInv);
    }
}
