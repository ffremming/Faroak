package resources.input;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import resources.app.GamePanel;
import resources.domain.object.BoatRideComponent;

import resources.geometry.Vector;

public class InputHandlingSystem {
    
    boolean up,left,down,right = false;
    GamePanel panel;
    private final ArrayDeque<InputAction> actionQueue = new ArrayDeque<>();


    public InputHandlingSystem(GamePanel panel){
        this.panel = panel;
    }

    public void update(double delta){
        // While the player is riding a boat, the boat reads these flags
        // directly to steer; routing them into player velocity too would
        // both drift the player off the boat and double-handle the input.
        boolean riding = panel.player != null
            && panel.player.hasComponent(BoatRideComponent.class);

        if (!riding) {
            if (up){
                panel.player.addVelocity(new Vector(0,-2*delta));
            }

            if (left){
                panel.player.addVelocity(new Vector(-2*delta,0));
            }

            if (down){
                panel.player.addVelocity(new Vector(0,2*delta));
            }

            if (right){
                panel.player.addVelocity(new Vector(2*delta,0));
            }
        }

        //setting the hovered entities from mouse input
        panel.world.setHoveredEntity(panel.mouse.x,panel.mouse.y);
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
