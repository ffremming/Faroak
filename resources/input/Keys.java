package resources.input;

import resources.app.GamePanel;
import resources.domain.object.BoatRideComponent;

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
            if (panel.multiplayer() == null || !panel.multiplayer().isOnline()) {
                BoatRideComponent ride = panel.player.getComponent(BoatRideComponent.class);
                if (ride != null && ride.boat() != null) {
                    // SPACE while riding = dismount; otherwise the usual interact.
                    if (!ride.boat().dismount() && panel.userInterface != null) {
                        panel.userInterface.showToast("Steer to land to disembark", 1500);
                    }
                } else {
                    panel.player.interact();
                }
            } else {
                panel.inputHandlingSystem.enqueueAction(InputAction.INTERACT);
            }

        }

        if (code == KeyEvent.VK_F){
            if (panel.multiplayer() == null || !panel.multiplayer().isOnline()) {
                panel.player.attack();
            } else {
                panel.inputHandlingSystem.enqueueAction(InputAction.ATTACK);
            }
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
