package resources.net.multiplayer.server;

import java.util.ArrayList;
import java.util.Map;

import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.SnapshotCodec;

/**
 * Builds and dispatches per-recipient baseline/delta snapshots. Pure logic
 * helper: holds only immutable configuration, performs no synchronization, and
 * is invoked exclusively from inside the {@link AuthoritativeLobbyRuntime}'s
 * lock. The collections it iterates and the {@link Sender} it writes through are
 * supplied per call.
 */
final class SnapshotPublisher {

    /** Outbound sink; backed by the runtime's per-player queues. */
    interface Sender {
        void send(String playerId, ProtocolEnvelope envelope);
    }

    private final SnapshotCodec snapshotCodec;
    private final int protocolVersion;
    private final double interestRadius;

    SnapshotPublisher(SnapshotCodec snapshotCodec, int protocolVersion, double interestRadius) {
        this.snapshotCodec = snapshotCodec;
        this.protocolVersion = protocolVersion;
        this.interestRadius = interestRadius;
    }

    void publishSnapshots(
            Map<String, Session> sessions,
            Map<Long, SimObject> worldObjects,
            Map<Long, SimObject> tombstones,
            long tick,
            Sender sender) {
        for (Session recipient : sessions.values()) {
            boolean baseline = !recipient.baselineSent;
            ArrayList<ProtocolPayloads.PlayerState> states = new ArrayList<>();
            for (Session candidate : sessions.values()) {
                if (!isInterested(recipient, candidate)) continue;
                if (!baseline && !candidate.changedSince(recipient.lastSentTick)) continue;
                states.add(candidate.toPayloadState());
            }
            ArrayList<ProtocolPayloads.WorldObjectState> objects = new ArrayList<>();
            if (baseline) {
                for (SimObject object : worldObjects.values()) {
                    objects.add(object.toPayloadState());
                }
            } else {
                for (SimObject object : worldObjects.values()) {
                    if (object.changedSince(recipient.lastSentTick)) {
                        objects.add(object.toPayloadState());
                    }
                }
                for (SimObject tombstone : tombstones.values()) {
                    if (tombstone.changedSince(recipient.lastSentTick)) {
                        objects.add(tombstone.toPayloadState());
                    }
                }
            }
            if (!baseline && states.isEmpty() && objects.isEmpty()) continue;
            ProtocolPayloads.Snapshot snap = new ProtocolPayloads.Snapshot(
                baseline, recipient.lastAcceptedSeq, states, objects);
            byte[] payload = snapshotCodec.encode(snap);
            ProtocolMessageType type = baseline ? ProtocolMessageType.BASELINE_SNAPSHOT : ProtocolMessageType.DELTA_SNAPSHOT;
            sender.send(recipient.playerId, new ProtocolEnvelope(protocolVersion, recipient.playerId, 0L, recipient.lastAcceptedSeq, tick, type, payload));
            recipient.baselineSent = true;
            recipient.lastSentTick = tick;
        }
    }

    private boolean isInterested(Session recipient, Session candidate) {
        if (recipient.playerId.equals(candidate.playerId)) return true;
        double dx = candidate.x - recipient.x;
        double dy = candidate.y - recipient.y;
        return ((dx * dx) + (dy * dy)) <= (interestRadius * interestRadius);
    }
}
