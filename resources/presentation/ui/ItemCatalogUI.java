package resources.presentation.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import resources.app.GamePanel;
import resources.domain.inventory.Item;
import resources.domain.inventory.ItemType;
import resources.domain.inventory.ItemTypeRegistry;

/**
 * Creative/dev catalog: a scrollable grid of EVERY registered ItemType. Click a
 * cell to give that item to the player. Opened with key I via
 * {@link ItemCatalogUIBridge}; registered as a modal overlay so clicks route
 * here and Escape closes it.
 */
public final class ItemCatalogUI extends Container {

    private static final int CELL    = 56;
    private static final int GUTTER  = 8;
    private static final int COLS    = 8;
    private static final int HEADER  = 34;
    private static final int DEFAULT_GIVE = 16;

    private final List<ItemType> items = new ArrayList<>();
    private int scrollPx = 0;

    public ItemCatalogUI(GamePanel panel, int x, int y) {
        super(panel, x, y);
        items.addAll(ItemTypeRegistry.instance().values());
        this.width  = COLS * CELL + (COLS + 1) * GUTTER;
        this.height = Math.min(panel.screenHeight - 120,
                               HEADER + 9 * (CELL + GUTTER) + GUTTER);
        setBackground(new Color(95, 60, 30));
        setForeGround(new Color(50, 30, 12));
    }

    private int rowsTotal()     { return (items.size() + COLS - 1) / COLS; }
    private int contentHeight() { return HEADER + rowsTotal() * (CELL + GUTTER) + GUTTER; }
    private int maxScroll()     { return Math.max(0, contentHeight() - height); }

    @Override
    public void draw(Graphics2D g2) {
        if (!visible) return;
        drawRect(g2);
        g2.setColor(new Color(235, 220, 200));
        g2.drawString("Item Catalog  (click to give, scroll, Esc to close)", x + 10, y + 22);

        Shape oldClip = g2.getClip();
        g2.setClip(x, y + HEADER, width, height - HEADER);
        for (int i = 0; i < items.size(); i++) {
            Rectangle cell = cellBounds(i);
            if (cell.y + cell.height < y + HEADER || cell.y > y + height) continue; // offscreen
            g2.setColor(new Color(70, 45, 22));
            g2.fillRect(cell.x, cell.y, cell.width, cell.height);
            g2.setColor(new Color(40, 25, 10));
            g2.drawRect(cell.x, cell.y, cell.width, cell.height);
            ItemType t = items.get(i);
            BufferedImage img = panel.images().getItemImage(t.spriteName());
            if (img != null) {
                g2.drawImage(img, cell.x + 4, cell.y + 4, cell.width - 8, cell.height - 8, null);
            }
        }
        g2.setClip(oldClip);
    }

    /** Screen-space bounds of cell i, accounting for scroll. */
    private Rectangle cellBounds(int i) {
        int col = i % COLS, row = i / COLS;
        int cx = x + GUTTER + col * (CELL + GUTTER);
        int cy = y + HEADER + GUTTER + row * (CELL + GUTTER) - scrollPx;
        return new Rectangle(cx, cy, CELL, CELL);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        for (int i = 0; i < items.size(); i++) {
            Rectangle cell = cellBounds(i);
            if (cell.y + cell.height < y + HEADER || cell.y > y + height) continue;
            if (cell.contains(e.getX(), e.getY())) {
                give(items.get(i));
                return;
            }
        }
    }

    private void give(ItemType t) {
        if (panel.player() == null) return;
        int amount = Math.min(t.maxStack(), DEFAULT_GIVE);
        panel.player().addItem(new Item(panel, t.spriteName()), Math.max(1, amount));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scrollPx += e.getWheelRotation() * 40;
        if (scrollPx < 0) scrollPx = 0;
        if (scrollPx > maxScroll()) scrollPx = maxScroll();
    }
}
