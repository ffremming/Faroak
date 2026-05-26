package resources.domain.object;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

/**
 * Lightweight 9-slot inventory used by a {@link Barrel}.
 *
 * Extends {@link Inventory} so it can plug into existing UI widgets
 * ({@link resources.presentation.ui.ItemContainer}), but reports only 9
 * usable slots via {@link #getSize()}. The base class still allocates its
 * default backing list (36 stacks) — slots 0..8 are the barrel's; the rest
 * are simply ignored by the UI.
 */
public class BarrelInventory extends Inventory {

    /** Visible slot count exposed by this inventory. */
    public static final int BARREL_SLOTS = 9;

    public BarrelInventory(GamePanel panel) {
        super(panel);
    }

    @Override
    public int getSize() {
        return BARREL_SLOTS;
    }
}
