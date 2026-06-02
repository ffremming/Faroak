package resources.net.multiplayer.hostauth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import resources.app.GameContext;
import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.codec.SnapshotCodec;

/**
 * Authoritative lobby backed by the HOST client's real game engine.
 *
 * <p>Instead of the legacy SimObject simulation, this lobby serializes the host's
 * live {@code WorkingMemory} world (via {@link EngineSnapshotBuilder}) into the
 * existing snapshot protocol, so online state matches offline gameplay exactly.
 *
 * <h2>Threading</h2>
 * The real engine runs on the host's frame thread ({@code GamePanel.update ->
 * world.simulate()}). Reading it concurrently from the server thread would race.
 * So responsibilities are split:
 * <ul>
 *   <li>{@link #receive} / {@link #drainFor} only touch thread-safe queues — safe
 *       from any thread (the transport).</li>
 *   <li>{@link #produceSnapshots} reads the engine and MUST be called on the host
 *       frame thread (wired in Phase 3.3 from {@code MultiplayerRuntime.update}).</li>
 *   <li>{@link #tick} (the {@link LobbyRuntime} contract called by the server loop)
 *       only drains inbound control messages and emits welcomes/presence — it does
 *       NOT read the engine, so it cannot race with simulation.</li>
 * </ul>
 */
public final class HostAuthoritativeLobby implements LobbyRuntime {

    private final GameContext ctx;
    private final MultiplayerConfig config;
    private final SnapshotCodec snapshotCodec;
    private final EngineSnapshotBuilder builder;
    private final RemoteInputApplier remotes;
    private final ProtocolPayloadCodec payloadCodec = new ProtocolPayloadCodec();
    private final int protocolVersion;
    private final String hostPlayerId;

    private final ArrayDeque<ProtocolEnvelope> inbound = new ArrayDeque<>();
    private final Map<String, ArrayDeque<ProtocolEnvelope>> outbound = new HashMap<>();
    private final Set<String> joined = new LinkedHashSet<>();
    private final Set<String> needsBaseline = new LinkedHashSet<>();

    private long serverTick;

    public HostAuthoritativeLobby(
            GameContext ctx,
            MultiplayerConfig config,
            SnapshotCodec snapshotCodec,
            StableEntityIds ids) {
        this.ctx = ctx;
        this.config = config;
        this.snapshotCodec = snapshotCodec;
        this.builder = new EngineSnapshotBuilder(ctx, ids);
        this.remotes = new RemoteInputApplier(ctx);
        this.protocolVersion = config.protocolVersion();
        this.hostPlayerId = config.playerId();
    }

    @Override public synchronized void onConnect(String playerId) { /* join handled on JOIN message */ }

    @Override
    public synchronized void onDisconnect(String playerId) {
        if (playerId == null) return;
        if (joined.remove(playerId)) {
            needsBaseline.remove(playerId);
            outbound.remove(playerId);
            remotes.leave(playerId);
            broadcastPresence(playerId, false);
        }
    }

    @Override
    public synchronized void receive(ProtocolEnvelope envelope) {
        if (envelope != null) inbound.addLast(envelope);
    }

    /**
     * Server-thread tick: drain control messages and emit welcomes/presence. Does
     * NOT read the engine (snapshots are produced on the host frame thread).
     */
    @Override
    public synchronized void tick() {
        serverTick++;
        while (!inbound.isEmpty()) {
            ProtocolEnvelope envelope = inbound.removeFirst();
            if (envelope.protocolVersion() != protocolVersion) continue;
            ProtocolMessageType type = envelope.messageType();
            String playerId = envelope.playerId();
            if (ProtocolMessageType.JOIN.equals(type)) {
                ProtocolPayloads.JoinRequest join = payloadCodec.decodeJoinRequest(envelope.payload());
                onJoin(playerId, join);
            } else if (ProtocolMessageType.LEAVE.equals(type)) {
                onDisconnect(playerId);
            } else if (ProtocolMessageType.INPUT_STATE.equals(type)) {
                if (joined.contains(playerId)) {
                    remotes.apply(playerId, payloadCodec.decodeInputState(envelope.payload()), envelope.sequence());
                }
            }
            // ACTION / COMMAND handled in Phase 5.
        }
    }

    /**
     * Host-frame tick: read the live engine and queue a baseline or delta snapshot
     * for each joined player. MUST run on the host frame thread.
     */
    public synchronized void produceSnapshots() {
        if (joined.isEmpty()) return;
        // Each recipient should see the OTHER remote players (not itself — the client
        // renders its own local player). So players are filtered per recipient.
        java.util.List<ProtocolPayloads.PlayerState> allRemotes = new ArrayList<>();
        for (RemoteInputApplier.RemoteAvatar a : remotes.avatars()) allRemotes.add(a.toPayload());
        for (String playerId : joined) {
            java.util.List<ProtocolPayloads.PlayerState> peers = new ArrayList<>();
            for (ProtocolPayloads.PlayerState p : allRemotes) {
                if (!p.playerId.equals(playerId)) peers.add(p);
            }
            boolean wantsBaseline = needsBaseline.remove(playerId);
            ProtocolPayloads.Snapshot snapshot = wantsBaseline
                ? builder.buildBaseline(0L, peers)
                : builder.buildDelta(0L, peers);
            ProtocolMessageType type = wantsBaseline
                ? ProtocolMessageType.BASELINE_SNAPSHOT
                : ProtocolMessageType.DELTA_SNAPSHOT;
            byte[] payload = snapshotCodec.encode(snapshot);
            enqueue(playerId, new ProtocolEnvelope(
                protocolVersion, playerId, 0L, 0L, serverTick, type, payload));
        }
    }

    @Override
    public synchronized List<ProtocolEnvelope> drainFor(String playerId) {
        ArrayList<ProtocolEnvelope> out = new ArrayList<>();
        ArrayDeque<ProtocolEnvelope> queue = outbound.get(playerId);
        if (queue == null) return out;
        while (!queue.isEmpty()) out.add(queue.removeFirst());
        return out;
    }

    @Override
    public synchronized void close() {
        inbound.clear();
        outbound.clear();
        joined.clear();
        needsBaseline.clear();
    }

    private void onJoin(String playerId, ProtocolPayloads.JoinRequest join) {
        if (playerId == null || playerId.isBlank()) return;
        boolean fresh = joined.add(playerId);
        needsBaseline.add(playerId);
        // The host itself does not need a remote avatar (it IS the engine); only guests do.
        if (!playerId.equals(hostPlayerId)) {
            double sx = (join != null && join.hasSpawn) ? join.spawnX : 0.0;
            double sy = (join != null && join.hasSpawn) ? join.spawnY : 0.0;
            remotes.join(playerId, sx, sy);
        }
        // WELCOME with empty payload mirrors the legacy lobby's accept handshake.
        enqueue(playerId, new ProtocolEnvelope(
            protocolVersion, playerId, 0L, 0L, serverTick, ProtocolMessageType.WELCOME, new byte[0]));
        if (fresh) broadcastPresence(playerId, true);
    }

    private void broadcastPresence(String playerId, boolean joinedFlag) {
        // Presence payload encoding is added with the codec wiring in Phase 4; for the
        // skeleton we only notify already-connected peers structurally. The host itself
        // does not need a presence envelope.
    }

    private void enqueue(String playerId, ProtocolEnvelope envelope) {
        if (playerId == null || playerId.isBlank() || envelope == null) return;
        outbound.computeIfAbsent(playerId, ignored -> new ArrayDeque<>()).addLast(envelope);
    }
}
