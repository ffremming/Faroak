package resources.net.multiplayer;

import java.util.List;
import java.util.ArrayDeque;

import resources.app.GameContext;
import resources.input.InputAction;
import resources.input.InputHandlingSystem;
import resources.net.multiplayer.message.ClientActionMessage;
import resources.net.multiplayer.message.ClientInputMessage;
import resources.net.multiplayer.message.ClientJoinMessage;
import resources.net.multiplayer.message.ClientLeaveMessage;
import resources.net.multiplayer.message.ClientPingMessage;
import resources.net.multiplayer.message.PlayerStateMessage;
import resources.net.multiplayer.message.ServerAckMessage;
import resources.net.multiplayer.message.ServerMessage;
import resources.net.multiplayer.message.ServerPlayerPresenceMessage;
import resources.net.multiplayer.message.ServerSnapshotMessage;
import resources.net.multiplayer.message.ServerWelcomeMessage;

/**
 * Coordinates multiplayer networking in the client runtime.
 *
 * Uses ports/adapters:
 * - port: {@link MultiplayerServerAdapter}
 * - adapter choice: {@link MultiplayerAdapterRegistry}
 */
public final class MultiplayerRuntime {

    private static final long DEFAULT_RECONNECT_DELAY_MS = 1000L;

    private final GameContext ctx;
    private final MultiplayerConfig config;
    private final MultiplayerServerAdapter adapter;
    private final RemotePlayerDirectory remotes;
    private final ReplicatedObjectDirectory worldObjects;
    private final boolean reconnectEnabled;
    private final long reconnectDelayMs;
    private final boolean localReconcileEnabled;

    private long sequence;
    private boolean started;
    private boolean joined;
    private boolean joinSent;
    private boolean closed;
    private long nextReconnectAttemptAtMs;
    private long lastMovementMask = -1L;
    private long pingCounter;
    private long lastAckedSeq;
    private final ArrayDeque<PredictedInput> pendingInputs = new ArrayDeque<>();

    private MultiplayerRuntime(GameContext ctx, MultiplayerConfig config, MultiplayerServerAdapter adapter) {
        this.ctx = ctx;
        this.config = config;
        this.adapter = adapter;
        this.remotes = new RemotePlayerDirectory(ctx);
        this.worldObjects = new ReplicatedObjectDirectory(ctx);
        this.reconnectEnabled = parseBoolean(
            "game.multiplayer.reconnect.enabled", true);
        this.reconnectDelayMs = parseLong(
            "game.multiplayer.reconnectDelayMs", DEFAULT_RECONNECT_DELAY_MS, 0L, 60_000L);
        this.localReconcileEnabled = parseBoolean(
            "game.multiplayer.reconcileLocal", true);
    }

    public static MultiplayerRuntime createDefault(GameContext ctx) {
        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        MultiplayerServerAdapter adapter = config.online()
            ? MultiplayerAdapterRegistry.create(config)
            : new NoopServerAdapter();
        return new MultiplayerRuntime(ctx, config, adapter);
    }

    public MultiplayerMode mode() {
        return config.mode();
    }

    public boolean isOnline() {
        return config.online();
    }

    public boolean isJoined() {
        return joined;
    }

    public int remotePlayerCount() {
        return remotes.size();
    }

    public int replicatedObjectCount() {
        return worldObjects.size();
    }

    public double remotePlayersMeanX() {
        return remotes.meanX();
    }

    public double remotePlayersMeanY() {
        return remotes.meanY();
    }

    public long lastAckedSequence() {
        return lastAckedSeq;
    }

    public long localSequence() {
        return sequence;
    }

    public void update(double delta) {
        if (closed || !config.online()) return;
        ensureStarted();
        adapter.tick();
        consume(adapter.poll());
        if (!adapter.isConnected()) {
            reconnectIfDue();
            return;
        }
        sendJoinIfPossible();
        if (!joined) return;
        publishMovement();
        publishActions();
        remotes.interpolate(config.interpolationDelayMs(), config.snapshotRate());
    }

