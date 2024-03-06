package ressurser.main.GUIMenu;
import java.awt.Graphics2D;

import ressurser.baseEntity.playable.Inventory.Inventory;
import ressurser.main.GamePanel;

public class PlayerInventory  extends ItemContainer{

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
                    ((ItemContainerSlot)comp).draw(g2);
                    if (inventory!= null){
                        g2.drawImage(panel.imageContainer.getItemImage(inventory.getStack(rows*cols-1).getName()),x,y,null);
                    }
                } else{
                }
            }
        } else {
            drawBarRect(g2);
            int counter = 0;
            for (Component comp:content){
                
                if (inventory!= null){
                if (counter >= inventory.getSize()-9){
                    if (comp instanceof ItemContainerSlot){
                        ((ItemContainerSlot)comp).drawRectInPos(g2,comp.x,panel.height-(100-padding/2));
                        
                        System.out.println("barrect");
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
    
}
