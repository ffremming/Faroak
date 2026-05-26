package ressurser.main.GUIMenu;
import java.awt.Graphics2D;

import ressurser.baseEntity.playable.Inventory.Inventory;
import ressurser.main.GamePanel;
import java.awt.Color;

public class PlayerInventory  extends ItemContainer{

    int index = 0;
    public PlayerInventory(GamePanel panel, int rows, int cols, int x, int y, Inventory inventory) {
        super(panel, rows, cols, x, y, inventory);
        //TODO Auto-generated constructor stub
    }

    public  void draw(Graphics2D g2){

        if (visible){
            drawRect(g2);
            int count = 0;
            for (Component comp:content){
                
                if (comp instanceof ItemContainerSlot){
                    ((ItemContainerSlot)comp).draw(g2,count,inventory);
                    
                } else{
                }
            count ++;
            }
        } else {
            drawBarRect(g2);
            int counter = 0;
            boolean indexed = false;
            for (Component comp:content){
                
                if (inventory!= null){
                if (counter >= inventory.getSize()-9){
                    if (comp instanceof ItemContainerSlot){
                        if (counter == inventory.getIndex()+inventory.getSize()-9){
                            indexed = true;
                        } else {
                            indexed = false;
                        }

                        ((ItemContainerSlot)comp).drawRectInPos(g2,comp.x,panel.height-(100-padding/2),indexed);
                        
                        
                            if (inventory.getStack(rows*cols)!=null){
                                g2.drawImage(panel.imageContainer.getItemImage(inventory.getStack(rows*cols).getName()),x,y,null);
                           
                                }
                            }
                        }
                    } else{
                }
            counter ++;
            }
            
        }
    }

    

    public void drawBarRect(Graphics2D g2){
        y = panel.height-100;
        height = 70;
        drawRect(g2);

        //g2.setColor(background);
        //g2.fillRect(x,panel.height-100,width,height/4+padding);
        //g2.setColor(foreground);
        //g2.drawRect(x,panel.height-100,width,height/4+padding);
    }

    public void increseIndex(){
        index++;
    }

    public void decreaseIndex(){
        index--;
    }

    public void setIndex(int newIndex){
        this.index = newIndex;
    }

    public int getIndex(){
        return index;
    }
    
}
