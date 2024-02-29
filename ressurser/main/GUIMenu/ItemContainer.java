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
    

    public ItemContainer(GamePanel panel,int rows,int cols,int x,int y) {
        super(panel);
        
        //maxIndeks = cols-1;

        this.rows = rows;
        this.cols = cols;
        this.x = x;
        this.y = y;
        visible = true;


        addSlots();
        setWidth(64*cols+20);
        setHeight(64*rows+20);
    }

    private void addSlots(){
        for (int i = 0;i<rows;i++){
            for (int j = 0;j<cols;j++){
                ItemContainerSlot itemCS = new ItemContainerSlot(panel);
                add(itemCS);
               
            
              
              
            }
        }
      
    }

    public void mouseReleased(){
        //if an item is chosen, place item on chosen slot
    }

    public void mouseDragged(){
        //items might be spreaded
    }
    public  void draw(Graphics2D g2){
        super.draw(g2);
        //g2.fillRect(x,y,width,height);
         drawIndexSlot(g2);
    }

    private void drawIndexSlot(Graphics2D g2){
        ItemContainerSlot slot = (ItemContainerSlot) content.get(indeks);
        g2.setColor(Color.white);
        g2.drawRect(slot.x,slot.y,slot.width,slot.height);
    }
    
}
