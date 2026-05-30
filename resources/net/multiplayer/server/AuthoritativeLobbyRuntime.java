package resources.net.multiplayer.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.authority.AuthorityService;
import resources.net.multiplayer.server.codec.SnapshotCodec;
import resources.net.multiplayer.server.persistence.PersistenceStore;

/**
 * Authoritative single-lobby runtime.
 */
public final class AuthoritativeLobbyRuntime implements LobbyRuntime {

    private static final double DIAGONAL = 0.70710678118;
    private static final long WORLD_CHUNK_KEY = 0L;
    private static final String META_SERVER_TICK = "server_tick";
    private static final String META_WORLD_NEXT_OBJECT_ID = "world_next_object_id";
    private static final String META_WORLD_REVISION = "world_revision";

    private final int maxPlayers;
    private final int protocolVersion;
    private final int snapshotEveryTicks;
    private final int playerFlushEveryTicks;
    private final int worldFlushEveryTicks;
    private final double moveSpeedPerTick;
    private final double maxActionRange;
    private final double interestRadius;
    private final AuthorityService authority;
    private final PersistenceStore persistence;
    private final SnapshotCodec snapshotCodec;
    private final ActionResolver actionResolver;
    private final SnapshotPublisher snapshotPublisher;
    private final ProtocolPayloadCodec payloadCodec = new ProtocolPayloadCodec();
    private final ArrayDeque<ProtocolEnvelope> inbound = new ArrayDeque<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, ArrayDeque<ProtocolEnvelope>> outbound = new HashMap<>();
    private final Map<Long, SimObject> worldObjects = new HashMap<>();
    private final Map<Long, SimObject> tombstones = new HashMap<>();
    private final long tombstoneTtlTicks;

    private long tick;
    private long nextObjectId;
    private long worldRevision;
    private boolean worldDirty;

    public AuthoritativeLobbyRuntime(
            MultiplayerConfig config,
            AuthorityService authority,
            PersistenceStore persistence,
            SnapshotCodec snapshotCodec) {
        this.maxPlayers = Math.max(1, config.maxPlayers());
        this.protocolVersion = config.protocolVersion();
        this.snapshotEveryTicks = Math.max(1, config.serverTickRate() / Math.max(1, config.snapshotRate()));
        this.playerFlushEveryTicks = Math.max(1, config.serverTickRate() * 5);
        this.worldFlushEveryTicks = Math.max(1, config.serverTickRate() * 30);
        this.moveSpeedPerTick = config.serverMoveSpeedPerTick();
        this.maxActionRange = config.serverActionRange();
        this.interestRadius = config.serverInterestRadius();
        this.authority = authority;
        this.persistence = persistence;
        this.snapshotCodec = snapshotCodec;
        this.actionResolver = new ActionResolver(this.maxActionRange);
        this.snapshotPublisher = new SnapshotPublisher(snapshotCodec, this.protocolVersion, this.interestRadius);
        this.tombstoneTtlTicks = Math.max(1L, config.serverTickRate() * 10L);
        this.tick = ServerParse.parseLong(persistence.getMeta(META_SERVER_TICK, "0"), 0L);
        this.nextObjectId = Math.max(1L, ServerParse.parseLong(persistence.getMeta(META_WORLD_NEXT_OBJECT_ID, "1"), 1L));
        this.worldRevision = Math.max(0L, ServerParse.parseLong(persistence.getMeta(META_WORLD_REVISION, "0"), 0L));
        restoreWorldObjects();
    }

    @Override public synchronized void onConnect(String playerId) { /* no-op: join handled in onJoin */ }

    @Override
    public synchronized void onDisconnect(String playerId) {
        if (playerId == null) return;
        Session removed = sessions.remove(playerId);
        if (removed != null) {
            persist(removed);
            presence(playerId, false);
            event(playerId, "disconnect", "");
        }
    }

    @Override
    public synchronized void receive(ProtocolEnvelope envelope) {
        if (envelope != null) inbound.addLast(envelope);
    }

    @Override
    public synchronized void tick() {
        tick++;
        processInbound();
        integratePlayers();
        pruneTombstones();
        if (tick % snapshotEveryTicks == 0L) {
            snapshotPublisher.publishSnapshots(sessions, worldObjects, tombstones, tick, this::send);
        }
        if (tick % playerFlushEveryTicks == 0L) flushPlayers();
        if (tick % worldFlushEveryTicks == 0L) {
            flushWorld();
            persistence.putMeta(META_SERVER_TICK, Long.toString(tick));
            persistence.checkpoint();
        }
    }

    @Override
    public synchronized List<ProtocolEnvelope> drainFor(String playerId) {
        ArrayDeque<ProtocolEnvelope> queue = outbound.get(playerId);
        ArrayList<ProtocolEnvelope> out = new ArrayList<>();
        if (queue == null) return out;
        while (!queue.isEmpty()) out.add(queue.removeFirst());
        return out;
    }

