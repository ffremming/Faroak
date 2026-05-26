package resources.net.multiplayer;

import resources.input.InputAction;

/**
 * Networked high-level actions that may mutate server state.
 */
public enum MultiplayerAction {
    INTERACT,
    ATTACK,
    PLACE;

    public static MultiplayerAction fromInput(InputAction action) {
        if (action == null) return null;
        if (InputAction.INTERACT.equals(action)) return INTERACT;
        if (InputAction.ATTACK.equals(action))   return ATTACK;
        if (InputAction.PLACE.equals(action))    return PLACE;
        return null;
    }
}
