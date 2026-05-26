package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

import java.awt.Graphics2D;

/**
 * Renders either the full inventory grid (when {@code visible}) or just the
 * hotbar row pinned to the bottom of the screen. The selected hotbar slot is
 * highlighted; the source of truth for selection is {@link Inventory#getIndex()}.
 */
public class PlayerInventory extends ItemContainer {

    public PlayerInventory(GamePanel panel, int rows, int cols, int x, int y, Inventory inventory) {
        super(panel, rows, cols, x, y, inventory);
    }

    @Override
    public void draw(Graphics2D g2) {
        if (visible) {
            drawRect(g2);
            int count = 0;
            for (Component comp : content) {
                if (comp instanceof ItemContainerSlot) {
                    ((ItemContainerSlot) comp).draw(g2, count, inventory);
                }
                count++;
            }
            return;
        }
        drawHotbar(g2);
    }

    private void drawHotbar(Graphics2D g2) {
        if (inventory == null) return;
        drawBarRect(g2);
        int counter = 0;
        int hotbarOffset = Inventory.HOTBAR_OFFSET;
        int selectedSlot = inventory.getIndex() + hotbarOffset;
        int hotbarY = panel.height - (100 - padding / 2);
        for (Component comp : content) {
            if (counter >= hotbarOffset && comp instanceof ItemContainerSlot) {
                boolean indexed = (counter == selectedSlot);
                ((ItemContainerSlot) comp).drawRectInPos(g2, comp.x, hotbarY, indexed);
            }
            counter++;
        }
    }

    public void drawBarRect(Graphics2D g2) {
        y = panel.height - 100;
        height = 70;
        drawRect(g2);
    }
}