    @Override
    public synchronized void close() {
        flushPlayers();
        flushWorld();
        persistence.putMeta(META_SERVER_TICK, Long.toString(tick));
        persistence.checkpoint();
        persistence.close();
        sessions.clear();
        inbound.clear();
        outbound.clear();
        worldObjects.clear();
        tombstones.clear();
    }

    private void processInbound() {
        while (!inbound.isEmpty()) {
            ProtocolEnvelope envelope = inbound.removeFirst();
            if (envelope.protocolVersion() != protocolVersion) {
                reject(envelope.playerId(), "protocol version mismatch");
                continue;
            }
            ProtocolMessageType type = envelope.messageType();
            if (ProtocolMessageType.JOIN.equals(type)) onJoin(envelope);
            else if (ProtocolMessageType.LEAVE.equals(type)) onDisconnect(envelope.playerId());
            else if (ProtocolMessageType.INPUT_STATE.equals(type)) onInput(envelope);
            else if (ProtocolMessageType.ACTION.equals(type)) onAction(envelope);
            else if (ProtocolMessageType.PING.equals(type)) ack(envelope.playerId(), envelope.sequence());
        }
    }

    private void onJoin(ProtocolEnvelope envelope) {
        String playerId = envelope.playerId();
        if (playerId == null || playerId.isBlank()) { reject(playerId, "missing player id"); return; }
        if (!sessions.containsKey(playerId) && sessions.size() >= maxPlayers) { reject(playerId, "server full"); return; }
        Session session = sessions.get(playerId);
        boolean freshSession = false;
        if (session == null) {
            session = newSession(playerId);
            sessions.put(playerId, session);
            freshSession = true;
        }
        if (freshSession) {
            ProtocolPayloads.JoinRequest join = payloadCodec.decodeJoinRequest(envelope.payload());
            if (join.hasSpawn) {
                session.x = join.spawnX;
                session.y = join.spawnY;
                session.lastChangedTick = tick;
            }
        }
        session.baselineSent = false;
        session.lastSentTick = 0L;
        session.lastChangedTick = tick;
        send(playerId, new ProtocolEnvelope(protocolVersion, playerId, 0L, session.lastAcceptedSeq, tick, ProtocolMessageType.WELCOME, new byte[0]));
        presence(playerId, true);
        event(playerId, "join", "");
    }

    private void onInput(ProtocolEnvelope envelope) {
        Session s = sessions.get(envelope.playerId());
        if (s == null) return;
        if (!authority.acceptsSequence(envelope.sequence(), s.lastAcceptedSeq)) return;
        ProtocolPayloads.InputState input = payloadCodec.decodeInputState(envelope.payload());
        boolean changed = s.up != input.up || s.left != input.left || s.down != input.down || s.right != input.right;
        s.lastAcceptedSeq = envelope.sequence();
        s.up = input.up; s.left = input.left; s.down = input.down; s.right = input.right;
        if (changed) s.lastChangedTick = tick;
        ack(s.playerId, s.lastAcceptedSeq);
    }

    private void onAction(ProtocolEnvelope envelope) {
        Session s = sessions.get(envelope.playerId());
        if (s == null) return;
        if (!authority.acceptsSequence(envelope.sequence(), s.lastAcceptedSeq)) return;
        ProtocolPayloads.ActionRequest action = payloadCodec.decodeAction(envelope.payload());
        if (action.action == null || !authority.canPerformAction(s.playerId, action.action, tick)) return;
        if (action.hasTarget && !authority.withinRange(s.x, s.y, action.targetX, action.targetY, maxActionRange)) return;
        ActionResolver.Mutation mutation =
            new ActionResolver.Mutation(nextObjectId, worldRevision, worldDirty, tick, this::event);
        boolean applied = actionResolver.applyAction(s, action, worldObjects, tombstones, mutation);
        nextObjectId = mutation.nextObjectId;
        worldRevision = mutation.worldRevision;
        worldDirty = mutation.worldDirty;
        if (!applied) return;
        s.lastAcceptedSeq = envelope.sequence();
        s.lastAction = action.action;
        s.lastChangedTick = tick;
        ack(s.playerId, s.lastAcceptedSeq);
    }

