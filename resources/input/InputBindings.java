package resources.input;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Mutable mapping between AWT key codes and game {@link InputAction}s.
 * Stored separately from the {@link Keys} listener so bindings can be loaded
 * from settings, rebound at runtime, or replayed.
 *
 * Default bindings match the original hardcoded scheme so existing controls
 * keep working; rebinding is just {@link #bind(int, InputAction)}.
 */
public final class InputBindings {

    private final Map<Integer, InputAction> byKey = new HashMap<>();

    public InputBindings() { applyDefaults(); }

    public void bind(int keyCode, InputAction action) {
        byKey.put(keyCode, action);
    }

    public void unbind(int keyCode) {
        byKey.remove(keyCode);
    }

    /** @return the action bound to {@code keyCode}, or null. */
    public InputAction actionFor(int keyCode) {
        return byKey.get(keyCode);
    }

    public void clear() { byKey.clear(); }

    private void applyDefaults() {
        bind(KeyEvent.VK_W,      InputAction.MOVE_UP);
        bind(KeyEvent.VK_S,      InputAction.MOVE_DOWN);
        bind(KeyEvent.VK_A,      InputAction.MOVE_LEFT);
        bind(KeyEvent.VK_D,      InputAction.MOVE_RIGHT);
        bind(KeyEvent.VK_UP,     InputAction.MOVE_UP);
        bind(KeyEvent.VK_DOWN,   InputAction.MOVE_DOWN);
        bind(KeyEvent.VK_LEFT,   InputAction.MOVE_LEFT);
        bind(KeyEvent.VK_RIGHT,  InputAction.MOVE_RIGHT);
        bind(KeyEvent.VK_E,      InputAction.INTERACT);
        bind(KeyEvent.VK_I,      InputAction.OPEN_INVENTORY);
        bind(KeyEvent.VK_SPACE,  InputAction.ATTACK);
        bind(KeyEvent.VK_F,      InputAction.PLACE);
        bind(KeyEvent.VK_F3,     InputAction.TOGGLE_DEBUG);
        bind(KeyEvent.VK_R,      InputAction.NEW_SEED);
    }
}
