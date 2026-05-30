package resources.input;

import resources.app.GamePanel;
import resources.domain.object.BoatRideComponent;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

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

        // E always toggles the player inventory, even while it (or another
        // modal) is open — otherwise you couldn't close the inventory with the
        // same key that opened it. While a container overlay (chest/barrel/
        // crafting) is open, E closes that overlay instead — the inventory is
        // shown paired with it, so dismissing the chest dismisses both.
        if (code == KeyEvent.VK_E){
            if (panel.userInterface().hasOpenOverlay()) {
                panel.userInterface().closeTopOverlay();
            } else {
                panel.userInterface().toggleInventory();
            }
            return;
        }

        // I toggles the creative item catalog. Placed before the modal guard so
        // it can also close itself while open (mirrors E for the inventory).
        if (code == KeyEvent.VK_I){
            if (panel.userInterface().hasOpenOverlay()) {
                panel.userInterface().closeTopOverlay();
            } else {
                resources.presentation.ui.ItemCatalogUIBridge.toggle(panel);
            }
            return;
        }

        // Any modal UI (pause menu, or an open chest/crafting/barrel overlay)
        // captures input: gameplay keys are ignored so SPACE doesn't re-interact
        // and WASD doesn't walk the player while a container is open. Escape is
        // routed separately (window-scoped binding) so it can still close them.
        if (panel.userInterface() != null && panel.userInterface().isModalUIOpen()) {
            return;
        }

        if (code == KeyEvent.VK_W){
           panel.input().setUp(true);
           panel.player().nullPath();
        }

        
        if (code == KeyEvent.VK_A){
            panel.input().setLeft(true);
            panel.player().nullPath();
        }

        if (code == KeyEvent.VK_S){
            panel.input().setDown(true);
            panel.player().nullPath();
        }

        if (code == KeyEvent.VK_D){
            panel.input().setRight(true);
            panel.player().nullPath();
        }

        // Combat aim-up moved from I to U: I now opens the item catalog.
        if (code == KeyEvent.VK_U){
            panel.input().setAimUp(true);
        }

        if (code == KeyEvent.VK_J){
            panel.input().setAimLeft(true);
        }

        if (code == KeyEvent.VK_K){
            panel.input().setAimDown(true);
        }

        if (code == KeyEvent.VK_L){
            panel.input().setAimRight(true);
        }

        
        if (code == KeyEvent.VK_ENTER){
            if (panel.multiplayer() == null || !panel.multiplayer().isOnline()) {
                BoatRideComponent ride = panel.player().getComponent(BoatRideComponent.class);
                if (ride != null && ride.boat() != null) {
                    // ENTER while riding = dismount; otherwise the usual interact.
                    if (!ride.boat().dismount() && panel.userInterface() != null) {
                        panel.userInterface().showToast("Steer to land to disembark", 1500);
                    }
                } else {
                    panel.player().interact();
                }
            } else {
                panel.input().enqueueAction(InputAction.INTERACT);
            }

        }

        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_F){
            handleLightAttack();
        }

        if (code == KeyEvent.VK_G){
            handleHeavyAttack();
        }

        if (code == KeyEvent.VK_R){
            handleRangedAttack();
        }

        if (code == KeyEvent.VK_T){
            panel.camera().toggleTestData();
            
        }
       

        if (code == KeyEvent.VK_N){
            //new seed
            panel.newSeed();

        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W){
           panel.input().setUp(false);
        }

        
        if (code == KeyEvent.VK_A){
            panel.input().setLeft(false);
        }

        if (code == KeyEvent.VK_S){
            panel.input().setDown(false);
        }

        if (code == KeyEvent.VK_D){
            panel.input().setRight(false);
        }

        if (code == KeyEvent.VK_U){
            panel.input().setAimUp(false);
        }

        if (code == KeyEvent.VK_J){
            panel.input().setAimLeft(false);
        }

        if (code == KeyEvent.VK_K){
            panel.input().setAimDown(false);
        }

        if (code == KeyEvent.VK_L){
            panel.input().setAimRight(false);
        }
    } 

    private void handleAttack(Consumer<BoatRideComponent> offlineAction) {
        BoatRideComponent ride = activeRide();
        if (isOnline()) {
            if (ride != null) {
                showOnlineBoatCombatToast();
                return;
            }
            panel.input().enqueueAction(InputAction.ATTACK);
            return;
        }
        offlineAction.accept(ride);
    }

    private void handleLightAttack() {
        handleAttack(ride -> {
            if (ride != null && ride.boat() != null) {
                ride.boat().fireBroadside();
                return;
            }
            panel.player().requestLightAttack();
        });
    }

    private void handleHeavyAttack() {
        handleAttack(ride -> {
            if (ride == null || ride.boat() == null) {
                panel.player().requestHeavyAttack();
            }
        });
    }

    private void handleRangedAttack() {
        handleAttack(ride -> {
            if (ride == null || ride.boat() == null) {
                panel.player().requestRangedAttack();
            }
        });
    }

    private BoatRideComponent activeRide() {
        if (panel.player() == null) return null;
        BoatRideComponent ride = panel.player().getComponent(BoatRideComponent.class);
        return (ride != null && ride.boat() != null) ? ride : null;
    }

    private boolean isOnline() {
        return panel.multiplayer() != null && panel.multiplayer().isOnline();
    }

    private void showOnlineBoatCombatToast() {
        if (panel.userInterface() != null) {
            panel.userInterface().showToast("Boat combat is offline-only for now", 1500);
        }
    }
    
}
