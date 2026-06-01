package resources.net.multiplayer;

import java.util.List;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.awt.Point;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.combat.CombatService;
import resources.domain.combat.WeaponProfile;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.HealthComponent;
import resources.geometry.Vector;
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
import resources.net.multiplayer.message.ClientChatMessage;
import resources.net.multiplayer.message.ServerChatMessage;
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
    // Client-side visual combat effects (swing arc / hit flash). Damage stays
    // authoritative on the server; this only plays the local animation so the
    // attacker sees feedback online, mirroring the offline path.
    private final CombatService combatEffects = new CombatService();
    private final boolean reconnectEnabled;
    private final long reconnectDelayMs;
    private final boolean localReconcileEnabled;
    private final boolean clientAuthoritativeMovement;
    private final int localInterpolationDelayMs;
    // Divergence (px) the local player may differ from the server before we snap-
    // correct. Below this, crisp local movement is left fully in charge (no mushy
    // lerp); at/above it, a genuine desync is corrected by a single collision-safe
    // snap rather than a glide.
    private final double localReconcileTolerance;

    private long sequence;
    private boolean started;
    private boolean joined;
    private boolean joinSent;
    private boolean closed;
    private long nextReconnectAttemptAtMs;
    private long lastMovementMask = -1L;
    private double lastSentPosX = Double.NaN;
    private double lastSentPosY = Double.NaN;
    private long pingCounter;
    private long lastAckedSeq;
    private boolean localAlive = true;
    private final java.util.ArrayDeque<String> chatLog = new java.util.ArrayDeque<>();
    private final java.util.Set<String> roster = new java.util.LinkedHashSet<>();
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
        // Client-authoritative movement (default on): the client reports its own
        // collision-resolved position and the server adopts it, so the local player is
        // never snapped to a re-simulated server pose. Set false to revert to the old
        // server-simulated movement + reconciliation.
        this.clientAuthoritativeMovement = parseBoolean(
            "game.multiplayer.clientAuthoritativeMovement", true);
        this.localInterpolationDelayMs = (int) parseLong(
            "game.multiplayer.localInterpolationDelayMs", 0L, 0L, 250L);
        // While moving steadily the local (predicted) player legitimately leads the
        // latest *received* server pose by roughly speed × pipeline-delay (interp delay
        // + snapshot interval + network latency). Correcting within that lead causes a
        // periodic backward snap as the lead rebuilds and re-trips the threshold. So
        // the tolerance must exceed the expected lead: only a genuine desync (teleport,
        // respawn, big lag) should snap. Default is derived from the move pipeline with
        // generous headroom; override with -Dgame.multiplayer.localReconcileTolerance.
        double pxPerMs = (config.serverMoveSpeedPerTick() * config.serverTickRate()) / 1000.0;
        double pipelineMs = config.interpolationDelayMs() + (1000.0 / Math.max(1, config.snapshotRate())) + 120.0;
        double derivedTolerance = Math.max(96.0, pxPerMs * pipelineMs * 1.5);
        this.localReconcileTolerance = parseDouble(
            "game.multiplayer.localReconcileTolerance", derivedTolerance, 1.0, 4096.0);
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

    /**
     * Ask the server to respawn the local player. The authoritative alive-state
     * flips back via the next snapshot, which is what actually clears the death
     * UI — this just submits the request. Used by the death-screen "Respawn"
     * button (the dead interact-key path in {@link #publishActions} does the same).
     */
    public boolean requestRespawn() {
        if (closed || !config.online() || !joined) return false;
        long seq = submitCommand(ProtocolPayloads.CommandRequest.respawn());
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
        // Client-authoritative movement: the server mirrors the client's reported
        // position, so the local player must NOT be snapped toward the server pose
        // (that was the teleport). Reconciliation only runs as a fallback if the
        // client-authoritative path is explicitly disabled.
        if (localReconcileEnabled && !clientAuthoritativeMovement) applyLocalAuthoritativePose();
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
        // Client-authoritative movement: the client owns its own collision-resolved
        // position and reports it to the server. The server adopts it (clamped)
        // instead of re-simulating with a different collision model.
        double px = localPlayerX();
        double py = localPlayerY();

        if (!localAlive) {
            // Dead: report a stopped input once, holding the current position.
            if (lastMovementMask != 0L) {
                long seq = ++sequence;
                adapter.submit(new ClientInputMessage(config.playerId(), seq,
                    false, false, false, false, true, px, py));
                pendingInputs.addLast(new PredictedInput(seq));
                lastMovementMask = 0L;
            }
            return;
        }

        long mask = movementMask(input);
        // Send when the keys changed OR the player actually moved since the last
        // report, so the server position tracks the client continuously (needed for
        // range-gated actions and to keep remotes in sync) without flooding when idle.
        boolean moved = Math.abs(px - lastSentPosX) > 0.5 || Math.abs(py - lastSentPosY) > 0.5;
        if (mask == lastMovementMask && !moved) return;

        long seq = ++sequence;
        adapter.submit(new ClientInputMessage(config.playerId(), seq,
            input.isUp(), input.isLeft(), input.isDown(), input.isRight(), true, px, py));
        pendingInputs.addLast(new PredictedInput(seq));
        lastMovementMask = mask;
        lastSentPosX = px;
        lastSentPosY = py;
    }

    private void publishActions() {
        List<InputAction> actions = ctx.input().drainActions();
        if (!localAlive) {
            // Dead: any interact press requests a respawn; all other actions ignored.
            for (InputAction action : actions) {
                if (InputAction.INTERACT.equals(action)) {
                    submitCommand(ProtocolPayloads.CommandRequest.respawn());
                    break;
                }
            }
            return;
        }
        for (InputAction action : actions) {
            if (InputAction.ATTACK.equals(action) || InputAction.ATTACK_LIGHT.equals(action)) {
                submitAttackCommand(ProtocolPayloads.CommandRequest.ATTACK_LIGHT_AT);
                playLocalSwing(false);
                continue;
            }
            if (InputAction.ATTACK_HEAVY.equals(action)) {
                submitAttackCommand(ProtocolPayloads.CommandRequest.ATTACK_HEAVY_AT);
                playLocalSwing(true);
                continue;
            }
            if (InputAction.ATTACK_RANGED.equals(action)) {
                submitAttackCommand(ProtocolPayloads.CommandRequest.ATTACK_RANGED_AT);
                continue;
            }
            MultiplayerAction mapped = MultiplayerAction.fromInput(action);
            if (mapped == null) continue;
            if (MultiplayerAction.PLACE.equals(mapped) && ctx.mouse() != null) {
                submitWorldClick(ctx.mouse().getMouseWorldX(), ctx.mouse().getMouseWorldY());
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
                applyLocalPlayerHealthSnapshot(snapshot.players());
                applyWorldTime(snapshot.worldTimeTicks());
                if (localReconcileEnabled) {
                    reconcileLocal(snapshot.players(), snapshot.acknowledgedSequence());
                }
            } else if (message instanceof ServerAckMessage) {
                onAck((ServerAckMessage) message);
            } else if (message instanceof ServerCommandResultMessage) {
                onCommandResult((ServerCommandResultMessage) message);
            } else if (message instanceof ServerPlayerPresenceMessage) {
                onPresence((ServerPlayerPresenceMessage) message);
            } else if (message instanceof ServerChatMessage) {
                onChat((ServerChatMessage) message);
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

    /**
     * Reconcile the local player toward the latest authoritative server pose. The
     * local player moves immediately from input (client-side prediction); each frame
     * we nudge it a fraction of the way toward the most recent server position. Because
     * the correction is measured against the current pose it CONVERGES — it shrinks to
     * zero as the client aligns with the server and then stops, so the player does not
     * drift when no input is held. Large divergence (teleport/desync) snaps directly.
     */
    private void applyLocalAuthoritativePose() {
        if (ctx.player() == null || localSamples.isEmpty()) return;
        LocalSnapshotSample server = localSamples.peekLast();
        if (server == null) return;

        double curX = ctx.player().getWorldX();
        double curY = ctx.player().getWorldY();

        // Local prediction uses the same move speed + collision as the server, so in
        // normal play the two track each other closely. Leave the player entirely to
        // crisp local movement until divergence exceeds a tolerance — this avoids the
        // mushy "coasting" feel a continuous lerp produced. Only a genuine desync
        // (teleport, dropped inputs) triggers a correction, and then we snap rather
        // than glide so start/stop stays sharp.
        if (!needsReconcileSnap(curX, curY, server.x, server.y, localReconcileTolerance)) return;

        // Snap to the authoritative pose — but NEVER into a tile the client treats as
        // solid (water/walls). Reconciliation bypasses the movement collision loop, so
        // without this guard a correction near a shoreline can place the player on
        // water. If the target is solid, hold the current (collision-valid) position.
        if (!correctedPositionIsSolid(server.x, server.y)) {
            ctx.player().setWorldX(server.x);
            ctx.player().setWorldY(server.y);
            ctx.player().getHitBox().updateCoords();
        }
    }

    /** True if moving the local player to (x,y) would put its hitbox in solid terrain
     *  (e.g. water). Restores the player's position afterward — pure query. */
    private boolean correctedPositionIsSolid(double x, double y) {
        double savedX = ctx.player().getWorldX();
        double savedY = ctx.player().getWorldY();
        ctx.player().setWorldX(x);
        ctx.player().setWorldY(y);
        ctx.player().getHitBox().updateCoords();
        boolean solid = ctx.world().solidCollision(ctx.player().getHitBox(), ctx.player());
        ctx.player().setWorldX(savedX);
        ctx.player().setWorldY(savedY);
        ctx.player().getHitBox().updateCoords();
        return solid;
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

    private long submitAttackCommand(String commandType) {
        double x = ctx.mouse() == null ? localPlayerX() : ctx.mouse().getMouseWorldX();
        double y = ctx.mouse() == null ? localPlayerY() : ctx.mouse().getMouseWorldY();
        Stack equipped = ctx.player() == null ? null : ctx.player().getEquipped();
        return submitCommand(new ProtocolPayloads.CommandRequest(
            commandType, true, x, y, 0L, stackName(equipped), selectedInventorySlot(), 0L, -1, 0));
    }

    /**
     * Play the local melee swing VFX online. Damage is authoritative on the server;
     * this is the same visual the offline path spawns so the attacker gets immediate
     * feedback (the swing arc + facing) instead of nothing. {@code heavy} widens the
     * arc/duration to match the offline heavy attack.
     */
    private void playLocalSwing(boolean heavy) {
        if (ctx.player() == null || ctx.input() == null) return;
        Stack equipped = ctx.player().getEquipped();
        String itemName = (equipped == null || equipped.isEmpty()) ? null : equipped.getName();
        WeaponProfile weapon = WeaponProfile.forItem(itemName);
        if (weapon.swingSpriteName == null || weapon.swingSpriteName.isBlank()) return;
        Vector aim = ctx.input().combatAimVector();
        combatEffects.spawnActionEffects(
            ctx.player(), ctx, aim,
            weapon.swingSpriteName,
            heavy ? weapon.swingDurationTicks + 2 : weapon.swingDurationTicks,
            heavy ? weapon.swingArcDegrees + 10.0 : weapon.swingArcDegrees,
            weapon.swingRadiusPx,
            null);
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

    private void applyLocalPlayerHealthSnapshot(List<PlayerStateMessage> players) {
        if (players == null || ctx.player() == null) return;
        for (PlayerStateMessage state : players) {
            if (state == null || !config.playerId().equals(state.playerId())) continue;
            HealthComponent health = ctx.player().getComponent(HealthComponent.class);
            if (health == null) health = ctx.player().addComponent(new HealthComponent(Math.max(1, state.maxHealth())));
            int current = health.current();
            if (state.health() < current) {
                health.takeDamage(current - state.health());
            } else if (state.health() > current) {
                health.heal(state.health() - current);
            }
            updateLocalAliveState(state.alive());
            break;
        }
    }

    /** Track local death/respawn transitions and notify the player. */
    private void updateLocalAliveState(boolean alive) {
        if (alive == localAlive) return;
        localAlive = alive;
        if (!alive) {
            toast("You died — press the interact key to respawn", 6000L);
        } else {
            toast("Respawned", 2000L);
        }
    }

    /** True while the local player is dead (server-authoritative); input is suppressed. */
    public boolean localPlayerDead() {
        return config.online() && !localAlive;
    }

    /** True once the server is authoritative for the world clock (online + joined),
     *  so the local frame loop should stop ticking the clock itself. */
    public boolean drivesWorldClock() {
        return config.online() && joined;
    }

    /** Drive the client day/night clock from the authoritative server world time. */
    private void applyWorldTime(long serverTicks) {
        if (serverTicks <= 0L || ctx.clock() == null) return;
        long local = ctx.clock().ticks();
        long delta = serverTicks - local;
        if (delta > 0L) {
            ctx.clock().advance(delta);
        }
        // If the local clock is ahead (rare: a delta arrived out of order), leave it —
        // GameClock only advances forward, and the next in-order snapshot realigns it.
    }

    private void toast(String text, long durationMs) {
        try {
            if (ctx.userInterface() != null) ctx.userInterface().showToast(text, durationMs);
        } catch (RuntimeException ignored) {
            // UI not ready yet (early frames) — a missed toast is harmless.
        }
    }

    private void onPresence(ServerPlayerPresenceMessage presence) {
        if (presence.playerId() == null || presence.playerId().isBlank()) return;
        if (presence.joined()) roster.add(presence.playerId());
        else roster.remove(presence.playerId());
    }

    private void onChat(ServerChatMessage chat) {
        String line = chat.system()
            ? "* " + chat.text()
            : chat.senderName() + ": " + chat.text();
        chatLog.addLast(line);
        while (chatLog.size() > 50) chatLog.removeFirst();
        toast(line, 5000L);
    }

    /** Send a chat line to the server for relay to everyone. No-op if offline/empty. */
    public void sendChat(String text) {
        if (!config.online() || text == null) return;
        String trimmed = text.strip();
        if (trimmed.isEmpty() || trimmed.length() > 240) return;
        adapter.submit(new ClientChatMessage(config.playerId(), trimmed));
    }

    /** Recent chat lines (oldest first), for a chat overlay. */
    public java.util.List<String> recentChat() {
        return new java.util.ArrayList<>(chatLog);
    }

    /** Connected player ids (roster), for a player-list overlay. */
    public java.util.List<String> roster() {
        return new java.util.ArrayList<>(roster);
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

    /**
     * Pure reconciliation decision, extracted for testing. Crisp local movement owns
     * the player until it diverges from the server by more than {@code tolerance} px;
     * only then does the client correct, and it does so by a single snap (collision
     * safety is applied by the caller). Returns true when a snap to the server pose
     * is warranted, false to leave the local (predicted) pose untouched.
     */
    public static boolean needsReconcileSnap(
            double curX, double curY,
            double serverX, double serverY,
            double tolerance) {
        double dx = serverX - curX;
        double dy = serverY - curY;
        return (dx * dx + dy * dy) > (tolerance * tolerance);
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
