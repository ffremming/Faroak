package resources.presentation.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import resources.app.GamePanel;
import resources.domain.crafting.CraftingService;
import resources.domain.inventory.Stack;

/**
 * Read-only output slot for a crafting table. Renders the {@link CraftingService}
 * preview stack and, on left-click, commits the recipe — handing the produced
 * stack to the player's "in-hand" cursor (or stacking onto it if the cursor
 * already holds the same item).
 *
 * Not a {@link ItemContainerSlot} because it's never a destination for the
 * player to drop items into: dropping into the output would silently destroy
 * them. Lives in its own subclass so the crafting UI can compose it alongside
 * a regular {@link ItemContainer} input grid.
 */
public final class CraftingOutputSlot extends Component {

    private final CraftingService service;

    public CraftingOutputSlot(GamePanel panel, CraftingService service,
                              int x, int y, int size) {
        super(panel);
        this.service = service;
        this.x = x; this.y = y;
        this.width  = size;
        this.height = size;
        this.borderSize = 2;
        setBackground(new Color(70, 70, 70, 230));
        setForeGround(new Color(220, 200, 120));
    }

    @Override
    public void drawRect(Graphics2D g2) {
        g2.setColor(background);
        g2.fillRect(x, y, width, height);
        g2.setColor(foreground);
        g2.setStroke(new BasicStroke(borderSize));
        g2.drawRect(x, y, width, height);

        Stack out = service.output();
        if (out == null || out.isEmpty() || "empty".equals(out.getName())) return;
        BufferedImage img = panel.imageContainer.getItemImage(out.getName());
        g2.drawImage(img, x + 4, y + 4, width - 8, height - 8, null);
        g2.setColor(Color.white);
        g2.drawString(out.getAmount() + "", x + width - 14, y + height - 8);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        takeOutput();
    }

    /**
     * Commit the recipe: produce the result and merge it onto the player's
     * cursor. If the cursor already holds the same item, top it up; if it
     * holds something else, refuse rather than overwrite (Minecraft rule).
     *
     * Pre-flight capacity check: if the produced stack cannot fit entirely
     * into hand + inventory, the craft is refused (no ingredient consumption,
     * no item loss). Without this, calling addStack() twice could silently
     * destroy any overflow that exceeded both destinations.
     */
    private void takeOutput() {
        Stack hand = panel.player.getTempInHand();
        Stack preview = service.output();
        if (preview == null || preview.isEmpty() || "empty".equals(preview.getName())) return;

        boolean handEmpty = (hand == null) || hand.isEmpty();
        if (!handEmpty && !hand.getName().equals(preview.getName())) return;

        int needed = preview.getAmount();
        int capacity = freeCapacityFor(preview.getName(), hand);
        if (capacity < needed) return; // refuse rather than destroy items

        Stack produced = service.craft();
        if (produced == null || produced.isEmpty()) return;

        if (handEmpty) {
            panel.player.setTempInHand(produced);
        } else {
            hand.addStack(produced);
            // capacity check above guarantees the inventory can absorb any
            // overflow — but assert it anyway so a future change to addStack
            // can't silently regress to item loss.
            if (!produced.isEmpty()) {
                panel.player.getInventory().addStack(produced);
            }
        }
    }

    /**
     * Total slots-worth-of-room for an item named {@code itemName} across
     * the cursor stack (if it matches) and the main inventory. Counts:
     *  - the empty cursor: stackLimit (if hand is empty/null)
     *  - the same-name cursor: stackLimit - amount
     *  - each "empty" slot in the inventory: stackLimit
     *  - each same-name slot: stackLimit - amount
     */
    private int freeCapacityFor(String itemName, Stack hand) {
        int capacity = 0;
        boolean handEmpty = (hand == null) || hand.isEmpty();
        if (handEmpty) {
            // Cursor can carry up to stackLimit of anything when empty. Use the
            // limit from the preview itself (same type) so we don't need an
            // ItemType lookup here.
            capacity += service.output().getStackLimit();
        } else if (itemName.equals(hand.getName())) {
            capacity += hand.getStackLimit() - hand.getAmount();
        }
        var inv = panel.player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            Stack s = inv.getStack(i);
            if (s == null) continue;
            if (s.isEmpty() || "empty".equals(s.getName())) {
                capacity += service.output().getStackLimit();
            } else if (itemName.equals(s.getName())) {
                capacity += s.getStackLimit() - s.getAmount();
            }
        }
        return capacity;
    }
}
