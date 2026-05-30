package resources.presentation.ui;

import resources.app.GamePanel;
import resources.domain.tile.Tile;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.inventory.ItemManager;

import java.awt.Color;
import java.awt.Graphics2D;

import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.app.GamePanel;


public class ItemContainer extends Container {

    //layout == inventory

    int indeks = 0;
    int maxIndeks = 8;
    int minIndeks = 0;
    int slotHeight;
    int slotWidth;
    Inventory inventory;

    /** Cell size and inter-cell gutter used to size the grid. The gutter MUST
     *  match {@link ItemContainerSlot}'s internal PADDING (8) — that's what
     *  {@code ItemContainerSlot.layoutInGrid} divides the container width/height
     *  by at draw time. Keep them in sync or slots won't fill the box. */
    private static final int CELL   = 50;
    private static final int GUTTER = 8;

    /**
     * Shared baseline palette for ALL inventory panels (player inventory,
     * hotbar, chest, barrel, crafting). The chest's wood-brown look was chosen
     * as the single house style — every inventory UI now reads from these
     * constants instead of defining its own colors, so they stay consistent.
     * Override per-subclass only if a panel genuinely needs to stand apart.
     */
    public static final Color PANEL_BG      = new Color(95, 60, 30);
    public static final Color PANEL_FG      = new Color(50, 30, 12);
    public static final int   PANEL_PADDING = 15;

    public ItemContainer(GamePanel panel,int rows,int cols,int x,int y,Inventory inventory) {
        super(panel);
        this.inventory = inventory;
        //maxIndeks = cols-1;

        this.rows = rows;
        this.cols = cols;
        this.x = x;
        this.y = y;

        padding = PANEL_PADDING;

        // Size the container to fit a rows x cols grid of CELL-sized slots with
        // a GUTTER between/around them. Without this, width/height stay 0
        // (inherited from java.awt.Rectangle) and the slot layout collapses to
        // zero/negative-sized cells — the container opens but draws nothing,
        // which is exactly the "chest/barrel UI is invisible" bug.
        this.width  = cols * CELL + (cols + 1) * GUTTER;
        this.height = rows * CELL + (rows + 1) * GUTTER;

        this.slotHeight = ((height)+padding*2)/rows;
        this.slotWidth = (width+padding*2)/cols;


        addSlots();
        setBackground(PANEL_BG);
        setForeGround(PANEL_FG);


    }

    private void addSlots(){
        int count = 0;
        for (int i = 0;i<rows;i++){
            for (int j = 0;j<cols;j++){
                
                ItemContainerSlot itemCS = new ItemContainerSlot(panel,x+j*((width)/cols),y+i*((height)/rows),slotWidth,slotHeight,j,i,count,inventory);
                add(itemCS);
                count++;
            }
        }
    }

    public void mouseReleased(){
        //if an item is chosen, place item on chosen slot
    }

    public void mouseDragged(){
        //items might be spreaded
    }
    /**
     * Container draw. Slots paint themselves (cell + item + count overlay) via
     * {@link ItemContainerSlot#draw}. We do NOT also draw the item icon here —
     * doing so used to double-render every cell and bypass the cached icon
     * lookup that ItemContainerSlot.drawStackAt uses.
     */
    public void draw(Graphics2D g2){
        if (!visible) return;
        drawRect(g2);
        int count = 0;
        for (Component comp : content){
            if (comp instanceof ItemContainerSlot){
                ((ItemContainerSlot) comp).draw(g2, count, inventory);
            }
            count++;
        }
    }

    private void drawIndexSlot(Graphics2D g2){
        ItemContainerSlot slot = (ItemContainerSlot) content.get(indeks);
        g2.setColor(Color.white);
        g2.drawRect(slot.x,slot.y,slot.width,slot.height);
    }

    /**
     * The non-empty {@link Stack} sitting in whichever slot the cursor is over,
     * or {@code null} if the cursor isn't over an occupied slot. Used by
     * {@link UserInterface} to render the hover tooltip.
     *
     * <p>We test slot geometry against the live mouse point rather than reading
     * the slots' {@code hover} flag: that flag is set on mouseMoved but never
     * reset, so it goes stale and would light up multiple slots at once. Slot
     * rectangles are laid out fresh every frame in {@code drawRect}, so by the
     * time this runs (after the container has drawn) the coordinates are current.
     */
    public Stack stackUnderMouse(int mouseX, int mouseY) {
        if (!visible || inventory == null) return null;
        int idx = 0;
        for (Component comp : content) {
            // The crafting output cell isn't backed by an inventory index — its
            // own draw path handles its stack. Skip it (it has number == 100,
            // outside the grid range) so we never index the inventory with it.
            if (comp instanceof CraftingOutputSlot) continue;
            if (comp instanceof ItemContainerSlot) {
                if (comp.contains(mouseX, mouseY)) {
                    if (idx < 0 || idx >= inventory.getSize()) return null;
                    Stack stack = inventory.getStack(idx);
                    if (stack != null && !stack.isEmpty() && !"empty".equals(stack.getName())) {
                        return stack;
                    }
                    return null;
                }
                idx++;
            }
        }
        return null;
    }

}
