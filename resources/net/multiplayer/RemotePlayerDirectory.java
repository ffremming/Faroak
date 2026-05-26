package resources.net.multiplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.net.multiplayer.message.PlayerStateMessage;

/**
 * Tracks remote avatar entities and applies authoritative snapshots.
 */
final class RemotePlayerDirectory {

    private final GameContext ctx;
    private final Map<String, RemotePlayerAvatar> byPlayerId = new HashMap<>();
    private long latestServerTick;

    RemotePlayerDirectory(GameContext ctx) {
        this.ctx = ctx;
    }

    void applySnapshot(long serverTick, boolean baseline, List<PlayerStateMessage> players, String localPlayerId) {
        if (players == null) return;
        latestServerTick = Math.max(latestServerTick, serverTick);
        Set<String> seen = new HashSet<>();
        for (PlayerStateMessage state : players) {
            if (state == null || state.playerId() == null) continue;
            // Skip the local player: the local input system is authoritative
            // for our own position. The server's SimPlayer for us is currently
            // a parallel simulation that starts at (0,0) and would yank us
            // back to origin on every snapshot. When a real authoritative
            // server exists, reconcile here against snapshot+input prediction.
            if (state.playerId().equals(localPlayerId)) continue;
            seen.add(state.playerId());
            RemotePlayerAvatar avatar = byPlayerId.computeIfAbsent(state.playerId(), id -> spawn(id, state));
            avatar.pushSnapshot(serverTick, state);
        }
        if (baseline) removeMissing(seen);
    }

    void interpolate(int interpolationDelayMs, int snapshotRate) {
        int ticks = Math.max(0, (interpolationDelayMs * Math.max(1, snapshotRate)) / 1000);
        for (RemotePlayerAvatar avatar : byPlayerId.values()) {
            avatar.advanceInterpolation(latestServerTick, ticks);
        }
    }

    private RemotePlayerAvatar spawn(String playerId, PlayerStateMessage state) {
        GamePanel panel = ctx.player().panel;
        RemotePlayerAvatar avatar = new RemotePlayerAvatar(panel, playerId, state.worldX(), state.worldY());
        ctx.world().placeEntity(avatar);
        return avatar;
    }

    private void removeMissing(Set<String> seen) {
        List<String> toRemove = new ArrayList<>();
        for (String id : byPlayerId.keySet()) {
            if (!seen.contains(id)) toRemove.add(id);
        }
        for (String id : toRemove) {
            RemotePlayerAvatar avatar = byPlayerId.remove(id);
            if (avatar != null) ctx.world().removeEntity(avatar);
        }
    }
}
