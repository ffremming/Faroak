package resources.net.multiplayer.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import resources.net.multiplayer.MultiplayerAction;
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
    private static final double MIN_OBJECT_GAP = 32.0;

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
        this.tombstoneTtlTicks = Math.max(1L, config.serverTickRate() * 10L);
        this.tick = parseLong(persistence.getMeta(META_SERVER_TICK, "0"), 0L);
        this.nextObjectId = Math.max(1L, parseLong(persistence.getMeta(META_WORLD_NEXT_OBJECT_ID, "1"), 1L));
        this.worldRevision = Math.max(0L, parseLong(persistence.getMeta(META_WORLD_REVISION, "0"), 0L));
        restoreWorldObjects();
    }

    @Override public synchronized void onConnect(String playerId) {}

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
        if (tick % snapshotEveryTicks == 0L) publishSnapshots();
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
        if (!applyAction(s, action)) return;
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

    private void publishSnapshots() {
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
            send(recipient.playerId, new ProtocolEnvelope(protocolVersion, recipient.playerId, 0L, recipient.lastAcceptedSeq, tick, type, payload));
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

    private boolean applyAction(Session session, ProtocolPayloads.ActionRequest action) {
        if (action == null || action.action == null) return false;
        if (MultiplayerAction.PLACE.equals(action.action)) {
            return applyPlaceAction(session, action);
        }
        if (MultiplayerAction.ATTACK.equals(action.action)) {
            return applyAttackAction(session, action);
        }
        if (MultiplayerAction.INTERACT.equals(action.action)) {
            return applyInteractAction(session, action);
        }
        return false;
    }

    private boolean applyPlaceAction(Session session, ProtocolPayloads.ActionRequest action) {
        if (!action.hasTarget) return false;
        String objectType = sanitizeObjectType(action.argument);
        if (objectType.isBlank()) return false;
        double x = Math.rint(action.targetX);
        double y = Math.rint(action.targetY);
        if (placementBlocked(x, y)) return false;

        long objectId = nextObjectId++;
        long revision = ++worldRevision;
        SimObject object = new SimObject(objectId, objectType, x, y, false, revision, tick);
        worldObjects.put(objectId, object);
        tombstones.remove(objectId);
        worldDirty = true;
        event(session.playerId, "action", "place:" + objectType + ":" + (int) x + "," + (int) y);
        return true;
    }

    private boolean applyAttackAction(Session session, ProtocolPayloads.ActionRequest action) {
        double targetX = action.hasTarget ? action.targetX : session.x;
        double targetY = action.hasTarget ? action.targetY : session.y;
        SimObject nearest = nearestObject(targetX, targetY, maxActionRange);
        if (nearest == null) {
            event(session.playerId, "action", "attack:none");
            return true;
        }
        worldObjects.remove(nearest.objectId);
        SimObject tombstone = new SimObject(
            nearest.objectId, nearest.objectType, nearest.worldX, nearest.worldY, true, ++worldRevision, tick);
        tombstones.put(tombstone.objectId, tombstone);
        worldDirty = true;
        event(session.playerId, "action", "attack:remove:" + nearest.objectId);
        return true;
    }

    private boolean applyInteractAction(Session session, ProtocolPayloads.ActionRequest action) {
        double targetX = action.hasTarget ? action.targetX : session.x;
        double targetY = action.hasTarget ? action.targetY : session.y;
        SimObject nearest = nearestObject(targetX, targetY, maxActionRange);
        if (nearest == null) {
            event(session.playerId, "action", "interact:none");
            return true;
        }
        nearest.revision = ++worldRevision;
        nearest.lastChangedTick = tick;
        worldDirty = true;
        event(session.playerId, "action", "interact:" + nearest.objectId);
        return true;
    }

    private SimObject nearestObject(double x, double y, double maxRange) {
        SimObject nearest = null;
        double bestDist2 = maxRange * maxRange;
        for (SimObject object : worldObjects.values()) {
            double dx = object.worldX - x;
            double dy = object.worldY - y;
            double dist2 = (dx * dx) + (dy * dy);
            if (dist2 > bestDist2) continue;
            bestDist2 = dist2;
            nearest = object;
        }
        return nearest;
    }

    private boolean placementBlocked(double x, double y) {
        double minDist2 = MIN_OBJECT_GAP * MIN_OBJECT_GAP;
        for (SimObject object : worldObjects.values()) {
            double dx = object.worldX - x;
            double dy = object.worldY - y;
            if (((dx * dx) + (dy * dy)) <= minDist2) return true;
        }
        return false;
    }

    private String sanitizeObjectType(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase();
        if (value.isBlank() || "empty".equals(value)) return "";
        if (value.length() > 64) value = value.substring(0, 64);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-';
            if (!ok) return "";
        }
        return value;
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return Long.parseLong(value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private void event(String playerId, String type, String payload) {
        persistence.appendSessionEvent(tick, playerId, type, payload);
    }

    private static final class Session {
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

    private static final class SimObject {
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
}
