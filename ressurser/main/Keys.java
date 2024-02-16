package ressurser.main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import ressurser.baseEntity.Vector;

public class Keys implements KeyListener{

    GamePanel panel;

    public Keys(GamePanel panel){
        this.panel = panel;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
        //throw new UnsupportedOperationException("Unimplemented method 'keyTyped'");
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W){
           panel.inputHandlingSystem.setUp(true);
        }

        
        if (code == KeyEvent.VK_A){
            panel.inputHandlingSystem.setLeft(true);
        }

        if (code == KeyEvent.VK_S){
            panel.inputHandlingSystem.setDown(true);
        }

        if (code == KeyEvent.VK_D){
            panel.inputHandlingSystem.setRight(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W){
           panel.inputHandlingSystem.setUp(false);
        }

        
        if (code == KeyEvent.VK_A){
            panel.inputHandlingSystem.setLeft(false);
        }

        if (code == KeyEvent.VK_S){
            panel.inputHandlingSystem.setDown(false);
        }

        if (code == KeyEvent.VK_D){
            panel.inputHandlingSystem.setRight(false);
        }
    } 
    
}
