package ressurser.main.interactions;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import ressurser.main.GamePanel;

public class MenuInteraction implements Interaction{

    GamePanel panel;

    public MenuInteraction(GamePanel panel){
        this.panel = panel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
       
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        panel.menuStateUI.mousePressed(e);

       
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

    @Override
    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub
       
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        panel.menuStateUI.mouseMoved(e);
      
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // TODO Auto-generated method stub
          throw new UnsupportedOperationException("Unimplemented method 'mouseWheelMoved'");
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
       
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // TODO Auto-generated method stub
      
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
      
    }
    
}
