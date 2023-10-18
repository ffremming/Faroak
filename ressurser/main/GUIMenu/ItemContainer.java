package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.Graphics2D;

import ressurser.main.GamePanel;
import ressurser.meny.items.Item;

public class ItemContainer extends Container {

    //layout == inventory

    int indeks = 0;
    int maxIndeks = 8;
    int minIndeks = 0;
    

    public ItemContainer(GamePanel panel,int rows,int cols,int slotWidth,int slotHeight) {
        super(panel);
        layout = INVENTORY;
        //maxIndeks = cols-1;

        this.rows = rows;
        this.cols = cols;
        this.slotHeight = slotHeight;
        this.slotWidth = slotWidth;


        addSlots();
        setWidth(slotWidth*cols+20);
        setHeight(slotHeight*rows+20);
    }

    private void addSlots(){
        for (int i = 0;i<cols*rows;i++){
            ItemContainerSlot itemCS = new ItemContainerSlot(panel);
            itemCS.setHeight(slotHeight);
            itemCS.setWidth(slotWidth);
            add(itemCS);
            if (i<6){
                itemCS.addItem((Item) panel.menu.items.content.get(i));
              
            }

            
            
        }
        content.get(0).addItem((Item) panel.menu.materials.content.get(0));
    }

    public void mouseReleased(){
        //if an item is chosen, place item on chosen slot
    }

    public void mouseDragged(){
        //items might be spreaded
    }
    public  void draw(Graphics2D g2){
        super.draw(g2);
        drawIndexSlot(g2);
    }

    private void drawIndexSlot(Graphics2D g2){
        ItemContainerSlot slot = (ItemContainerSlot) content.get(indeks);
        g2.setColor(Color.white);
        g2.drawRect(slot.x,slot.y,slot.width,slot.height);
    }
    
}
