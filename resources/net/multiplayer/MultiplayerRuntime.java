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

    private final GameContext ctx;
    private final MultiplayerConfig config;
    private final MultiplayerServerAdapter adapter;
    private final RemotePlayerDirectory remotes;
    private final ReplicatedObjectDirectory worldObjects;

    private long sequence;
    private boolean started;
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

    public void update(double delta) {
        if (!config.online()) return;
        ensureStarted();
        adapter.tick();
        publishMovement();
        publishActions();
        consume(adapter.poll());
        remotes.interpolate(config.interpolationDelayMs(), config.snapshotRate());
    }

    public void close() {
        if (!started) return;
        adapter.submit(new ClientLeaveMessage(config.playerId()));
        adapter.disconnect(config.playerId());
        started = false;
    }

    private void ensureStarted() {
        if (started) return;
        if (config.mode() == MultiplayerMode.HOST && isWebSocketBackend(config.backend())) {
            int port = parsePort(System.getProperty("game.multiplayer.gatewayPort", "8080"));
            if (System.getProperty("game.multiplayer.serverUrl", "").isBlank()) {
                System.setProperty("game.multiplayer.serverUrl", "ws://127.0.0.1:" + port + "/ws");
            }
            EmbeddedWebSocketHost.ensureStarted(config, port);
        }
        adapter.connect(config.playerId());
        adapter.submit(new ClientJoinMessage(config.playerId()));
        started = true;
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
                reconcileLocal(snapshot.players(), snapshot.acknowledgedSequence());
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
        catch (NumberFormatException ignored) { return 8080; }
    }

    private static final class PredictedInput {
        final long sequence;
        PredictedInput(long sequence) { this.sequence = sequence; }
    }
}
