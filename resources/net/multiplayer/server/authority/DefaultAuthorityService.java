package resources.net.multiplayer.server.authority;

import java.util.HashMap;
import java.util.Map;

import resources.net.multiplayer.MultiplayerAction;

/**
 * Baseline anti-cheat and spam throttle policy.
 */
public final class DefaultAuthorityService implements AuthorityService {

    private static final long ACTION_COOLDOWN_TICKS = 3L;

    private final Map<String, Long> actionTick = new HashMap<>();

    @Override
    public boolean acceptsSequence(long incoming, long lastAccepted) {
        return incoming > 0L && incoming > lastAccepted;
    }

    @Override
    public boolean canMove(double moveX, double moveY, double maxSpeedPerTick) {
        double speed = Math.sqrt((moveX * moveX) + (moveY * moveY));
        return speed <= (maxSpeedPerTick + 0.0001);
    }

    @Override
    public boolean canPerformAction(String playerId, MultiplayerAction action, long serverTick) {
        if (playerId == null || playerId.isBlank() || action == null) return false;
        String key = playerId + ":" + action.name();
        Long last = actionTick.get(key);
        if (last != null && (serverTick - last.longValue()) < ACTION_COOLDOWN_TICKS) return false;
        actionTick.put(key, serverTick);
        return true;
    }

    @Override
    public boolean withinRange(double fromX, double fromY, double toX, double toY, double maxRange) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return ((dx * dx) + (dy * dy)) <= (maxRange * maxRange);
    }
}
