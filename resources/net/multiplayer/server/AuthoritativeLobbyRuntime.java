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
    private static final double MAX_MOVE_SUBSTEP = 4.0;
    private static final long WORLD_CHUNK_KEY = 0L;
    private static final String META_SERVER_TICK = "server_tick";
    private static final String META_WORLD_NEXT_OBJECT_ID = "world_next_object_id";
    private static final String META_WORLD_REVISION = "world_revision";
    private static final String META_WORLD_CHUNK_KEYS = "world_chunk_keys";
    private static final String META_WORLD_POPULATED = "world_populated";

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
    private final ServerTerrainRules terrainRules;
    private final AuthoritativeGameHost gameHost;
    private final ServerMobSpawner mobSpawner;
    private final SnapshotPublisher snapshotPublisher;
    private final ProtocolPayloadCodec payloadCodec = new ProtocolPayloadCodec();
    private final ArrayDeque<ProtocolEnvelope> inbound = new ArrayDeque<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, ArrayDeque<ProtocolEnvelope>> outbound = new HashMap<>();
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
        this.terrainRules = new ServerTerrainRules();
        this.gameHost = new AuthoritativeGameHost(this.terrainRules, this.maxActionRange);
        long worldSeed = ServerParse.parseLong(System.getProperty("game.world.seed", "424242"), 424242L);
        int mobCap = (int) ServerParse.parseLong(System.getProperty("game.multiplayer.mobCap", "12"), 12L);
        this.mobSpawner = new ServerMobSpawner(
            this.terrainRules, mobCap, 384.0, 768.0, 1600.0,
            Math.max(1L, config.serverTickRate() / 2L), worldSeed);
        this.snapshotPublisher = new SnapshotPublisher(snapshotCodec, this.protocolVersion, this.interestRadius);
        this.tombstoneTtlTicks = Math.max(1L, config.serverTickRate() * 10L);
        this.tick = ServerParse.parseLong(persistence.getMeta(META_SERVER_TICK, "0"), 0L);
        this.nextObjectId = Math.max(1L, ServerParse.parseLong(persistence.getMeta(META_WORLD_NEXT_OBJECT_ID, "1"), 1L));
        this.worldRevision = Math.max(0L, ServerParse.parseLong(persistence.getMeta(META_WORLD_REVISION, "0"), 0L));
        long respawnSeconds = ServerParse.parseLong(System.getProperty("game.multiplayer.respawnSeconds", "5"), 5L);
        this.gameHost.setRespawnDelayTicks(Math.max(1L, respawnSeconds * config.serverTickRate()));
        this.gameHost.setPvpEnabled(!"false".equalsIgnoreCase(System.getProperty("game.multiplayer.pvp", "true")));
        // Trust the client for ground-object placement terrain by default (matches
        // client-authoritative movement). Avoids client/server shoreline-tile
        // disagreement rejecting valid placements.
        this.gameHost.setTrustClientPlacement(
            !"false".equalsIgnoreCase(System.getProperty("game.multiplayer.trustClientPlacement", "true")));
        this.gameHost.setActiveSessions(this.sessions);
        this.gameHost.setCounters(this.tick, this.worldRevision, this.nextObjectId);
        restoreWorldObjects();
        populateFreshWorld(worldSeed);
    }

    /**
     * Seed the authoritative world with harvestable objects the first time it is
     * created. Guarded by a persisted meta flag so a restarted server does not
     * re-seed on top of persisted state.
     */
    private void populateFreshWorld(long worldSeed) {
        boolean alreadyPopulated = "true".equals(persistence.getMeta(META_WORLD_POPULATED, "false"));
        if (alreadyPopulated) return;
        // Default OFF: the client generates the real, sprited world from the shared
        // seed and (with client-authoritative movement) owns its own collision, so
        // server-seeded generic "tree"/"rock" objects only render as placeholder
        // swatches that clutter the client's real trees. Opt in for a dedicated
        // server that wants server-authoritative harvestables via worldObjectCount.
        int count = (int) ServerParse.parseLong(
            System.getProperty("game.multiplayer.worldObjectCount", "0"), 0L);
        if (count <= 0) { persistence.putMeta(META_WORLD_POPULATED, "true"); return; }
        int radiusTiles = (int) ServerParse.parseLong(
            System.getProperty("game.multiplayer.worldRadiusTiles", "40"), 40L);
        ServerWorldPopulator populator = new ServerWorldPopulator(
            terrainRules, worldSeed, radiusTiles, 64, count);
        int placed = populator.populate(gameHost, tick);
        nextObjectId = gameHost.nextEntityId();
        worldRevision = gameHost.revision();
        worldDirty = true;
        persistence.putMeta(META_WORLD_POPULATED, "true");
        System.out.println("[Lobby] seeded " + placed + " harvestable world objects");
    }

    @Override public synchronized void onConnect(String playerId) { /* no-op: join handled in onJoin */ }

    @Override
    public synchronized void onDisconnect(String playerId) {
        if (playerId == null) return;
        Session removed = sessions.remove(playerId);
        if (removed != null) {
            persist(removed);
            gameHost.removePlayer(playerId);
            presence(playerId, false);
            event(playerId, "disconnect", "");
            broadcastChat("", removed.displayName + " left", true);
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
        gameHost.integratePlayers(sessions, authority, moveSpeedPerTick, tick);
        gameHost.tickPlayerLifecycle(sessions, tick);
        mobSpawner.tick(gameHost, sessions, tick);
        gameHost.tickWorld(sessions, tick);
        nextObjectId = gameHost.nextEntityId();
        worldRevision = gameHost.revision();
        worldDirty = worldDirty || gameHost.dirty();
        pruneTombstones();
        if (tick % snapshotEveryTicks == 0L) {
            gameHost.publishSnapshots(sessions, tick, protocolVersion, interestRadius, snapshotCodec, this::send);
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
            else if (ProtocolMessageType.COMMAND.equals(type)) onCommand(envelope);
            else if (ProtocolMessageType.CHAT.equals(type)) onChat(envelope);
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
            gameHost.syncPlayerFromPersistence(
                playerId, session.x, session.y, session.vx, session.vy, session.lastAcceptedSeq, tick);
            gameHost.syncToSession(session);
        }
        ensureSpawnOnLand(session);
        gameHost.syncPlayerFromPersistence(
            playerId, session.x, session.y, session.vx, session.vy, session.lastAcceptedSeq, tick);
        gameHost.syncToSession(session);
        session.baselineSent = false;
        session.lastSentTick = 0L;
        session.lastChangedTick = tick;
        send(playerId, new ProtocolEnvelope(protocolVersion, playerId, 0L, session.lastAcceptedSeq, tick, ProtocolMessageType.WELCOME, new byte[0]));
        presence(playerId, true);
        event(playerId, "join", "");
        if (freshSession) broadcastChat("", session.displayName + " joined", true);
    }

    private void onInput(ProtocolEnvelope envelope) {
        Session s = sessions.get(envelope.playerId());
        if (s == null) return;
        if (!authority.acceptsSequence(envelope.sequence(), s.lastAcceptedSeq)) return;
        ProtocolPayloads.InputState input = payloadCodec.decodeInputState(envelope.payload());
        boolean changed = s.up != input.up || s.left != input.left || s.down != input.down || s.right != input.right;
        s.lastAcceptedSeq = envelope.sequence();
        s.up = input.up; s.left = input.left; s.down = input.down; s.right = input.right;
        gameHost.setInput(s, input, envelope.sequence(), tick);
        if (changed) s.lastChangedTick = tick;
        ack(s.playerId, s.lastAcceptedSeq);
    }

    private void onAction(ProtocolEnvelope envelope) {
        Session s = sessions.get(envelope.playerId());
        if (s == null) return;
        if (!authority.acceptsSequence(envelope.sequence(), s.lastAcceptedSeq)) return;
        ProtocolPayloads.ActionRequest action = payloadCodec.decodeAction(envelope.payload());
        if (action.action == null || !authority.canPerformAction(s.playerId, action.action, tick)) return;
        // Measure from the player CENTER (corner + 24,48), matching the client's
        // cursor-relative targeting and the command-path range checks. Using the raw
        // corner here under-reported range and rejected edge-of-range targets.
        if (action.hasTarget && !authority.withinRange(
                s.x + 24.0, s.y + 48.0, action.targetX, action.targetY, maxActionRange)) return;
        boolean applied = gameHost.applyAction(s, action, tick, this::event);
        nextObjectId = gameHost.nextEntityId();
        worldRevision = gameHost.revision();
        worldDirty = gameHost.dirty();
        if (!applied) return;
        s.lastAcceptedSeq = envelope.sequence();
        s.lastAction = action.action;
        s.lastChangedTick = tick;
        ack(s.playerId, s.lastAcceptedSeq);
    }

    private void onCommand(ProtocolEnvelope envelope) {
        Session s = sessions.get(envelope.playerId());
        if (s == null) return;
        if (!authority.acceptsSequence(envelope.sequence(), s.lastAcceptedSeq)) {
            commandResult(s.playerId, envelope.sequence(), false, "stale command");
            return;
        }
        ProtocolPayloads.CommandRequest command = payloadCodec.decodeCommand(envelope.payload());
        AuthoritativeGameHost.CommandOutcome outcome = gameHost.applyCommand(
            s, command, envelope.sequence(), tick, this::event);
        nextObjectId = gameHost.nextEntityId();
        worldRevision = gameHost.revision();
        worldDirty = gameHost.dirty();
        if (outcome.accepted) {
            s.lastAcceptedSeq = envelope.sequence();
            s.lastChangedTick = tick;
        }
        commandResult(s.playerId, envelope.sequence(), outcome.accepted, outcome.reason);
    }

    private void integratePlayers() {
        for (Session s : sessions.values()) {
            ensureSpawnOnLand(s);
            double dx = 0.0; double dy = 0.0;
            if (s.up) dy -= moveSpeedPerTick;
            if (s.down) dy += moveSpeedPerTick;
            if (s.left) dx -= moveSpeedPerTick;
            if (s.right) dx += moveSpeedPerTick;
            if (dx != 0.0 && dy != 0.0) { dx *= DIAGONAL; dy *= DIAGONAL; }
            if (!authority.canMove(dx, dy, moveSpeedPerTick)) continue;
            MoveResult movement = moveWithTerrainSteps(s, dx, dy);
            double nextVx = movement.movedX;
            double nextVy = movement.movedY;
            boolean moved = movement.moved;
            boolean velocityChanged = s.vx != nextVx || s.vy != nextVy;
            s.vx = nextVx;
            s.vy = nextVy;
            if (moved || velocityChanged) s.lastChangedTick = tick;
        }
    }

    private MoveResult moveWithTerrainSteps(Session s, double dx, double dy) {
        if (dx == 0.0 && dy == 0.0) return MoveResult.NONE;

        int steps = Math.max(1, (int) Math.ceil(
            Math.max(Math.abs(dx), Math.abs(dy)) / MAX_MOVE_SUBSTEP));
        double stepX = dx / steps;
        double stepY = dy / steps;
        double movedX = 0.0;
        double movedY = 0.0;
        boolean moved = false;

        for (int i = 0; i < steps; i++) {
            double targetX = s.x + stepX;
            double targetY = s.y + stepY;
            if (terrainRules.canPlayerOccupy(targetX, targetY)) {
                s.x = targetX;
                s.y = targetY;
                movedX += stepX;
                movedY += stepY;
                moved = true;
                continue;
            }
            boolean progressed = false;
            if (stepX != 0.0 && terrainRules.canPlayerOccupy(s.x + stepX, s.y)) {
                s.x += stepX;
                movedX += stepX;
                moved = true;
                progressed = true;
            }
            if (stepY != 0.0 && terrainRules.canPlayerOccupy(s.x, s.y + stepY)) {
                s.y += stepY;
                movedY += stepY;
                moved = true;
                progressed = true;
            }
            if (!progressed) break;
        }
        return new MoveResult(movedX, movedY, moved);
    }

    private Session newSession(String playerId) {
        PersistenceStore.PersistedPlayer persisted = persistence.loadPlayer(playerId)
            .orElse(new PersistenceStore.PersistedPlayer(playerId, 0.0, 0.0, 0.0, 0.0, 0L));
        Session session = new Session(persisted);
        gameHost.syncPlayerFromPersistence(
            playerId, persisted.worldX, persisted.worldY, persisted.velocityX, persisted.velocityY, persisted.lastSequence, tick);
        gameHost.syncToSession(session);
        ensureSpawnOnLand(session);
        return session;
    }

    private void ensureSpawnOnLand(Session session) {
        if (session == null) return;
        if (terrainRules.canPlayerOccupy(session.x, session.y)) return;
        double[] spawn = nearestLand(session.x, session.y);
        session.x = spawn[0];
        session.y = spawn[1];
        session.vx = 0.0;
        session.vy = 0.0;
    }

    private double[] nearestLand(double aroundX, double aroundY) {
        final int tile = 64;
        int baseX = (int) Math.rint(aroundX / tile) * tile;
        int baseY = (int) Math.rint(aroundY / tile) * tile;
        for (int radius = 0; radius <= 48; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dy) < radius) continue;
                    double x = baseX + (dx * tile);
                    double y = baseY + (dy * tile);
                    if (terrainRules.canPlayerOccupy(x, y)) {
                        return new double[] { x, y };
                    }
                }
            }
        }
        return new double[] { aroundX, aroundY };
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

    private void commandResult(String playerId, long seq, boolean accepted, String reason) {
        byte[] payload = payloadCodec.encodeCommandResult(new ProtocolPayloads.CommandResult(seq, accepted, reason));
        send(playerId, new ProtocolEnvelope(
            protocolVersion, playerId, 0L, accepted ? seq : 0L, tick, ProtocolMessageType.COMMAND_RESULT, payload));
    }

    private void onChat(ProtocolEnvelope envelope) {
        Session s = sessions.get(envelope.playerId());
        if (s == null) return;
        String text = payloadCodec.decodeChat(envelope.payload());
        if (text == null) return;
        text = text.strip();
        if (text.isEmpty() || text.length() > 240) return;
        broadcastChat(s.displayName, text, false);
        event(s.playerId, "chat", text);
    }

    /** Relay a chat line to every connected client. System lines have no sender. */
    private void broadcastChat(String senderName, String text, boolean system) {
        byte[] payload = payloadCodec.encodeChatBroadcast(senderName, text, system);
        broadcast(new ProtocolEnvelope(
            protocolVersion, "", 0L, 0L, tick, ProtocolMessageType.CHAT_BROADCAST, payload));
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
        if (!worldDirty && !gameHost.dirty() && nextObjectId <= 1L && worldRevision <= 0L) return;
        Map<Long, ProtocolPayloads.Snapshot> chunks = gameHost.persistenceSnapshotsByChunk();
        StringBuilder keys = new StringBuilder();
        for (Map.Entry<Long, ProtocolPayloads.Snapshot> entry : chunks.entrySet()) {
            persistence.saveWorldChunk(entry.getKey().longValue(), payloadCodec.encodeSnapshot(entry.getValue()));
            if (keys.length() > 0) keys.append(',');
            keys.append(entry.getKey().longValue());
        }
        nextObjectId = gameHost.nextEntityId();
        worldRevision = gameHost.revision();
        persistence.putMeta(META_WORLD_NEXT_OBJECT_ID, Long.toString(Math.max(1L, nextObjectId)));
        persistence.putMeta(META_WORLD_REVISION, Long.toString(Math.max(0L, worldRevision)));
        persistence.putMeta(META_WORLD_CHUNK_KEYS, keys.toString());
        worldDirty = false;
        gameHost.clearDirty();
    }

    private void restoreWorldObjects() {
        List<Long> keys = restoredChunkKeys();
        for (Long key : keys) {
            if (key == null) continue;
            byte[] bytes = persistence.loadWorldChunk(key.longValue()).orElse(null);
            if (bytes == null || bytes.length == 0) continue;
            ProtocolPayloads.Snapshot snapshot = payloadCodec.decodeSnapshot(bytes);
            gameHost.restoreFromSnapshot(snapshot, tick);
        }
        nextObjectId = gameHost.nextEntityId();
        worldRevision = gameHost.revision();
    }

    private List<Long> restoredChunkKeys() {
        ArrayList<Long> keys = new ArrayList<>();
        String encoded = persistence.getMeta(META_WORLD_CHUNK_KEYS, "");
        if (encoded != null && !encoded.isBlank()) {
            String[] parts = encoded.split(",");
            for (String part : parts) {
                if (part == null || part.isBlank()) continue;
                try { keys.add(Long.parseLong(part.trim())); }
                catch (NumberFormatException ignored) {}
            }
        }
        if (!keys.isEmpty()) return keys;
        keys.addAll(persistence.listWorldChunkKeys());
        if (!keys.isEmpty()) return keys;
        keys.add(WORLD_CHUNK_KEY);
        return keys;
    }

    private void pruneTombstones() {
        gameHost.pruneTombstones(tick, tombstoneTtlTicks);
    }

    private void event(String playerId, String type, String payload) {
        persistence.appendSessionEvent(tick, playerId, type, payload);
    }

    private static final class MoveResult {
        static final MoveResult NONE = new MoveResult(0.0, 0.0, false);
        final double movedX;
        final double movedY;
        final boolean moved;

        MoveResult(double movedX, double movedY, boolean moved) {
            this.movedX = movedX;
            this.movedY = movedY;
            this.moved = moved;
        }
    }
}
