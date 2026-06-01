package resources.input;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import resources.app.GamePanel;
import resources.domain.object.BoatRideComponent;

import resources.geometry.Vector;

public class InputHandlingSystem {
    
    boolean up,left,down,right = false;
    boolean aimUp,aimLeft,aimDown,aimRight = false;
    GamePanel panel;
    private final ArrayDeque<InputAction> actionQueue = new ArrayDeque<>();


    public InputHandlingSystem(GamePanel panel){
        this.panel = panel;
    }

    public void update(double delta){
        // A dead player is frozen: held movement keys must not keep feeding
        // velocity into the corpse. Without this the input system kept adding
        // velocity every frame regardless of death — invisible in single-player
        // (Playable.update early-returns and never drains it, so the player
        // lurched on respawn) and a live drift online (where reconciliation runs
        // against the server pose). Gate at the source and zero any residual so
        // both paths stay still. See DeadInputFreezeProbe.
        if (isLocalPlayerDead()) {
            if (panel.player() != null) panel.player().setVelocity(new Vector(0, 0));
            panel.world().setHoveredEntity(panel.mouse().x, panel.mouse().y);
            return;
        }

        // While the player is riding a boat, the boat reads these flags
        // directly to steer; routing them into player velocity too would
        // both drift the player off the boat and double-handle the input.
        boolean riding = panel.player() != null
            && (panel.player().hasComponent(BoatRideComponent.class)
                || (panel.multiplayer() != null && panel.multiplayer().isOnline()
                    && panel.multiplayer().localPlayerRiding()));

        // Online play uses client-side prediction: local input moves the player
        // immediately, then MultiplayerRuntime reconciles against the server.
        // Single-player uses the same path, which keeps collision behaviour in
        // parity instead of special-casing online movement.
        if (!riding) {
            if (up){
                panel.player().addVelocity(new Vector(0,-2*delta));
            }

            if (left){
                panel.player().addVelocity(new Vector(-2*delta,0));
            }

            if (down){
                panel.player().addVelocity(new Vector(0,2*delta));
            }

            if (right){
                panel.player().addVelocity(new Vector(2*delta,0));
            }
        }

        //setting the hovered entities from mouse input
        panel.world().setHoveredEntity(panel.mouse().x,panel.mouse().y);
    }

    /**
     * True when the locally-controlled player is dead. Online play defers to the
     * server-authoritative flag ({@code MultiplayerRuntime.localPlayerDead});
     * single-player reads the player's own {@link PlayerLifecycle}.
     */
    private boolean isLocalPlayerDead() {
        if (panel.multiplayer() != null && panel.multiplayer().isOnline()) {
            return panel.multiplayer().localPlayerDead();
        }
        return panel.player() != null
            && panel.player().lifecycle() != null
            && panel.player().lifecycle().isDead();
    }

    public void setDown(boolean val){
        down = val;
    }
    public void setUp(boolean val){
        up = val;
    }
    public void setLeft(boolean val){
        left = val;
    }
    public void setRight(boolean val){
        right = val;
    }

    public boolean isUp()    { return up; }
    public boolean isLeft()  { return left; }
    public boolean isDown()  { return down; }
    public boolean isRight() { return right; }
    public boolean isAimUp()    { return aimUp; }
    public boolean isAimLeft()  { return aimLeft; }
    public boolean isAimDown()  { return aimDown; }
    public boolean isAimRight() { return aimRight; }

    public void setAimUp(boolean val)    { aimUp = val; }
    public void setAimLeft(boolean val)  { aimLeft = val; }
    public void setAimDown(boolean val)  { aimDown = val; }
    public void setAimRight(boolean val) { aimRight = val; }

    /** Drop all currently-held movement/aim keys (used when opening modal UI). */
    public void clearHeldInput() {
        up = false;
        left = false;
        down = false;
        right = false;
        aimUp = false;
        aimLeft = false;
        aimDown = false;
        aimRight = false;
    }

    /**
     * Combat aim is keyboard-first:
     * 1) explicit I/J/K/L aim keys
     * 2) current movement keys
     * 3) player's facing direction
     */
    public Vector combatAimVector() {
        double ax = 0;
        double ay = 0;
        if (aimUp) ay -= 1;
        if (aimDown) ay += 1;
        if (aimLeft) ax -= 1;
        if (aimRight) ax += 1;

        if (ax == 0 && ay == 0) {
            if (up) ay -= 1;
            if (down) ay += 1;
            if (left) ax -= 1;
            if (right) ax += 1;
        }

        if (ax == 0 && ay == 0 && panel.player() != null) {
            Vector facing = panel.player().getFacingVector();
            if (facing != null) return facing.normalize(1.0);
        }

        Vector aim = new Vector(ax, ay);
        if (aim.hasNoVelocity()) return new Vector(1, 0);
        return aim.normalize(1.0);
    }

    /** Hard cap on the action queue so a stuck consumer (e.g. running offline
     *  while a producer keeps enqueueing) doesn't grow memory unbounded. The
     *  oldest action is dropped when full. */
    private static final int MAX_QUEUED_ACTIONS = 256;

    public void enqueueAction(InputAction action) {
        if (action == null) return;
        while (actionQueue.size() >= MAX_QUEUED_ACTIONS) actionQueue.pollFirst();
        actionQueue.addLast(action);
    }

    /** Drain one-shot actions captured since last poll. */
    public List<InputAction> drainActions() {
        ArrayList<InputAction> out = new ArrayList<>(actionQueue.size());
        while (!actionQueue.isEmpty()) out.add(actionQueue.removeFirst());
        return out;
    }
}
