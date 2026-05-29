package resources.presentation.ui;

import java.awt.Color;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

/**
 * 3x9 item-container UI bound to a {@link resources.domain.object.ChestInventory}.
 * Reuses the generic {@link ItemContainer} grid/slot rendering with a wood
 * palette to distinguish chests visually from barrels (richer brown) and the
 * player inventory (grey).
 */
public class ChestContainerUI extends ItemContainer {

    private static final int ROWS = 3;
    private static final int COLS = 9;

    private static final Color BG = new Color(95, 60, 30);
    private static final Color FG = new Color(50, 30, 12);

    public ChestContainerUI(GamePanel panel, Inventory chestInv, int x, int y) {
        super(panel, ROWS, COLS, x, y, chestInv);
        setBackground(BG);
        setForeGround(FG);
        setPadding(15);
    }
}
