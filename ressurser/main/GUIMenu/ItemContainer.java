package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.Graphics2D;

import ressurser.baseEntity.playable.Inventory.Inventory;
import ressurser.main.GamePanel;
import ressurser.meny.items.Item;

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
        
        //maxIndeks = cols-1;

        this.rows = rows;
        this.cols = cols;
        this.x = x;
        this.y = y;
        visible = true;

       
        padding = 10;
        
        this.slotHeight = ((height)+padding*2)/rows;
        this.slotWidth = (width+padding*2)/cols;
        addSlots();
        setBackground(Color.lightGray);
        setForeGround(Color.gray);
        
        this.inventory = inventory;
    }

    private void addSlots(){
        for (int i = 0;i<rows;i++){
            for (int j = 0;j<cols;j++){
                
                ItemContainerSlot itemCS = new ItemContainerSlot(panel,x+j*((width)/cols),y+i*((height)/rows),slotWidth,slotHeight,j,i);
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
        

        if (visible){
            drawRect(g2);
           
          
           int count = 0;
            for (Component comp:content){
                
                if (comp instanceof ItemContainerSlot){
                    ((ItemContainerSlot)comp).draw(g2);
                    if (inventory!= null){
                        g2.drawImage(panel.imageContainer.getItemImage(inventory.getStack(rows*cols).getName()),x,y,null);
                    }
                   
                   

                } else{
                    
                    
                }
                
            }
        }
       

       

        
    }

    private void drawIndexSlot(Graphics2D g2){
        ItemContainerSlot slot = (ItemContainerSlot) content.get(indeks);
        g2.setColor(Color.white);
        g2.drawRect(slot.x,slot.y,slot.width,slot.height);
    }
    
}
