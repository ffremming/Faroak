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
     */
    private void takeOutput() {
        Stack hand = panel.player.getTempInHand();
        Stack preview = service.output();
        if (preview == null || preview.isEmpty() || "empty".equals(preview.getName())) return;

        boolean handEmpty = (hand == null) || hand.isEmpty();
        if (!handEmpty && !hand.getName().equals(preview.getName())) return;

        Stack produced = service.craft();
        if (produced == null || produced.isEmpty()) return;

        if (handEmpty) {
            panel.player.setTempInHand(produced);
        } else {
            hand.addStack(produced);
            // any overflow remains in `produced`; drop it into the player's
            // main inventory so we never destroy items.
            if (!produced.isEmpty()) {
                panel.player.getInventory().addStack(produced);
            }
        }
    }
}
