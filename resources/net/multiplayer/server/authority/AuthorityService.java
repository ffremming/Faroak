package resources.net.multiplayer.server.authority;

import resources.net.multiplayer.MultiplayerAction;

/**
 * Validates player intents against server authority policy.
 */
public interface AuthorityService {

    boolean acceptsSequence(long incoming, long lastAccepted);

    boolean canMove(double moveX, double moveY, double maxSpeedPerTick);

    boolean canPerformAction(String playerId, MultiplayerAction action, long serverTick);

    boolean withinRange(double fromX, double fromY, double toX, double toY, double maxRange);
}
