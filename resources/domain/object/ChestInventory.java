package resources.domain.object;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

/**
 * 27-slot inventory used by a {@link Chest} — a roomier sibling of
 * {@link BarrelInventory}. Reuses the base 36-slot allocation but reports
 * only the visible 27 cells via {@link #getSize()}, so the existing UI
 * widgets render a 3x9 grid.
 */
public class ChestInventory extends Inventory {

    public static final int CHEST_SLOTS = 27;

    public ChestInventory(GamePanel panel) {
        super(panel);
    }

    @Override
    public int getSize() {
        return CHEST_SLOTS;
    }
}