    public void close() {
        if (closed) return;
        closed = true;
        if (!started) return;
        adapter.submit(new ClientLeaveMessage(config.playerId()));
        adapter.disconnect(config.playerId());
        started = false;
        joined = false;
        joinSent = false;
        nextReconnectAttemptAtMs = 0L;
    }

    private void ensureStarted() {
        if (started || closed) return;
        if (config.mode() == MultiplayerMode.HOST && isWebSocketBackend(config.backend())) {
            int port = parsePort(System.getProperty("game.multiplayer.gatewayPort", "8080"));
            if (System.getProperty("game.multiplayer.serverUrl", "").isBlank()) {
                System.setProperty("game.multiplayer.serverUrl", "ws://127.0.0.1:" + port + "/ws");
            }
            EmbeddedWebSocketHost.ensureStarted(config, port);
        }
        started = true;
        connectAndJoin();
    }

    private void reconnectIfDue() {
        if (!started || closed || !reconnectEnabled) return;
        long now = System.currentTimeMillis();
        if (now < nextReconnectAttemptAtMs) return;
        connectAndJoin();
        if (!adapter.isConnected()) {
            nextReconnectAttemptAtMs = now + reconnectDelayMs;
        }
    }

    private void connectAndJoin() {
        joined = false;
        joinSent = false;
        pendingInputs.clear();
        lastMovementMask = -1L;
        adapter.connect(config.playerId());
        nextReconnectAttemptAtMs = 0L;
        sendJoinIfPossible();
    }

    private void sendJoinIfPossible() {
        if (closed || joined || joinSent || !adapter.isConnected()) return;
        boolean hasSpawn = ctx.player() != null;
        double spawnX = hasSpawn ? ctx.player().getWorldX() : 0.0;
        double spawnY = hasSpawn ? ctx.player().getWorldY() : 0.0;
        adapter.submit(new ClientJoinMessage(config.playerId(), hasSpawn, spawnX, spawnY));
        joinSent = true;
    }

    private void publishMovement() {
        InputHandlingSystem input = ctx.input();
        long mask = movementMask(input);
        if (mask == lastMovementMask) return;
        long seq = ++sequence;
        adapter.submit(new ClientInputMessage(config.playerId(), seq,
            input.isUp(), input.isLeft(), input.isDown(), input.isRight()));
        pendingInputs.addLast(new PredictedInput(seq));
        lastMovementMask = mask;
    }

    private void publishActions() {
        List<InputAction> actions = ctx.input().drainActions();
        for (InputAction action : actions) {
            MultiplayerAction mapped = MultiplayerAction.fromInput(action);
            if (mapped == null) continue;
            long seq = ++sequence;
            if (MultiplayerAction.PLACE.equals(mapped) && ctx.mouse() != null) {
                String placeType = "";
                if (ctx.player() != null && ctx.player().getEquipped() != null) {
                    placeType = ctx.player().getEquipped().getName();
                }
                adapter.submit(new ClientActionMessage(
                    config.playerId(), seq, mapped, true,
                    ctx.mouse().getMouseWorldX(), ctx.mouse().getMouseWorldY(), placeType));
            } else {
                adapter.submit(new ClientActionMessage(config.playerId(), seq, mapped));
            }
        }
        pingCounter++;
        if (pingCounter % Math.max(1, config.serverTickRate()) == 0L) {
            adapter.submit(new ClientPingMessage(config.playerId(), ++sequence, System.currentTimeMillis()));
        }
    }

