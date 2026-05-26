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
        setBackground(Color.green);
        setForeGround(Color.black);
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
        if (hover) { setBackground(Color.white); setForeGround(new Color(240, 240, 240)); }
        else        { setBackground(Color.gray);  setForeGround(Color.black); }
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
        BufferedImage image = panel.imageContainer.getItemImage(stack.getName());
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
    public void mousePressed(MouseEvent e) { swapWithPlayerHand(); }

    @Override
    public void mouseMoved(MouseEvent e)   { hover(); }

    private void swapWithPlayerHand() {
        Stack tempInHand = panel.player.getTempInHand();
        Stack mine = inventory.getStack(number);
        panel.player.setTempInHand(mine);
        inventory.setStack(number, tempInHand);
    }
}
