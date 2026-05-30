package resources.presentation.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;

/**
 * One cell in an {@link ItemContainer} grid. Paints itself, draws the item
 * sprite (with count overlay) when an inventory is attached, and on click
 * swaps its stack with whatever the player is currently holding.
 */
public class ItemContainerSlot extends Component {

    private static final int PADDING  = 8;
    private static final int LABEL_PADDING = 10;

    Item item;
    final int col;
    final int row;
    final int number;
    final Inventory inventory;

    public ItemContainerSlot(GamePanel panel, int x, int y, int width, int height,
                             int col, int row, int number, Inventory inventory) {
        super(panel);
        borderSize = 1;
        this.x = x; this.y = y;
        this.width  = width;
        this.height = height;
        this.col = col; this.row = row;
        this.number = number;
        this.inventory = inventory;
        // Neutral dark grey background; hover swaps to lighter grey via
        // applyHoverColors. The previous green default made the hotbar look
        // visually busy alongside the world tiles.
        setBackground(new Color(50, 50, 50, 220));
        setForeGround(new Color(180, 180, 180));
    }

    public void addItem(Item item)  { this.item = item; }
    public void removeItem()        { item = null; }
    public Item getItem()           { return item; }
    public void hover()             { hover = true; }
    public void press()             {}

    public void draw(Graphics2D g2, int count, Inventory inv) { drawRect(g2); }

    @Override
    public void drawRect(Graphics2D g2) {
        applyHoverColors();
        layoutInGrid();
        paintCell(g2);
        drawStackAt(g2, x, y, number, true);
    }

    public void drawRectInPos(Graphics2D g2, int slotX, int slotY, boolean indexed) {
        applyHoverColors();
        width  = (container.width - PADDING * (container.cols + 1)) / container.cols;
        height = width;
        this.x = slotX;
        this.y = slotY;
        paintCell(g2);
        drawStackAt(g2, slotX, slotY, number, false);
        if (indexed) paintSelectedHighlight(g2, slotX, slotY);
    }

    public void drawContent(Graphics2D g2, int count, Inventory inv) {
        drawStackAt(g2, x, y, count, true);
    }

    // ---- shared helpers ----

    private void applyHoverColors() {
        if (hover) {
            setBackground(new Color(90, 90, 90, 230));
            setForeGround(new Color(230, 230, 230));
        } else {
            setBackground(new Color(50, 50, 50, 220));
            setForeGround(new Color(180, 180, 180));
        }
    }

    private void layoutInGrid() {
        width  = (container.width  - PADDING * (container.cols + 1)) / container.cols;
        height = (container.height - PADDING * (container.rows + 1)) / container.rows;
        x = container.x + col * (width  + PADDING) + PADDING;
        y = container.y + row * (height + PADDING) + PADDING;
    }

    private void paintCell(Graphics2D g2) {
        g2.setColor(background); g2.fillRect(x, y, width, height);
        g2.setColor(foreground); g2.drawRect(x, y, width, height);
    }

    private void drawStackAt(Graphics2D g2, int slotX, int slotY, int slotIndex, boolean showCount) {
        if (inventory == null) return;
        Stack stack = inventory.getStack(slotIndex);
        if (stack == null || "empty".equals(stack.getName())) return;
        BufferedImage image = panel.images().getItemImage(stack.getName());
        g2.drawImage(image, slotX, slotY, width, height, null);
        if (showCount) {
            g2.drawString(stack.getAmount() + "", slotX + width - LABEL_PADDING, slotY + height - LABEL_PADDING);
        }
    }

