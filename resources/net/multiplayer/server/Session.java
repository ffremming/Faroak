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
    MultiplayerAction lastAction;

    Session(PersistenceStore.PersistedPlayer persisted) {
        this.playerId = persisted.playerId;
        this.x = persisted.worldX; this.y = persisted.worldY;
        this.vx = persisted.velocityX; this.vy = persisted.velocityY;
        this.lastAcceptedSeq = persisted.lastSequence;
        this.lastChangedTick = 0L;
    }

    boolean changedSince(long sentTick) {
        return sentTick <= 0L || lastChangedTick > sentTick;
    }

    ProtocolPayloads.PlayerState toPayloadState() {
        return new ProtocolPayloads.PlayerState(playerId, x, y, vx, vy, lastAcceptedSeq);
    }
}