    private void consume(List<ServerMessage> messages) {
        for (ServerMessage message : messages) {
            if (message instanceof ServerWelcomeMessage) {
                onWelcome((ServerWelcomeMessage) message);
            } else if (message instanceof ServerSnapshotMessage) {
                ServerSnapshotMessage snapshot = (ServerSnapshotMessage) message;
                remotes.applySnapshot(snapshot.tick(), snapshot.baseline(), snapshot.players(), config.playerId());
                worldObjects.applySnapshot(snapshot.baseline(), snapshot.worldObjects());
                if (localReconcileEnabled) {
                    reconcileLocal(snapshot.players(), snapshot.acknowledgedSequence());
                }
            } else if (message instanceof ServerAckMessage) {
                onAck((ServerAckMessage) message);
            } else if (message instanceof ServerPlayerPresenceMessage) {
                // Presence events are consumed by UI later; current runtime only
                // needs snapshots for simulation.
            }
        }
    }

    private void onWelcome(ServerWelcomeMessage welcome) {
        if (!welcome.playerId().equals(config.playerId())) return;
        if (!welcome.accepted()) {
            close();
            System.out.println("multiplayer join rejected: " + welcome.reason());
            return;
        }
        joined = true;
        joinSent = true;
        long acknowledged = welcome.acknowledgedSequence();
        if (acknowledged > 0L) {
            onAck(new ServerAckMessage(config.playerId(), acknowledged, 0L));
            sequence = Math.max(sequence, acknowledged);
        }
    }

    private void onAck(ServerAckMessage ack) {
        if (!config.playerId().equals(ack.playerId())) return;
        lastAckedSeq = Math.max(lastAckedSeq, ack.acknowledgedSequence());
        while (!pendingInputs.isEmpty() && pendingInputs.peekFirst().sequence <= lastAckedSeq) {
            pendingInputs.removeFirst();
        }
    }

    private void reconcileLocal(List<PlayerStateMessage> players, long ackSeq) {
        if (players == null || ctx.player() == null) return;
        if (ackSeq > 0L) onAck(new ServerAckMessage(config.playerId(), ackSeq, 0L));
        for (PlayerStateMessage state : players) {
            if (state == null || !config.playerId().equals(state.playerId())) continue;
            double dx = state.worldX() - ctx.player().getWorldX();
            double dy = state.worldY() - ctx.player().getWorldY();
            double dist2 = (dx * dx) + (dy * dy);
            if (dist2 > 400.0) {
                ctx.player().setWorldX(state.worldX());
                ctx.player().setWorldY(state.worldY());
                ctx.player().getHitBox().updateCoords();
            } else if (dist2 > 1.0) {
                ctx.player().setWorldX(ctx.player().getWorldX() + (dx * 0.25));
                ctx.player().setWorldY(ctx.player().getWorldY() + (dy * 0.25));
                ctx.player().getHitBox().updateCoords();
            }
            break;
        }
    }

    private long movementMask(InputHandlingSystem input) {
        long mask = 0L;
        if (input.isUp()) mask |= 1L;
        if (input.isLeft()) mask |= 2L;
        if (input.isDown()) mask |= 4L;
        if (input.isRight()) mask |= 8L;
        return mask;
    }

    private static boolean isWebSocketBackend(String backend) {
        if (backend == null) return false;
        String value = backend.trim().toLowerCase();
        return "websocket".equals(value) || "ws".equals(value);
    }

    private static int parsePort(String raw) {
        if (raw == null || raw.isBlank()) return 8080;
        try { return Integer.parseInt(raw.trim()); }
        catch (NumberFormatException ignored) { return 8080; } // expected: non-numeric port falls back to default
    }

    private static boolean parseBoolean(String key, boolean fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        if ("true".equalsIgnoreCase(raw.trim())) return true;
        if ("false".equalsIgnoreCase(raw.trim())) return false;
        return fallback;
    }

    private static long parseLong(String key, long fallback, long min, long max) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            long value = Long.parseLong(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback; // expected: non-numeric config value falls back to default
        }
    }

    private static final class PredictedInput {
        final long sequence;
        PredictedInput(long sequence) { this.sequence = sequence; }
    }
}
