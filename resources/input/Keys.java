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
        // Escape is handled by a window-scoped Swing key binding in GamePanel.wireInput()
        // so it works regardless of which component holds focus. Handling it here too would
        // double-toggle the menu (open then close) whenever this panel has focus.

        // Escape menu is modal: gameplay keys are ignored until resumed.
        if (panel.userInterface != null && panel.userInterface.isMenuOpen()) {
            return;
        }

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

        if (code == KeyEvent.VK_I){
            panel.inputHandlingSystem.setAimUp(true);
        }

        if (code == KeyEvent.VK_J){
            panel.inputHandlingSystem.setAimLeft(true);
        }

        if (code == KeyEvent.VK_K){
            panel.inputHandlingSystem.setAimDown(true);
        }

        if (code == KeyEvent.VK_L){
            panel.inputHandlingSystem.setAimRight(true);
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
            handleLightAttack();
        }

        if (code == KeyEvent.VK_G){
            handleHeavyAttack();
        }

        if (code == KeyEvent.VK_R){
            handleRangedAttack();
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

        if (code == KeyEvent.VK_I){
            panel.inputHandlingSystem.setAimUp(false);
        }

        if (code == KeyEvent.VK_J){
            panel.inputHandlingSystem.setAimLeft(false);
        }

        if (code == KeyEvent.VK_K){
            panel.inputHandlingSystem.setAimDown(false);
        }

        if (code == KeyEvent.VK_L){
            panel.inputHandlingSystem.setAimRight(false);
        }
    } 

    private void handleLightAttack() {
        BoatRideComponent ride = activeRide();
        if (isOnline()) {
            if (ride != null) {
                showOnlineBoatCombatToast();
                return;
            }
            panel.inputHandlingSystem.enqueueAction(InputAction.ATTACK);
            return;
        }
        if (ride != null && ride.boat() != null) {
            ride.boat().fireBroadside();
            return;
        }
        panel.player.requestLightAttack();
    }

    private void handleHeavyAttack() {
        BoatRideComponent ride = activeRide();
        if (isOnline()) {
            if (ride != null) {
                showOnlineBoatCombatToast();
                return;
            }
            panel.inputHandlingSystem.enqueueAction(InputAction.ATTACK);
            return;
        }
        if (ride == null || ride.boat() == null) {
            panel.player.requestHeavyAttack();
        }
    }

    private void handleRangedAttack() {
        BoatRideComponent ride = activeRide();
        if (isOnline()) {
            if (ride != null) {
                showOnlineBoatCombatToast();
                return;
            }
            panel.inputHandlingSystem.enqueueAction(InputAction.ATTACK);
            return;
        }
        if (ride == null || ride.boat() == null) {
            panel.player.requestRangedAttack();
        }
    }

    private BoatRideComponent activeRide() {
        if (panel.player == null) return null;
        BoatRideComponent ride = panel.player.getComponent(BoatRideComponent.class);
        return (ride != null && ride.boat() != null) ? ride : null;
    }

    private boolean isOnline() {
        return panel.multiplayer() != null && panel.multiplayer().isOnline();
    }

    private void showOnlineBoatCombatToast() {
        if (panel.userInterface != null) {
            panel.userInterface.showToast("Boat combat is offline-only for now", 1500);
        }
    }
    
}
