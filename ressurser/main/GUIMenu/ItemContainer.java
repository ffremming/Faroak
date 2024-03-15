package ressurser.main.GUIMenu;

import java.awt.Color;
import java.awt.Graphics2D;

import ressurser.baseEntity.playable.Inventory.Inventory;
import ressurser.baseEntity.playable.Inventory.Item;
import ressurser.baseEntity.playable.Inventory.Stack;
import ressurser.main.GamePanel;


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
    public  void draw(Graphics2D g2){
        

        if (visible){
            drawRect(g2);
           
          
           int count = 0;
            for (Component comp:content){
                
                if (comp instanceof ItemContainerSlot){
                    ((ItemContainerSlot)comp).draw(g2);
                    if (inventory!= null){
                      
                        Stack stack =  inventory.getStack(count);
                        

                        Item item = stack.getItem(0);
                        if (item != null){
                            System.out.println("drawing item");
                            g2.drawImage(item.images.get(0),comp.x+padding,comp.y+padding,comp.width-padding*2,comp.height-padding*2,null);
                        }
                       
                    }
                   
                   

                } else{
                    
                    
                }
                count++;
                
            }
        }
       

       

        
    }

    private void drawIndexSlot(Graphics2D g2){
        ItemContainerSlot slot = (ItemContainerSlot) content.get(indeks);
        g2.setColor(Color.white);
        g2.drawRect(slot.x,slot.y,slot.width,slot.height);
    }
    
}
