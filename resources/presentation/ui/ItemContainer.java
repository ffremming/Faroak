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
    

    public ItemContainer(GamePanel panel,int rows,int cols,int x,int y,Inventory inventory) {
        super(panel);
        this.inventory = inventory;
        //maxIndeks = cols-1;

        this.rows = rows;
        this.cols = cols;
        this.x = x;
        this.y = y;
        

       
        padding = 10;
        
        this.slotHeight = ((height)+padding*2)/rows;
        this.slotWidth = (width+padding*2)/cols;

        
        addSlots();
        setBackground(Color.lightGray);
        setForeGround(Color.gray);
        
        
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
    
}
