package ressurser.main;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import ressurser.main.interactions.InventoryInteraction;
import ressurser.main.interactions.MenuInteraction;
import ressurser.main.interactions.OptionInteraction;
import ressurser.main.interactions.PlayInteractionManager;

public class MouseHandler implements MouseListener, MouseMotionListener,MouseWheelListener {
    public int x = 0;
    public int y = 0;

    private PlayInteractionManager interactionP;
    private InventoryInteraction interactionI;
    private MenuInteraction interactionM;
    private OptionInteraction interactionO;

    GamePanel panel;
    public MouseHandler(GamePanel panel){
        this.panel = panel;
        this.interactionP = panel.interactionPlay;
        this.interactionO = panel.interactionOption;
        this.interactionI = panel.interactionInventory;
        this.interactionM = panel.interactionMenu;


        panel.addMouseListener(this);
        panel.addMouseMotionListener(this);
        panel.addMouseWheelListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (panel.gameState == panel.MENUSTATE){
            //panel.menuStateUI.mousePressed(e);
        }


        
       if (panel.gameState == panel.PLAYSTATE){
        
       } else if (panel.gameState == panel.OPTIONSTATE){
        
       } else if (panel.gameState == panel.MENUSTATE){
        
       } else if (panel.gameState == panel.INVENTORYSTATE){
        
       }
       
        panel.interactionPlay.placeObjectWithToolMouse(getXRelativeToScreen((int)e.getPoint().getX()+12),getYRelativeToScreen((int)e.getPoint().getY()-12));
       

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (panel.gameState == panel.MENUSTATE){
            panel.menuStateUI.mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
       
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        panel.interactionPlay.placeObjectWithToolMouse(getXRelativeToScreen((int)e.getPoint().getX()),getYRelativeToScreen((int)e.getPoint().getY()));
        
        //panel.interactionM.placeObjectWithToolMouse(getXRelativeToScreen((int)e.getPoint().getX()),getYRelativeToScreen((int)e.getPoint().getY()));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (panel.gameState == panel.MENUSTATE){
            panel.menuStateUI.mouseMoved(e);
        } else {

        


        panel.interactionPlay.getHighlightedTile(getXRelativeToScreen((int)e.getPoint().getX()),getYRelativeToScreen((int)e.getPoint().getY()));
        panel.objM.tempObject = panel.interactionPlay.getObjectPlaceable(getXRelativeToScreen((int)e.getPoint().getX()),getYRelativeToScreen((int)e.getPoint().getY()));
           
        
        }
        //panel.interactionM.placeTempObjectWithToolMouse(getXRelativeToScreen((int)e.getPoint().getX()),getYRelativeToScreen((int)e.getPoint().getY()));
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        
        if (panel.gameState == panel.PLAYSTATE){
            panel.menuStateUI.mouseWheelMoved(e);

            if (e.isShiftDown()) {
                System.err.println("Horizontal " + e.getWheelRotation());
            } else {
                
                if (e.getWheelRotation()== -1){
                    panel.interactionPlay.changeIndexScroll(-1);
                } else if (e.getWheelRotation()== 1){
                    panel.interactionPlay.changeIndexScroll(1);
                }
                                 
            }
        }
        
        else if (panel.gameState == panel.MENUSTATE){
            panel.menuStateUI.mouseWheelMoved(e);

        } else if (panel.gameState == panel.OPTIONSTATE){
        
         
        } else if (panel.gameState == panel.INVENTORYSTATE){
         
        }
    }

            
    
    public int getXRelativeToScreen(int screenX){
        return panel.spiller.worldX-(panel.spiller.screenX)+screenX;
    }
    public int getYRelativeToScreen(int screenY){
        return panel.spiller.worldY-(panel.spiller.screenY)+screenY;
    }
}
