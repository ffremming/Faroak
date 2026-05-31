package resources.net.multiplayer;

import java.util.List;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.awt.Point;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.Barrel;
import resources.domain.object.Boat;
import resources.domain.object.Chest;
import resources.domain.object.CraftingTable;
import resources.input.InputAction;
import resources.input.InputHandlingSystem;
import resources.net.multiplayer.message.ClientCommandMessage;
import resources.net.multiplayer.message.ClientInputMessage;
import resources.net.multiplayer.message.ClientJoinMessage;
import resources.net.multiplayer.message.ClientLeaveMessage;
import resources.net.multiplayer.message.ClientPingMessage;
import resources.net.multiplayer.message.PlayerStateMessage;
import resources.net.multiplayer.message.ServerAckMessage;
import resources.net.multiplayer.message.ServerCommandResultMessage;
import resources.net.multiplayer.message.ServerMessage;
import resources.net.multiplayer.message.ServerPlayerPresenceMessage;
import resources.net.multiplayer.message.ServerSnapshotMessage;
import resources.net.multiplayer.message.ServerWelcomeMessage;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.presentation.ui.NetworkContainerUIBridge;

/**
 * Coordinates multiplayer networking in the client runtime.
 *
 * Uses ports/adapters:
 * - port: {@link MultiplayerServerAdapter}
 * - adapter choice: {@link MultiplayerAdapterRegistry}
 */
public final class MultiplayerRuntime {

    private static final long DEFAULT_RECONNECT_DELAY_MS = 1000L;
    private static final int MAX_LOCAL_SAMPLES = 20;

    private final GameContext ctx;
    private final MultiplayerConfig config;
    private final MultiplayerServerAdapter adapter;
    private final RemotePlayerDirectory remotes;
    private final ReplicatedWorldState replicatedWorld;
    private final boolean reconnectEnabled;
    private final long reconnectDelayMs;
    private final boolean localReconcileEnabled;
    private final int localInterpolationDelayMs;
    private final double localReconcileWarpDistance;
    private final double localReconcileBlend;

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
    private final ArrayDeque<LocalSnapshotSample> localSamples = new ArrayDeque<>();
    private final Map<Long, Runnable> commandAcceptedHandlers = new HashMap<>();

    private MultiplayerRuntime(GameContext ctx, MultiplayerConfig config, MultiplayerServerAdapter adapter) {
        this.ctx = ctx;
        this.config = config;
        this.adapter = adapter;
        this.remotes = new RemotePlayerDirectory(ctx);
        this.replicatedWorld = new ReplicatedWorldState(ctx);
        this.reconnectEnabled = parseBoolean(
            "game.multiplayer.reconnect.enabled", true);
        this.reconnectDelayMs = parseLong(
            "game.multiplayer.reconnectDelayMs", DEFAULT_RECONNECT_DELAY_MS, 0L, 60_000L);
        this.localReconcileEnabled = parseBoolean(
            "game.multiplayer.reconcileLocal", true);
        this.localInterpolationDelayMs = (int) parseLong(
            "game.multiplayer.localInterpolationDelayMs", 0L, 0L, 250L);
        this.localReconcileWarpDistance = parseDouble(
            "game.multiplayer.localReconcileWarpDistance", 96.0, 1.0, 2048.0);
        this.localReconcileBlend = parseDouble(
            "game.multiplayer.localReconcileBlend", 0.15, 0.01, 1.0);
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
        return replicatedWorld.entityCount();
    }

    public int replicatedInventoryCount() {
        return replicatedWorld.inventoryCount();
    }

