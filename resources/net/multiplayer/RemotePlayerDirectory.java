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

    RemotePlayerDirectory(GameContext ctx) {
        this.ctx = ctx;
    }

    void applySnapshot(long serverTick, boolean baseline, List<PlayerStateMessage> players, String localPlayerId) {
        if (players == null) return;
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

    void interpolate(int interpolationDelayMs, int serverTickRate) {
        for (RemotePlayerAvatar avatar : byPlayerId.values()) {
            avatar.advanceInterpolation(interpolationDelayMs, serverTickRate);
        }
    }

    /** Hide remote players who are riding a boat (anchor them to the deck and skip drawing
     *  the rider sprite), mirroring the offline BoatRideComponent render-skip. Called after
     *  snapshots are applied, using the replicated boats' rider components. */
    void applyRidingState(ReplicatedWorldState replicatedWorld) {
        if (replicatedWorld == null) return;
        for (Map.Entry<String, RemotePlayerAvatar> e : byPlayerId.entrySet()) {
            e.getValue().setRidingBoat(replicatedWorld.ridingBoatFor(e.getKey()));
        }
    }

    private RemotePlayerAvatar spawn(String playerId, PlayerStateMessage state) {
        GamePanel panel = ctx.player().panel;
        RemotePlayerAvatar avatar = new RemotePlayerAvatar(
            panel, playerId, state.spriteName(), state.displayName(), state.worldX(), state.worldY());
        ctx.world().placeEntityAuthoritative(avatar);
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

    int size() {
        return byPlayerId.size();
    }

    double meanX() {
        if (byPlayerId.isEmpty()) return 0.0;
        double sum = 0.0;
        for (RemotePlayerAvatar avatar : byPlayerId.values()) {
            sum += avatar.getWorldX();
        }
        return sum / byPlayerId.size();
    }

    double meanY() {
        if (byPlayerId.isEmpty()) return 0.0;
        double sum = 0.0;
        for (RemotePlayerAvatar avatar : byPlayerId.values()) {
            sum += avatar.getWorldY();
        }
        return sum / byPlayerId.size();
    }
}
