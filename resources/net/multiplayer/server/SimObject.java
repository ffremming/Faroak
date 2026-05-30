package resources.net.multiplayer.server;

import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Authoritative world object (or tombstone when {@code removed}). Mutated
 * exclusively under the owning {@link AuthoritativeLobbyRuntime}'s lock;
 * carries no synchronization of its own.
 */
final class SimObject {
    final long objectId;
    final String objectType;
    final double worldX;
    final double worldY;
    final boolean removed;
    long revision;
    long lastChangedTick;

    SimObject(
            long objectId,
            String objectType,
            double worldX,
            double worldY,
            boolean removed,
            long revision,
            long lastChangedTick) {
        this.objectId = Math.max(0L, objectId);
        this.objectType = (objectType == null) ? "" : objectType;
        this.worldX = worldX;
        this.worldY = worldY;
        this.removed = removed;
        this.revision = Math.max(0L, revision);
        this.lastChangedTick = Math.max(0L, lastChangedTick);
    }

    boolean changedSince(long sentTick) {
        return sentTick <= 0L || lastChangedTick > sentTick;
    }

    ProtocolPayloads.WorldObjectState toPayloadState() {
        return new ProtocolPayloads.WorldObjectState(
            objectId, objectType, worldX, worldY, removed, revision);
    }
}