    public int replicatedTileMutationCount() {
        return replicatedWorld.tileMutationCount();
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

    public boolean localPlayerRiding() {
        return replicatedWorld.ridingEntityIdFor(config.playerId()) > 0L;
    }

    public boolean submitWorldClick(double worldX, double worldY) {
        if (closed || !config.online() || !joined) return false;
        BaseEntity clicked = entityAt(worldX, worldY);
        Stack equipped = ctx.player() == null ? null : ctx.player().getEquipped();
        String itemType = stackName(equipped);
        int selectedSlot = selectedInventorySlot();

        if (clicked instanceof Boat && !"boat".equals(itemType)) {
            long entityId = replicatedWorld.entityIdFor(clicked);
            long seq = submitCommand(ProtocolPayloads.CommandRequest.interactEntity(entityId, worldX, worldY));
            return seq > 0L;
        }

        if (isContainer(clicked)) {
            long entityId = replicatedWorld.entityIdFor(clicked);
            long seq = submitCommand(ProtocolPayloads.CommandRequest.interactEntity(entityId, worldX, worldY));
            if (seq > 0L) {
                commandAcceptedHandlers.put(seq, () -> openNetworkContainer(clicked, entityId));
                return true;
            }
            return false;
        }

        long seq = submitCommand(ProtocolPayloads.CommandRequest.useEquippedAt(worldX, worldY, itemType, selectedSlot));
        return seq > 0L;
    }

    public boolean submitInventoryClick(Inventory inventory, int slotIndex, int button) {
        if (closed || !config.online() || !joined || inventory == null) return false;
        long inventoryId = inventoryIdFor(inventory);
        if (inventoryId < 0L) return false;
        long seq = submitCommand(ProtocolPayloads.CommandRequest.inventoryClick(inventoryId, slotIndex, button));
        return seq > 0L;
    }

    public void update(double delta) {
        if (closed || !config.online()) return;
        ensureStarted();
        adapter.tick();
        consume(adapter.poll());
        if (localReconcileEnabled) applyLocalAuthoritativePose();
        if (!adapter.isConnected()) {
            reconnectIfDue();
            return;
        }
        sendJoinIfPossible();
        if (!joined) return;
        publishMovement();
        publishActions();
        remotes.interpolate(config.interpolationDelayMs(), config.serverTickRate());
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
        localSamples.clear();
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
        localSamples.clear();
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
            if (MultiplayerAction.PLACE.equals(mapped) && ctx.mouse() != null) {
                submitWorldClick(ctx.mouse().getMouseWorldX(), ctx.mouse().getMouseWorldY());
            } else if (MultiplayerAction.ATTACK.equals(mapped)) {
                double x = ctx.mouse() == null ? localPlayerX() : ctx.mouse().getMouseWorldX();
                double y = ctx.mouse() == null ? localPlayerY() : ctx.mouse().getMouseWorldY();
                submitCommand(ProtocolPayloads.CommandRequest.attackAt(x, y));
            } else if (MultiplayerAction.INTERACT.equals(mapped)) {
                submitCommand(ProtocolPayloads.CommandRequest.interactAt(localPlayerX(), localPlayerY()));
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
                replicatedWorld.applySnapshot(
                    snapshot.baseline(),
                    snapshot.worldObjects(),
                    snapshot.entities(),
                    snapshot.inventories(),
                    snapshot.tileMutations());
                applyLocalPlayerInventorySnapshot();
                if (localReconcileEnabled) {
                    reconcileLocal(snapshot.players(), snapshot.acknowledgedSequence());
                }
            } else if (message instanceof ServerAckMessage) {
                onAck((ServerAckMessage) message);
            } else if (message instanceof ServerCommandResultMessage) {
                onCommandResult((ServerCommandResultMessage) message);
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

    private void onCommandResult(ServerCommandResultMessage result) {
        if (!config.playerId().equals(result.playerId())) return;
        lastAckedSeq = Math.max(lastAckedSeq, result.commandSequence());
        Runnable accepted = commandAcceptedHandlers.remove(result.commandSequence());
        if (result.accepted()) {
            if (accepted != null) accepted.run();
            return;
        }
        if (ctx.userInterface() != null && result.reason() != null && !result.reason().isBlank()) {
            ctx.userInterface().showToast(result.reason(), 1200L);
        }
    }

    private void reconcileLocal(List<PlayerStateMessage> players, long ackSeq) {
        if (players == null || ctx.player() == null) return;
        if (ackSeq > 0L) onAck(new ServerAckMessage(config.playerId(), ackSeq, 0L));
        for (PlayerStateMessage state : players) {
            if (state == null || !config.playerId().equals(state.playerId())) continue;
            pushLocalSample(state);
            break;
        }
    }

    private void pushLocalSample(PlayerStateMessage state) {
        long nowMs = System.currentTimeMillis();
        localSamples.addLast(new LocalSnapshotSample(
            nowMs, state.worldX(), state.worldY(), state.velocityX(), state.velocityY()));
        while (localSamples.size() > MAX_LOCAL_SAMPLES) localSamples.removeFirst();
    }

    private void applyLocalAuthoritativePose() {
        if (ctx.player() == null || localSamples.isEmpty()) return;
        long nowMs = System.currentTimeMillis();
        long targetMs = nowMs - Math.max(0, localInterpolationDelayMs);

        while (localSamples.size() >= 2) {
            LocalSnapshotSample second = sampleAt(1);
            if (second == null || second.arrivedAtMs > targetMs) break;
            localSamples.removeFirst();
        }

        double nextX;
        double nextY;
        if (localSamples.size() >= 2) {
            LocalSnapshotSample a = localSamples.peekFirst();
            LocalSnapshotSample b = sampleAt(1);
            if (a == null || b == null) return;
            long span = Math.max(1L, b.arrivedAtMs - a.arrivedAtMs);
            double alpha = clamp((targetMs - a.arrivedAtMs) / (double) span, 0.0, 1.0);
            nextX = lerp(a.x, b.x, alpha);
            nextY = lerp(a.y, b.y, alpha);
        } else {
            LocalSnapshotSample only = localSamples.peekFirst();
            if (only == null) return;
            long aheadMs = Math.max(0L, targetMs - only.arrivedAtMs);
            double cappedAhead = Math.min(aheadMs, 1000.0 / Math.max(1, config.serverTickRate()));
            double ticksAhead = cappedAhead * (Math.max(1, config.serverTickRate()) / 1000.0);
            nextX = only.x + (only.vx * ticksAhead);
            nextY = only.y + (only.vy * ticksAhead);
        }

        double currentX = ctx.player().getWorldX();
        double currentY = ctx.player().getWorldY();
        double dx = nextX - currentX;
        double dy = nextY - currentY;
        double dist2 = (dx * dx) + (dy * dy);
        if (dist2 >= localReconcileWarpDistance * localReconcileWarpDistance) {
            ctx.player().setWorldX(nextX);
            ctx.player().setWorldY(nextY);
        } else if (dist2 > 0.01) {
            ctx.player().setWorldX(currentX + (dx * localReconcileBlend));
            ctx.player().setWorldY(currentY + (dy * localReconcileBlend));
        }
        ctx.player().getHitBox().updateCoords();
    }

    private LocalSnapshotSample sampleAt(int index) {
        int i = 0;
        for (LocalSnapshotSample sample : localSamples) {
            if (i == index) return sample;
            i++;
        }
        return null;
    }

    private long movementMask(InputHandlingSystem input) {
        long mask = 0L;
        if (input.isUp()) mask |= 1L;
        if (input.isLeft()) mask |= 2L;
        if (input.isDown()) mask |= 4L;
        if (input.isRight()) mask |= 8L;
        return mask;
    }

    private long submitCommand(ProtocolPayloads.CommandRequest command) {
        if (command == null || command.commandType == null || command.commandType.isBlank()) return 0L;
        long seq = ++sequence;
        adapter.submit(new ClientCommandMessage(config.playerId(), seq, command));
        return seq;
    }

    private BaseEntity entityAt(double worldX, double worldY) {
        if (ctx.world() == null) return null;
        ArrayDeque<BaseEntity> hits = new ArrayDeque<>();
        for (BaseEntity entity : ctx.world().getEntitiesCollidedWith(new Point((int) Math.round(worldX), (int) Math.round(worldY)))) {
            if (entity == null || entity == ctx.player()) continue;
            hits.addLast(entity);
        }
        BaseEntity fallback = null;
        for (BaseEntity entity : hits) {
            if (isContainer(entity) || entity instanceof Boat) return entity;
            fallback = entity;
        }
        return fallback;
    }

    private boolean isContainer(BaseEntity entity) {
        return entity instanceof Chest || entity instanceof Barrel || entity instanceof CraftingTable;
    }

    private String stackName(Stack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String name = stack.getName();
        return name == null ? "" : name.trim().toLowerCase();
    }

    private int selectedInventorySlot() {
        if (ctx.player() == null || ctx.player().getInventory() == null) return -1;
        return Inventory.HOTBAR_OFFSET + ctx.player().getInventory().getIndex();
    }

    private long inventoryIdFor(Inventory inventory) {
        if (inventory instanceof NetworkInventory) return ((NetworkInventory) inventory).inventoryId();
        if (ctx.player() != null && inventory == ctx.player().getInventory()) {
            ProtocolPayloads.InventoryStatePayload playerInv = replicatedWorld.inventoryForPlayer(config.playerId());
            return playerInv == null ? -1L : playerInv.inventoryId;
        }
        return -1L;
    }

    private void openNetworkContainer(BaseEntity clicked, long entityId) {
        if (!(ctx instanceof GamePanel) || clicked == null || entityId <= 0L) return;
        ProtocolPayloads.InventoryStatePayload container = replicatedWorld.inventoryForOwner(entityId);
        ProtocolPayloads.InventoryStatePayload player = replicatedWorld.inventoryForPlayer(config.playerId());
        if (container == null || player == null) {
            if (ctx.userInterface() != null) ctx.userInterface().showToast("Container state not ready", 1200L);
            return;
        }
        GamePanel panel = (GamePanel) ctx;
        NetworkInventory containerInv = new NetworkInventory(
            panel, container.inventoryId, () -> replicatedWorld.inventory(container.inventoryId));
        NetworkInventory playerInv = new NetworkInventory(
            panel, player.inventoryId, () -> replicatedWorld.inventory(player.inventoryId));
        NetworkContainerUIBridge.open(panel, clicked, container.inventoryType, containerInv, playerInv);
    }

    private void applyLocalPlayerInventorySnapshot() {
        if (ctx.player() == null || ctx.player().getInventory() == null || !(ctx instanceof GamePanel)) return;
        ProtocolPayloads.InventoryStatePayload snapshot = replicatedWorld.inventoryForPlayer(config.playerId());
        if (snapshot == null) return;
        Inventory local = ctx.player().getInventory();
        int limit = Math.min(local.getSize(), snapshot.slots.size());
        GamePanel panel = (GamePanel) ctx;
        for (int i = 0; i < limit; i++) {
            ProtocolPayloads.ItemStackPayload slot = snapshot.slots.get(i);
            local.setStack(i, toLocalStack(panel, slot));
        }
        ProtocolPayloads.InventoryStatePayload cursor = replicatedWorld.cursorForPlayer(config.playerId());
        if (cursor != null && !cursor.slots.isEmpty()) {
            ProtocolPayloads.ItemStackPayload held = cursor.slots.get(0);
            ctx.player().setTempInHand(toCursorStack(panel, held));
        }
    }

    private Stack toLocalStack(GamePanel panel, ProtocolPayloads.ItemStackPayload slot) {
        if (slot == null || slot.amount <= 0 || "empty".equals(slot.itemType)) {
            return new Stack(panel, "empty");
        }
        return new Stack(panel, new Item(panel, slot.itemType), slot.amount);
    }

    private Stack toCursorStack(GamePanel panel, ProtocolPayloads.ItemStackPayload slot) {
        if (slot == null || slot.amount <= 0 || "empty".equals(slot.itemType)) return null;
        return new Stack(panel, new Item(panel, slot.itemType), slot.amount);
    }

    private double localPlayerX() {
        return ctx.player() == null ? 0.0 : ctx.player().getWorldX();
    }

    private double localPlayerY() {
        return ctx.player() == null ? 0.0 : ctx.player().getWorldY();
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

    private static double parseDouble(String key, double fallback, double min, double max) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            double value = Double.parseDouble(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + ((b - a) * t);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static final class PredictedInput {
        final long sequence;
        PredictedInput(long sequence) { this.sequence = sequence; }
    }

    private static final class LocalSnapshotSample {
        final long arrivedAtMs;
        final double x;
        final double y;
        final double vx;
        final double vy;

        LocalSnapshotSample(long arrivedAtMs, double x, double y, double vx, double vy) {
            this.arrivedAtMs = arrivedAtMs;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }
}
