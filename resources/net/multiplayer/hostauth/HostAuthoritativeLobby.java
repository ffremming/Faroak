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
 *       only drains inbound control messages and emits welcomes/presence. It does
 *       NOT read or mutate the engine: interaction commands AND movement input are
 *       buffered and replayed on the frame thread by {@link #applyInteractions}, so it
 *       cannot race with simulation.</li>
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
    // Interaction commands are buffered on the server thread and applied on the host
    // frame thread (in applyInteractions) so engine mutation stays single-threaded.
    private final ArrayDeque<ProtocolEnvelope> pendingCommands = new ArrayDeque<>();
    // Input-state messages are likewise buffered on the server thread and applied on the
    // host frame thread (in applyInteractions). RemoteInputApplier.apply() reads the live
    // world via solidCollision() to validate remote positions; doing that on the server
    // thread would race world.simulate() on the frame thread (the world has no internal
    // synchronization). Deferring keeps every engine read single-threaded.
    private final ArrayDeque<ProtocolEnvelope> pendingInputs = new ArrayDeque<>();
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
        this.remotes = new RemoteInputApplier(ctx);
        this.builder = new EngineSnapshotBuilder(ctx, ids);
        this.protocolVersion = config.protocolVersion();
        this.hostPlayerId = config.playerId();
        this.builder.withRiderResolution(this.remotes, this.hostPlayerId);
    }

    /** Test-only: equipped item reported for a guest. */
    public synchronized String debugEquipped(String playerId) { return remotes.debugEquipped(playerId); }

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
                // Defer to the frame thread: applying input reads the live world
                // (solidCollision) which must not race world.simulate().
                if (joined.contains(playerId)) pendingInputs.addLast(envelope);
            } else if (ProtocolMessageType.COMMAND.equals(type)) {
                if (joined.contains(playerId)) pendingCommands.addLast(envelope);
            }
        }
    }

    /**
     * Host-frame tick: drain buffered interaction commands and run each through the real
     * engine on the acting guest's behalf. MUST run on the host frame thread (it mutates
     * the world via the real ClickRouter). Sends a COMMAND_RESULT per command.
     */
    public synchronized void applyInteractions() {
        // Apply buffered movement input first (on the frame thread) so any subsequent
        // interaction this frame measures reach from the avatar's updated position, and
        // so the solidCollision() world read never races world.simulate().
        while (!pendingInputs.isEmpty()) {
            ProtocolEnvelope envelope = pendingInputs.removeFirst();
            String playerId = envelope.playerId();
            if (!joined.contains(playerId)) continue;
            remotes.apply(playerId, payloadCodec.decodeInputState(envelope.payload()), envelope.sequence());
        }
        while (!pendingCommands.isEmpty()) {
            ProtocolEnvelope envelope = pendingCommands.removeFirst();
            String playerId = envelope.playerId();
            ProtocolPayloads.CommandRequest command = payloadCodec.decodeCommand(envelope.payload());
            boolean accepted = false;
            if (command != null && command.hasTarget) {
                if (ProtocolPayloads.CommandRequest.FIRE_BROADSIDE.equals(command.commandType)) {
                    resources.domain.player.Playable host = playerId.equals(hostPlayerId) ? ctx.player() : null;
                    accepted = remotes.applyBroadside(playerId, host);
                } else if (RemoteInputApplier.isAttackCommand(command.commandType)) {
                    // Combat (incl. PvP) resolves through the real CombatService. The HOST is
                    // the engine and has no guest avatar, so it attacks via its own player.
                    if (playerId.equals(hostPlayerId)) {
                        accepted = remotes.applyHostAttack(ctx.player(), command.commandType, command.targetX, command.targetY);
                    } else {
                        accepted = remotes.applyAttack(playerId, command.commandType, command.targetX, command.targetY);
                    }
                } else {
                    accepted = remotes.applyInteraction(playerId, command.targetX, command.targetY);
                }
            }
            enqueue(playerId, new ProtocolEnvelope(
                protocolVersion, playerId, 0L, accepted ? envelope.sequence() : 0L, serverTick,
                ProtocolMessageType.COMMAND_RESULT,
                payloadCodec.encodeCommandResult(new ProtocolPayloads.CommandResult(
                    envelope.sequence(), accepted, accepted ? "" : "no interaction"))));
        }
    }

    /**
     * Host-frame tick: read the live engine and queue a baseline or delta snapshot
     * for each joined player. MUST run on the host frame thread.
     */
    public synchronized void produceSnapshots() {
        if (joined.isEmpty()) return;
        // Each recipient sees the OTHER remote players (its own client renders itself).
        java.util.List<ProtocolPayloads.PlayerState> allRemotes = new ArrayList<>();
        for (RemoteInputApplier.RemoteAvatar a : remotes.avatars()) allRemotes.add(a.toPayload());

        // Player inventories: the host's own, plus each guest's headless-actor inventory,
        // each keyed "player:<id>" so a client maps it back to its own bag.
        java.util.List<ProtocolPayloads.InventoryStatePayload> playerInventories = new ArrayList<>();
        if (ctx.player() != null && ctx.player().getInventory() != null) {
            playerInventories.add(builder.inventoryPayload(
                ctx.player().getInventory(), builder.inventoryId(ctx.player().getInventory()),
                0L, "player:" + hostPlayerId.toLowerCase()));
        }
        for (String joinedId : joined) {
            if (joinedId.equals(hostPlayerId)) continue;
            resources.domain.inventory.Inventory inv = remotes.actorInventory(joinedId);
            if (inv != null) {
                playerInventories.add(builder.inventoryPayload(
                    inv, builder.inventoryId(inv), 0L, "player:" + joinedId.toLowerCase()));
            }
        }

        // Cursor inventories: the single item each player holds on the mouse (tempInHand),
        // keyed "cursor:<id>" so a client maps it back via setTempInHand(). The cursor id is
        // a stable token distinct from the main bag id so the two never collide on the client.
        if (ctx.player() != null) {
            // intern() the key so the IdentityHashMap-backed id registry hands out a stable
            // cursor inventory id every frame instead of churning (and leaking) one per frame.
            String hostCursorKey = ("cursor:" + hostPlayerId.toLowerCase()).intern();
            playerInventories.add(builder.cursorPayload(
                ctx.player().getTempInHand(), builder.inventoryId(hostCursorKey), hostCursorKey));
        }
        for (String joinedId : joined) {
            if (joinedId.equals(hostPlayerId)) continue;
            if (remotes.actorInventory(joinedId) == null) continue;
            String cursorKey = ("cursor:" + joinedId.toLowerCase()).intern();
            playerInventories.add(builder.cursorPayload(
                remotes.actorTempInHand(joinedId), builder.inventoryId(cursorKey), cursorKey));
        }

        // Build the shared world delta AT MOST ONCE per frame so the delta-signature cache
        // is advanced a single time — otherwise the first recipient's delta would consume
        // the change and later recipients would miss it. Baselines are per joining player.
        ProtocolPayloads.Snapshot sharedDelta = null;
        for (String playerId : joined) {
            java.util.List<ProtocolPayloads.PlayerState> peers = new ArrayList<>();
            for (ProtocolPayloads.PlayerState p : allRemotes) {
                if (!p.playerId.equals(playerId)) peers.add(p);
            }
            boolean wantsBaseline = needsBaseline.remove(playerId);
            ProtocolPayloads.Snapshot worldPart;
            ProtocolMessageType type;
            if (wantsBaseline) {
                worldPart = builder.buildBaseline(0L, peers, playerInventories);
                type = ProtocolMessageType.BASELINE_SNAPSHOT;
            } else {
                // Build the shared world delta once; carry player inventories on it so each
                // client keeps its bag synced. The builder delta-filters inventories (by id +
                // contents signature), so an unchanged bag is omitted from deltas.
                if (sharedDelta == null) sharedDelta = builder.buildDelta(0L, null, playerInventories);
                worldPart = withPlayers(sharedDelta, peers);
                type = ProtocolMessageType.DELTA_SNAPSHOT;
            }
            byte[] payload = snapshotCodec.encode(worldPart);
            enqueue(playerId, new ProtocolEnvelope(
                protocolVersion, playerId, 0L, 0L, serverTick, type, payload));
        }
    }

    /** Reuse a built world delta but swap in this recipient's peer player list. */
    private ProtocolPayloads.Snapshot withPlayers(
            ProtocolPayloads.Snapshot base, java.util.List<ProtocolPayloads.PlayerState> players) {
        return new ProtocolPayloads.Snapshot(
            base.baseline, base.acknowledgedSequence, players, base.worldObjects,
            base.entities, base.inventories, base.tileMutations).withWorldTime(base.worldTimeTicks);
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
        pendingCommands.clear();
        pendingInputs.clear();
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
