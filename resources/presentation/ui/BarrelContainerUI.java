package resources.presentation.ui;

import java.awt.Color;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

/**
 * 3x3 item-container UI bound to a {@link resources.domain.object.BarrelInventory}.
 *
 * Reuses the regular {@link ItemContainer} grid/slot rendering, just with a
 * brown palette to visually distinguish it from the player inventory.
 */
public class BarrelContainerUI extends ItemContainer {

    private static final int ROWS = 3;
    private static final int COLS = 3;

    private static final Color BG = new Color(110, 70, 35);
    private static final Color FG = new Color(60, 35, 15);

    public BarrelContainerUI(GamePanel panel, Inventory barrelInv, int x, int y) {
        super(panel, ROWS, COLS, x, y, barrelInv);
        setBackground(BG);
        setForeGround(FG);
        setPadding(15);
    }
}