    private void integratePlayers() {
        for (Session s : sessions.values()) {
            double dx = 0.0; double dy = 0.0;
            if (s.up) dy -= moveSpeedPerTick;
            if (s.down) dy += moveSpeedPerTick;
            if (s.left) dx -= moveSpeedPerTick;
            if (s.right) dx += moveSpeedPerTick;
            if (dx != 0.0 && dy != 0.0) { dx *= DIAGONAL; dy *= DIAGONAL; }
            if (!authority.canMove(dx, dy, moveSpeedPerTick)) continue;
            boolean moved = dx != 0.0 || dy != 0.0;
            boolean velocityChanged = s.vx != dx || s.vy != dy;
            s.vx = dx; s.vy = dy;
            s.x += dx; s.y += dy;
            if (moved || velocityChanged) s.lastChangedTick = tick;
        }
    }

    private Session newSession(String playerId) {
        PersistenceStore.PersistedPlayer persisted = persistence.loadPlayer(playerId)
            .orElse(new PersistenceStore.PersistedPlayer(playerId, 0.0, 0.0, 0.0, 0.0, 0L));
        return new Session(persisted);
    }

    private void presence(String playerId, boolean joined) {
        byte[] payload = payloadCodec.encodePresence(new ProtocolPayloads.Presence(playerId, joined));
        broadcast(new ProtocolEnvelope(protocolVersion, playerId, 0L, 0L, tick, ProtocolMessageType.PLAYER_JOIN_LEAVE, payload));
    }

    private void reject(String playerId, String reason) {
        byte[] payload = payloadCodec.encodeReject(new ProtocolPayloads.Reject(reason));
        send(playerId, new ProtocolEnvelope(protocolVersion, playerId, 0L, 0L, tick, ProtocolMessageType.REJECT, payload));
    }

    private void ack(String playerId, long seq) {
        byte[] payload = payloadCodec.encodeAck(new ProtocolPayloads.Ack(seq));
        send(playerId, new ProtocolEnvelope(protocolVersion, playerId, 0L, seq, tick, ProtocolMessageType.ACK, payload));
    }

    private void send(String playerId, ProtocolEnvelope envelope) {
        if (playerId == null || playerId.isBlank() || envelope == null) return;
        outbound.computeIfAbsent(playerId, ignored -> new ArrayDeque<>()).addLast(envelope);
    }

    private void broadcast(ProtocolEnvelope envelope) {
        for (String playerId : sessions.keySet()) send(playerId, envelope);
    }

    private void persist(Session s) {
        persistence.savePlayer(new PersistenceStore.PersistedPlayer(
            s.playerId, s.x, s.y, s.vx, s.vy, s.lastAcceptedSeq));
    }

    private void flushPlayers() {
        for (Session s : sessions.values()) persist(s);
    }

    private void flushWorld() {
        if (!worldDirty && worldObjects.isEmpty() && nextObjectId <= 1L && worldRevision <= 0L) return;
        ArrayList<ProtocolPayloads.WorldObjectState> objects = new ArrayList<>(worldObjects.size());
        for (SimObject object : worldObjects.values()) {
            if (!object.removed) objects.add(object.toPayloadState());
        }
        byte[] bytes = payloadCodec.encodeSnapshot(
            new ProtocolPayloads.Snapshot(true, 0L, new ArrayList<>(), objects));
        persistence.saveWorldChunk(WORLD_CHUNK_KEY, bytes);
        persistence.putMeta(META_WORLD_NEXT_OBJECT_ID, Long.toString(Math.max(1L, nextObjectId)));
        persistence.putMeta(META_WORLD_REVISION, Long.toString(Math.max(0L, worldRevision)));
        worldDirty = false;
    }

    private void restoreWorldObjects() {
        byte[] bytes = persistence.loadWorldChunk(WORLD_CHUNK_KEY).orElse(null);
        if (bytes == null || bytes.length == 0) return;
        ProtocolPayloads.Snapshot snapshot = payloadCodec.decodeSnapshot(bytes);
        long maxId = 0L;
        long maxRevision = worldRevision;
        for (ProtocolPayloads.WorldObjectState state : snapshot.worldObjects) {
            if (state == null || state.removed) continue;
            SimObject object = new SimObject(
                state.objectId, state.objectType, state.worldX, state.worldY, false, state.revision, tick);
            worldObjects.put(object.objectId, object);
            maxId = Math.max(maxId, object.objectId);
            maxRevision = Math.max(maxRevision, object.revision);
        }
        if (nextObjectId <= maxId) nextObjectId = maxId + 1L;
        worldRevision = Math.max(worldRevision, maxRevision);
    }

    private void pruneTombstones() {
        ArrayList<Long> expired = new ArrayList<>();
        for (Map.Entry<Long, SimObject> entry : tombstones.entrySet()) {
            SimObject tombstone = entry.getValue();
            if ((tick - tombstone.lastChangedTick) > tombstoneTtlTicks) {
                expired.add(entry.getKey());
            }
        }
        for (Long key : expired) tombstones.remove(key);
    }

    private void event(String playerId, String type, String payload) {
        persistence.appendSessionEvent(tick, playerId, type, payload);
    }
}
