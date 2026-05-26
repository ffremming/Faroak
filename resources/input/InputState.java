package resources.input;

import java.util.HashSet;
import java.util.Set;

/**
 * Authoritative "what's pressed right now" snapshot in semantic terms.
 *
 * Gameplay queries actions, not raw key codes:
 *   {@code if (state.isDown(InputAction.MOVE_UP)) player.moveUp();}
 *
 * The {@link Keys} listener writes into this state via
 * {@link #setDown(InputAction, boolean)} after consulting {@link InputBindings};
 * gameplay code only reads. That keeps the read-write seam crisp and makes
 * scripting / replay possible.
 */
public final class InputState {

    private final Set<InputAction> down = new HashSet<>();

    public boolean isDown(InputAction action) { return down.contains(action); }

    public void setDown(InputAction action, boolean pressed) {
        if (pressed) down.add(action); else down.remove(action);
    }

    public void clear() { down.clear(); }
}