    private void paintSelectedHighlight(Graphics2D g2, int slotX, int slotY) {
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(5));
        g2.drawRect(slotX - 1, slotY - 1, width + 2, height + 2);
        g2.setColor(foreground);
        g2.setStroke(new BasicStroke(1));
    }

    // ---- mouse ----

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            splitWithPlayerHand();
            return;
        }
        if (e.getButton() != MouseEvent.BUTTON1) return; // ignore middle/aux
        swapWithPlayerHand();
    }

    @Override
    public void mouseMoved(MouseEvent e)   { hover(); }

    /**
     * Click semantics:
     *   - Hand empty, slot has items: pick up the slot's stack into the hand;
     *     leave a fresh empty stack in the slot.
     *   - Hand has items, slot empty: drop the held stack into the slot;
     *     hand becomes empty.
     *   - Hand and slot both have items of the SAME type: pour the hand
     *     into the slot up to the stack limit; keep any overflow in the
     *     hand. This avoids accidentally swapping (and burying) a partial
     *     stack on top of the same item.
     *   - Hand and slot both have items of DIFFERENT types: swap.
     *
     * Each branch keeps the inventory slot non-null at all times — slots
     * always hold a Stack (possibly an "empty" one); the hand may be null
     * or empty. A previous version returned the player's stack into the
     * hand without clearing the slot, causing the same Stack object to be
     * referenced from two slots simultaneously and items to appear
     * duplicated.
     */
    private void swapWithPlayerHand() {
        Stack hand = panel.player().getTempInHand();
        Stack mine = inventory.getStack(number);
        if (mine == null) return; // shouldn't happen; slots are pre-seeded

        boolean handEmpty = (hand == null) || hand.isEmpty();
        boolean slotEmpty = mine.isEmpty();

        if (handEmpty && slotEmpty) return;

        if (handEmpty) {
            // Pick up: hand takes the slot's stack; slot gets a fresh empty.
            panel.player().setTempInHand(mine);
            inventory.setStack(number, new Stack(panel, "empty"));
            return;
        }

        if (slotEmpty) {
            // Drop into empty slot.
            inventory.setStack(number, hand);
            panel.player().setTempInHand(null);
            return;
        }

        // Both occupied.
        if (mine.getName().equals(hand.getName())) {
            // Same item: pour hand into slot up to its limit.
            mine.addStack(hand);
            if (hand.isEmpty()) {
                panel.player().setTempInHand(null);
            } else {
                panel.player().setTempInHand(hand);
            }
            return;
        }

        // Different items: swap.
        panel.player().setTempInHand(mine);
        inventory.setStack(number, hand);
    }

    /**
     * Right-click semantics:
     *   - Hand empty + slot has 2+ items: pick up half (rounded up) from the
     *     slot into a new stack on the cursor; the remainder stays in the
     *     slot.
     *   - Hand has items + slot is empty or same-type: drop one item from
     *     the hand into the slot.
     *   - Otherwise: no-op.
     *
     * Splitting always preserves the total item count — no creation, no
     * destruction.
     */
    private void splitWithPlayerHand() {
        Stack hand = panel.player().getTempInHand();
        Stack mine = inventory.getStack(number);
        if (mine == null) return;

        boolean handEmpty = (hand == null) || hand.isEmpty();
        boolean slotEmpty = mine.isEmpty();

        if (handEmpty && slotEmpty) return;

        if (handEmpty) {
            if (mine.getAmount() < 2) {
                // One item or none: just pick it up like a left-click.
                swapWithPlayerHand();
                return;
            }
            int take = (mine.getAmount() + 1) / 2; // round-up half
            Stack carried = new Stack(panel, mine.getItem(), 0);
            for (int i = 0; i < take; i++) {
                Item one = mine.getOneItem();
                if (one == null) break;
                carried.addItem(one);
            }
            panel.player().setTempInHand(carried);
            return;
        }

        // Hand non-empty: drop a single item into a compatible slot.
        if (slotEmpty || mine.getName().equals(hand.getName())) {
            Item one = hand.getOneItem();
            if (one == null) return;
            if (slotEmpty) {
                // Need to overwrite the empty placeholder slot — re-seed.
                Stack fresh = new Stack(panel, one, 0);
                fresh.addItem(one);
                inventory.setStack(number, fresh);
            } else {
                if (!mine.addItem(one)) {
                    // Slot was full; refund the item back to the hand so it's
                    // never lost.
                    hand.addItem(one);
                }
            }
            if (hand.isEmpty()) panel.player().setTempInHand(null);
        }
    }
}
