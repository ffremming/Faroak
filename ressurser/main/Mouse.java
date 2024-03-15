package ressurser.main;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;


public class Mouse implements MouseListener, MouseMotionListener,MouseWheelListener  {

    int x = 0;
    int y = 0;
    GamePanel panel;

    public Mouse(GamePanel panel){
        this.panel = panel;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(panel.player.getInventory().getIndex());
        if (e.getWheelRotation()<0){
            panel.player.getInventory().decreaseIndex();
            System.out.println(panel.player.getInventory().getIndex());
        } else if (e.getWheelRotation()>0){
            panel.player.getInventory().increseIndex();
        }
       
        panel.userInterface.mouseWheelMoved(e);
        
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        panel.userInterface.mouseDragged(e);
       
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        this.x = e.getX();
        this.y = e.getY();
        panel.UI.mouseMoved(e);
        panel.userInterface.mouseMoved(e);
        
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        panel.userInterface.mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!panel.userInterface.isEnabled()){
            //panel.player.setPath(panel.world.getPath(panel.player,new Point(e.getX()+((int)(panel.camera.getWorldX())),e.getY()+(int)panel.camera.getWorldY())));
            panel.world.tryPlaceEntity(panel.player.getEquipped());
            System.out.println("try place entity");
        } else{
            panel.userInterface.mousePressed(e);
        }
        
        
        
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
       
        
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
    
}
