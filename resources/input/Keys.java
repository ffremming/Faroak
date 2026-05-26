package ressurser.main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Keys implements KeyListener{

    GamePanel panel;

    public Keys(GamePanel panel){
        this.panel = panel;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W){
           panel.inputHandlingSystem.setUp(true);
           panel.player.nullPath();
        }

        
        if (code == KeyEvent.VK_A){
            panel.inputHandlingSystem.setLeft(true);
            panel.player.nullPath();
        }

        if (code == KeyEvent.VK_S){
            panel.inputHandlingSystem.setDown(true);
            panel.player.nullPath();
        }

        if (code == KeyEvent.VK_D){
            panel.inputHandlingSystem.setRight(true);
            panel.player.nullPath();
        }

        
        if (code == KeyEvent.VK_SPACE){
            panel.player.interact();
            
        }

        if (code == KeyEvent.VK_T){
            panel.camera.toggleTestData();
            
        }
       

        if (code == KeyEvent.VK_N){
            //new seed
            panel.newSeed();

        }

        if (code == KeyEvent.VK_E){
            panel.userInterface.toggleInventory();
        }
            

        if (code == KeyEvent.VK_ESCAPE){
            panel.userInterface.toggleMenu();
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
