package resources.net.multiplayer.server;

import resources.net.multiplayer.MultiplayerAction;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.persistence.PersistenceStore;

/**
 * Per-player server-side session state. Mutated exclusively under the owning
 * {@link AuthoritativeLobbyRuntime}'s lock; carries no synchronization of its own.
 */
final class Session {
    final String playerId;
    double x; double y; double vx; double vy;
    boolean up; boolean left; boolean down; boolean right;
    long lastAcceptedSeq;
    long lastSentTick;
    long lastChangedTick;
    boolean baselineSent;
    // Set once the client has reported its own collision-resolved position
    // (client-authoritative movement). While true the server NEVER re-simulates
    // movement from keys for this player — it only adopts the client's reported
    // position. Sticky for the session so held keys don't cause the server to keep
    // walking after a position update.
    boolean clientAuthoritative;
    MultiplayerAction lastAction;
    int health = 20;
    int maxHealth = 20;
    final String displayName;
    String spriteName = "red";
    // Death / respawn state (see Feature C). Alive players integrate input; dead
    // ones freeze until respawnAtTick or a respawn request.
    boolean alive = true;
    long respawnAtTick = 0L;
    boolean respawnRequested;

    Session(PersistenceStore.PersistedPlayer persisted) {
        this.playerId = persisted.playerId;
        this.x = persisted.worldX; this.y = persisted.worldY;
        this.vx = persisted.velocityX; this.vy = persisted.velocityY;
        this.lastAcceptedSeq = persisted.lastSequence;
        this.lastChangedTick = 0L;
        this.displayName = deriveDisplayName(persisted.playerId);
    }

    boolean changedSince(long sentTick) {
        return sentTick <= 0L || lastChangedTick > sentTick;
    }

    /** Facing index from current velocity (0=up,1=right,2=down,3=left); x dominates y,
     *  matching Moveable.updateDirectionIndex precedence. Keeps last facing while idle. */
    private int facing() {
        if (vx > 0.0) return 1;
        if (vx < 0.0) return 3;
        if (vy < 0.0) return 0;
        if (vy > 0.0) return 2;
        return lastFacing;
    }

    private int lastFacing = 2;

    ProtocolPayloads.PlayerState toPayloadState() {
        boolean moving = (vx * vx + vy * vy) > 1.0e-4;
        int facing = facing();
        if (moving) lastFacing = facing;
        return new ProtocolPayloads.PlayerState(
            playerId, x, y, vx, vy, lastAcceptedSeq, health, maxHealth,
            facing, moving, spriteName, displayName, alive);
    }

    /** LobbyScreen builds playerIds as "Name-<6hexsuffix>"; recover the friendly name. */
    static String deriveDisplayName(String playerId) {
        if (playerId == null || playerId.isBlank()) return "Player";
        int dash = playerId.lastIndexOf('-');
        String base = (dash > 0) ? playerId.substring(0, dash) : playerId;
        base = base.replace('_', ' ').trim();
        return base.isEmpty() ? playerId : base;
    }
}
