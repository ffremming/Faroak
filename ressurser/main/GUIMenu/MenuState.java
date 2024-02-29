package ressurser.main.GUIMenu;

import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.event.MouseWheelEvent;

import ressurser.main.GamePanel;
import ressurser.meny.items.Item;

public class MenuState extends ScreenState{

    Item chosenItem = null;
    ItemContainer itemC;
    ItemBar itemBar;
    public MenuState(GamePanel panel) {
        super(panel);
       

        itemC = new ItemContainer(panel,3,8,60,60);
        itemC.setX(50);
        itemC.setY(50);

        itemBar = new ItemBar(panel,1,9,60,60);
        itemBar.setX(panel.getWidth()/2-itemBar.width/2);
        itemBar.setY(panel.getHeight()-itemBar.height+10);

        itemBar.setX(200);
        itemBar.setY(500);
        
        add(itemBar);
        add(itemC);
        setVisible(false);
    }

    
    public void drawChosenItem(Graphics2D g2){
        if (chosenItem!= null){
            pointerI = MouseInfo.getPointerInfo();
            g2.drawImage(chosenItem.sprite,(int)pointerI.getLocation().getX()-panel.getLocationOnScreen().x,(int)pointerI.getLocation().getY()-panel.getLocationOnScreen().y,null);
            System.out.println("drawing");
            System.out.println((int)pointerI.getLocation().getX()+","+(int)pointerI.getLocation().getY());
        } 
    }

    public void closeScreenState(){
        returnChosenItem();
    }

    private void returnChosenItem(){
        if (chosenItem != null){
            for (Component comp: itemC.content){
                if (((ItemContainerSlot) comp).getItem() == null){
                    ((ItemContainerSlot) comp).addItem(chosenItem);
                }

            }
        }
    }
    public void mouseWheelMoved(MouseWheelEvent e){
        super.mouseWheelMoved(e);
        itemBar.mouseWheelMoved(e);
        System.out.println("wheel");

    }
}
