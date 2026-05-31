package resources.net.multiplayer.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import resources.net.multiplayer.MultiplayerAction;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.authority.AuthorityService;
import resources.net.multiplayer.server.codec.SnapshotCodec;
import resources.net.multiplayer.state.EntityState;
import resources.net.multiplayer.state.InventoryState;
import resources.net.multiplayer.state.ItemStackState;
import resources.net.multiplayer.state.PlayerReplicaState;
import resources.net.multiplayer.state.TileMutationState;
import resources.net.multiplayer.state.WorldState;

/**
 * Headless authoritative game simulation state. This is the migration point away
 * from the old Session + SimObject-only model: the lobby still owns transport,
 * but gameplay mutations live here as typed state.
 */
final class AuthoritativeGameHost {

    private static final String DEFAULT_DIMENSION = "core:overworld";
    private static final double DIAGONAL = 0.70710678118;
    private static final double MAX_MOVE_SUBSTEP = 4.0;
    private static final double MIN_ENTITY_GAP = 32.0;
    private static final int TILE_SIZE = 64;
    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int PLAYER_HOTBAR_OFFSET = 27;
    private static final int CURSOR_SLOTS = 1;
    private static final int MAX_STACK = 99;
    private static final double PLAYER_RIDER_OFFSET_X = -24.0;
    private static final double PLAYER_RIDER_OFFSET_Y = -64.0;

    private final WorldState world = new WorldState();
    private final ServerTerrainRules terrainRules;
    private final double maxActionRange;
    private boolean dirty;

    AuthoritativeGameHost(ServerTerrainRules terrainRules, double maxActionRange) {
        this.terrainRules = terrainRules;
        this.maxActionRange = Math.max(1.0, maxActionRange);
    }

    WorldState world() { return world; }
    boolean dirty() { return dirty; }
    void clearDirty() { dirty = false; }

    void setCounters(long tick, long revision, long nextEntityId) {
        world.setCounters(tick, revision, nextEntityId);
    }

    long revision() { return world.revision(); }
    long nextEntityId() { return world.nextEntityId(); }

    PlayerReplicaState ensurePlayer(
            String playerId,
            boolean hasSpawn,
            double spawnX,
            double spawnY,
            long processedSequence,
            long tick) {
        PlayerReplicaState existing = world.player(playerId);
        if (existing != null) return existing;
        double x = hasSpawn ? spawnX : 0.0;
        double y = hasSpawn ? spawnY : 0.0;
        if (terrainRules != null && !terrainRules.canPlayerOccupy(x, y)) {
            double[] spawn = nearestLand(x, y);
            x = spawn[0];
            y = spawn[1];
        }
        PlayerReplicaState player = new PlayerReplicaState(playerId, DEFAULT_DIMENSION, x, y, processedSequence, tick);
        world.putPlayer(player);
        ensurePlayerInventories(playerId, tick);
        return player;
    }

    void restoreFromSnapshot(ProtocolPayloads.Snapshot snapshot, long tick) {
        if (snapshot == null) return;
        long maxRevision = world.revision();
        for (ProtocolPayloads.WorldObjectState state : snapshot.worldObjects) {
            if (state == null || state.removed) continue;
            EntityState entity = new EntityState(
                state.objectId, state.objectType, DEFAULT_DIMENSION,
                state.worldX, state.worldY, state.revision, tick);
            world.putEntity(entity);
            maxRevision = Math.max(maxRevision, state.revision);
        }
        for (ProtocolPayloads.EntityStatePayload state : snapshot.entities) {
            if (state == null) continue;
            EntityState entity = new EntityState(
                state.entityId, state.entityType, state.dimensionId,
                state.worldX, state.worldY, state.revision, tick);
            for (ProtocolPayloads.ComponentStatePayload component : state.components) {
                if (component != null) entity.putComponent(component.key, component.value, state.revision, tick);
            }
            if (state.removed) entity.markRemoved(state.revision, tick);
            world.putEntity(entity);
            maxRevision = Math.max(maxRevision, state.revision);
        }
        for (ProtocolPayloads.InventoryStatePayload payload : snapshot.inventories) {
            if (payload == null) continue;
            InventoryState inventory = new InventoryState(
                payload.inventoryId, payload.ownerEntityId, payload.inventoryType,
                payload.slots.size(), payload.revision, tick);
            for (int i = 0; i < payload.slots.size(); i++) {
                ProtocolPayloads.ItemStackPayload slot = payload.slots.get(i);
                inventory.setSlot(i, new ItemStackState(slot.itemType, slot.amount), payload.revision, tick);
            }
            world.putInventory(inventory);
            maxRevision = Math.max(maxRevision, payload.revision);
        }
        for (ProtocolPayloads.TileMutationPayload payload : snapshot.tileMutations) {
            if (payload == null) continue;
            TileMutationState tile = new TileMutationState(
                payload.dimensionId, payload.tileX, payload.tileY, payload.tileType, payload.revision, tick);
            if (payload.watered) tile.water(payload.revision, tick);
            if (payload.cropType != null && !payload.cropType.isBlank()) {
                tile.plant(payload.cropType, payload.revision, tick);
                tile.advanceCrop(payload.cropStage, payload.revision, tick);
            }
            world.putTileMutation(tile);
            maxRevision = Math.max(maxRevision, payload.revision);
        }
        world.setCounters(tick, Math.max(world.revision(), maxRevision), world.nextEntityId());
    }

    void syncPlayerFromPersistence(
            String playerId,
            double x,
            double y,
            double vx,
            double vy,
            long sequence,
            long tick) {
        PlayerReplicaState player = ensurePlayer(playerId, true, x, y, sequence, tick);
        player.moveTo(DEFAULT_DIMENSION, x, y, vx, vy, tick);
    }

    void syncToSession(Session session) {
        if (session == null) return;
        PlayerReplicaState player = world.player(session.playerId);
        if (player == null) return;
        session.x = player.worldX();
        session.y = player.worldY();
        session.vx = player.velocityX();
        session.vy = player.velocityY();
        session.lastAcceptedSeq = Math.max(session.lastAcceptedSeq, player.processedSequence());
        session.lastChangedTick = Math.max(session.lastChangedTick, player.lastChangedTick());
    }

    void removePlayer(String playerId) {
        world.removePlayer(playerId);
    }

    void setInput(Session session, ProtocolPayloads.InputState input, long sequence, long tick) {
        if (session == null || input == null) return;
        PlayerReplicaState player = ensurePlayer(session.playerId, true, session.x, session.y, session.lastAcceptedSeq, tick);
        player.setInput(input.up, input.left, input.down, input.right, sequence, tick);
    }

    void integratePlayers(Map<String, Session> sessions, AuthorityService authority, double moveSpeedPerTick, long tick) {
        world.setTick(tick);
        for (Session session : sessions.values()) {
            PlayerReplicaState player = ensurePlayer(session.playerId, true, session.x, session.y, session.lastAcceptedSeq, tick);
            player.setInput(session.up, session.left, session.down, session.right, session.lastAcceptedSeq, tick);
            if (player.ridingEntityId() > 0L) {
                integrateRidingPlayer(session, player, authority, moveSpeedPerTick, tick);
                continue;
            }
            ensureOnLand(player, tick);

            double dx = 0.0;
            double dy = 0.0;
            if (player.up()) dy -= moveSpeedPerTick;
            if (player.down()) dy += moveSpeedPerTick;
            if (player.left()) dx -= moveSpeedPerTick;
            if (player.right()) dx += moveSpeedPerTick;
            if (dx != 0.0 && dy != 0.0) { dx *= DIAGONAL; dy *= DIAGONAL; }
            if (authority != null && !authority.canMove(dx, dy, moveSpeedPerTick)) continue;
            MoveResult movement = moveWithTerrainSteps(player, dx, dy, tick);
            player.moveTo(player.dimensionId(), player.worldX(), player.worldY(), movement.movedX, movement.movedY, tick);
            syncToSession(session);
        }
    }

    CommandOutcome applyCommand(
            Session session,
            ProtocolPayloads.CommandRequest command,
            long sequence,
            long tick,
            ActionResolver.EventSink events) {
        if (session == null || command == null || command.commandType == null || command.commandType.isBlank()) {
            return CommandOutcome.reject("invalid command");
        }
        world.setTick(tick);
        ensurePlayerInventories(session.playerId, tick);
        if (ProtocolPayloads.CommandRequest.USE_EQUIPPED_AT.equals(command.commandType)) {
            return applyUseEquippedAt(session, command, sequence, tick, events);
        }
        if (ProtocolPayloads.CommandRequest.INTERACT_ENTITY.equals(command.commandType)
                || ProtocolPayloads.CommandRequest.INTERACT_AT.equals(command.commandType)) {
            return applyInteractCommand(session, command, sequence, tick, events);
        }
        if (ProtocolPayloads.CommandRequest.ATTACK_AT.equals(command.commandType)) {
            ProtocolPayloads.ActionRequest attack = new ProtocolPayloads.ActionRequest(
                MultiplayerAction.ATTACK, command.hasTarget, command.targetX, command.targetY, "");
            if (!applyAttack(session, attack, tick, events)) return CommandOutcome.reject("nothing to attack");
            markSequence(session, sequence, tick);
            return CommandOutcome.accept();
        }
        if (ProtocolPayloads.CommandRequest.INVENTORY_CLICK.equals(command.commandType)) {
            return applyInventoryClick(session, command, sequence, tick, events);
        }
        return CommandOutcome.reject("unsupported command");
    }

    boolean applyAction(
            Session session,
            ProtocolPayloads.ActionRequest action,
            long tick,
            ActionResolver.EventSink events) {
        if (session == null || action == null || action.action == null) return false;
        world.setTick(tick);
        if (MultiplayerAction.PLACE.equals(action.action)) {
            return applyPlace(session, action, tick, events);
        }
        if (MultiplayerAction.ATTACK.equals(action.action)) {
            return applyAttack(session, action, tick, events);
        }
        if (MultiplayerAction.INTERACT.equals(action.action)) {
            return applyInteract(session, action, tick, events);
        }
        return false;
    }

    void pruneTombstones(long tick, long ttlTicks) {
        ArrayList<Long> expired = new ArrayList<>();
        for (EntityState entity : world.entities()) {
            if (!entity.removed()) continue;
            if ((tick - entity.lastChangedTick()) > ttlTicks) expired.add(entity.entityId());
        }
        for (Long id : expired) world.removeEntity(id.longValue());
    }

    void publishSnapshots(
            Map<String, Session> sessions,
            long tick,
            int protocolVersion,
            double interestRadius,
            SnapshotCodec snapshotCodec,
            SnapshotPublisher.Sender sender) {
        for (Session recipient : sessions.values()) {
            boolean baseline = !recipient.baselineSent;
            ArrayList<ProtocolPayloads.PlayerState> players = new ArrayList<>();
            for (Session candidate : sessions.values()) {
                if (!isInterested(recipient, candidate, interestRadius)) continue;
                if (!baseline && !candidate.changedSince(recipient.lastSentTick)) continue;
                players.add(candidate.toPayloadState());
            }

            ArrayList<ProtocolPayloads.WorldObjectState> compatibilityObjects = compatibilityObjects(baseline, recipient.lastSentTick);
            ArrayList<ProtocolPayloads.EntityStatePayload> entities = entityPayloads(baseline, recipient.lastSentTick);
            ArrayList<ProtocolPayloads.InventoryStatePayload> inventories = inventoryPayloads(baseline, recipient.lastSentTick);
            ArrayList<ProtocolPayloads.TileMutationPayload> tiles = tilePayloads(baseline, recipient.lastSentTick);
            if (!baseline && players.isEmpty() && compatibilityObjects.isEmpty()
                    && entities.isEmpty() && inventories.isEmpty() && tiles.isEmpty()) {
                continue;
            }

            ProtocolPayloads.Snapshot snapshot = new ProtocolPayloads.Snapshot(
                baseline, recipient.lastAcceptedSeq, players, compatibilityObjects, entities, inventories, tiles);
            byte[] payload = snapshotCodec.encode(snapshot);
            ProtocolMessageType type = baseline ? ProtocolMessageType.BASELINE_SNAPSHOT : ProtocolMessageType.DELTA_SNAPSHOT;
            sender.send(recipient.playerId, new ProtocolEnvelope(
                protocolVersion, recipient.playerId, 0L, recipient.lastAcceptedSeq, tick, type, payload));
            recipient.baselineSent = true;
            recipient.lastSentTick = tick;
        }
    }

    ProtocolPayloads.Snapshot persistenceSnapshot() {
        return new ProtocolPayloads.Snapshot(
            true, 0L, new ArrayList<>(), compatibilityObjects(true, 0L),
            entityPayloads(true, 0L), inventoryPayloads(true, 0L), tilePayloads(true, 0L));
    }

    Map<Long, ProtocolPayloads.Snapshot> persistenceSnapshotsByChunk() {
        LinkedHashMap<Long, ChunkPayloadBuilder> chunks = new LinkedHashMap<>();
        for (EntityState entity : world.entities()) {
            if (entity.removed()) continue;
            long key = chunkKeyForWorld(entity.dimensionId(), entity.worldX(), entity.worldY());
            ChunkPayloadBuilder builder = chunks.computeIfAbsent(key, ignored -> new ChunkPayloadBuilder());
            builder.compatibilityObjects.add(new ProtocolPayloads.WorldObjectState(
                entity.entityId(), entity.entityType(), entity.worldX(), entity.worldY(), false, entity.revision()));
            ArrayList<ProtocolPayloads.ComponentStatePayload> components = new ArrayList<>();
            for (Map.Entry<String, String> entry : entity.components().entrySet()) {
                components.add(new ProtocolPayloads.ComponentStatePayload(entry.getKey(), entry.getValue()));
            }
            builder.entities.add(new ProtocolPayloads.EntityStatePayload(
                entity.entityId(), entity.entityType(), entity.dimensionId(), entity.worldX(), entity.worldY(),
                false, entity.revision(), components));
        }
        for (InventoryState inventory : world.inventories()) {
            EntityState owner = world.entity(inventory.ownerEntityId());
            if (owner == null || owner.removed()) continue;
            long key = chunkKeyForWorld(owner.dimensionId(), owner.worldX(), owner.worldY());
            ChunkPayloadBuilder builder = chunks.computeIfAbsent(key, ignored -> new ChunkPayloadBuilder());
            ArrayList<ProtocolPayloads.ItemStackPayload> slots = new ArrayList<>();
            for (ItemStackState stack : inventory.slots()) {
                slots.add(new ProtocolPayloads.ItemStackPayload(stack.itemType(), stack.amount()));
            }
            builder.inventories.add(new ProtocolPayloads.InventoryStatePayload(
                inventory.inventoryId(), inventory.ownerEntityId(), inventory.inventoryType(), inventory.revision(), slots));
        }
        for (TileMutationState tile : world.tileMutations()) {
            long key = chunkKeyForTile(tile.dimensionId(), tile.tileX(), tile.tileY());
            ChunkPayloadBuilder builder = chunks.computeIfAbsent(key, ignored -> new ChunkPayloadBuilder());
            builder.tiles.add(new ProtocolPayloads.TileMutationPayload(
                tile.dimensionId(), tile.tileX(), tile.tileY(), tile.tileType(), tile.watered(),
                tile.cropType(), tile.cropStage(), tile.revision()));
        }

        LinkedHashMap<Long, ProtocolPayloads.Snapshot> out = new LinkedHashMap<>();
        for (Map.Entry<Long, ChunkPayloadBuilder> entry : chunks.entrySet()) {
            ChunkPayloadBuilder builder = entry.getValue();
            out.put(entry.getKey(), new ProtocolPayloads.Snapshot(
                true, 0L, new ArrayList<>(), builder.compatibilityObjects,
                builder.entities, builder.inventories, builder.tiles));
        }
        if (out.isEmpty()) out.put(0L, persistenceSnapshot());
        return out;
    }

    private boolean applyPlace(Session session, ProtocolPayloads.ActionRequest action, long tick, ActionResolver.EventSink events) {
        if (!action.hasTarget) return false;
        String type = sanitizeObjectType(action.argument);
        if (type.isBlank()) return false;
        return applyPlaceType(session, type, action.targetX, action.targetY, tick, events);
    }

    private boolean applyPlaceType(
            Session session,
            String rawType,
            double targetX,
            double targetY,
            long tick,
            ActionResolver.EventSink events) {
        String type = sanitizeObjectType(rawType);
        if (type.isBlank()) return false;
        double[] center = placementCenter(type, targetX, targetY);
        double x = Math.rint(center[0]);
        double y = Math.rint(center[1]);

        if (isTilling(type)) return tillTile(session, x, y, tick, events);
        if (isWatering(type)) return waterTile(session, x, y, tick, events);
        if (isSeed(type)) return plantSeed(session, type, x, y, tick, events);

        if (terrainRules != null && !terrainRules.canPlaceObject(type, x, y)) return false;
        if (placementBlocked(x, y)) return false;

        long revision = world.bumpRevision();
        EntityState entity = new EntityState(world.allocateEntityId(), type, DEFAULT_DIMENSION, x, y, revision, tick);
        attachDefaultComponents(entity, revision, tick);
        world.putEntity(entity);
        attachInventoryIfContainer(entity, revision, tick);
        dirty = true;
        if (events != null) events.event(session.playerId, "action", "place:" + type + ":" + (int) x + "," + (int) y);
        return true;
    }

    private boolean applyAttack(Session session, ProtocolPayloads.ActionRequest action, long tick, ActionResolver.EventSink events) {
        double targetX = action.hasTarget ? action.targetX : session.x;
        double targetY = action.hasTarget ? action.targetY : session.y;
        EntityState nearest = nearestEntity(targetX, targetY, maxActionRange);
        if (nearest == null) {
            if (events != null) events.event(session.playerId, "action", "attack:none");
            return true;
        }
        nearest.markRemoved(world.bumpRevision(), tick);
        dirty = true;
        if (events != null) events.event(session.playerId, "action", "attack:remove:" + nearest.entityId());
        return true;
    }

    private boolean applyInteract(Session session, ProtocolPayloads.ActionRequest action, long tick, ActionResolver.EventSink events) {
        double targetX = action.hasTarget ? action.targetX : session.x;
        double targetY = action.hasTarget ? action.targetY : session.y;
        EntityState nearest = nearestEntity(targetX, targetY, maxActionRange);
        if (nearest == null) {
            if (events != null) events.event(session.playerId, "action", "interact:none");
            return true;
        }
        CommandOutcome outcome = interactWithEntity(session, nearest, tick, events);
        if (!outcome.accepted) return false;
        InventoryState inventory = inventoryForOwner(nearest.entityId());
        if (inventory != null) inventory.setSlot(0, inventory.slot(0), world.bumpRevision(), tick);
        if (events != null) events.event(session.playerId, "action", "interact:" + nearest.entityId());
        return true;
    }

    private CommandOutcome applyUseEquippedAt(
            Session session,
            ProtocolPayloads.CommandRequest command,
            long sequence,
            long tick,
            ActionResolver.EventSink events) {
        if (!command.hasTarget) return CommandOutcome.reject("missing target");
        if (!withinRange(session.x, session.y, command.targetX, command.targetY, maxActionRange)) {
            return CommandOutcome.reject("too far away");
        }
        InventoryState inventory = playerInventory(session.playerId);
        if (inventory == null) return CommandOutcome.reject("missing inventory");
        int slotIndex = command.selectedSlot;
        if (slotIndex < 0 || slotIndex >= inventory.slotCount()) return CommandOutcome.reject("invalid selected slot");
        ItemStackState selected = inventory.slot(slotIndex);
        if (selected.isEmpty()) return CommandOutcome.reject("empty hand");
        if (!command.itemType.isBlank() && !command.itemType.equals(selected.itemType())) {
            return CommandOutcome.reject("selected item changed");
        }
        String type = selected.itemType();
        if (!isServerUsableItem(type)) {
            ProtocolPayloads.ActionRequest attack = new ProtocolPayloads.ActionRequest(
                MultiplayerAction.ATTACK, true, command.targetX, command.targetY, "");
            return applyAttack(session, attack, tick, events) ? CommandOutcome.accept() : CommandOutcome.reject("nothing to use");
        }
        boolean applied = applyPlaceType(session, type, command.targetX, command.targetY, tick, events);
        if (!applied) return CommandOutcome.reject("cannot use item here");
        if (consumesOnUse(type)) {
            decrementSlot(inventory, slotIndex, 1, world.bumpRevision(), tick);
        } else {
            inventory.setSlot(slotIndex, selected, world.bumpRevision(), tick);
        }
        markSequence(session, sequence, tick);
        return CommandOutcome.accept();
    }

    private CommandOutcome applyInteractCommand(
            Session session,
            ProtocolPayloads.CommandRequest command,
            long sequence,
            long tick,
            ActionResolver.EventSink events) {
        EntityState target = command.targetEntityId > 0L
            ? world.entity(command.targetEntityId)
            : nearestEntity(
                command.hasTarget ? command.targetX : session.x,
                command.hasTarget ? command.targetY : session.y,
                maxActionRange);
        if (target == null || target.removed()) return CommandOutcome.reject("nothing to interact with");
        if (!withinRange(session.x, session.y, target.worldX(), target.worldY(), maxActionRange)) {
            return CommandOutcome.reject("too far away");
        }
        CommandOutcome outcome = interactWithEntity(session, target, tick, events);
        if (!outcome.accepted) return outcome;
        InventoryState inventory = inventoryForOwner(target.entityId());
        if (inventory != null) inventory.setSlot(0, inventory.slot(0), world.bumpRevision(), tick);
        markSequence(session, sequence, tick);
        if (events != null) events.event(session.playerId, "action", "interact:" + target.entityId());
        return CommandOutcome.accept();
    }

    private CommandOutcome applyInventoryClick(
            Session session,
            ProtocolPayloads.CommandRequest command,
            long sequence,
            long tick,
            ActionResolver.EventSink events) {
        InventoryState target = world.inventory(command.inventoryId);
        InventoryState cursor = cursorInventory(session.playerId);
        if (target == null || cursor == null) return CommandOutcome.reject("inventory not found");
        if (!canAccessInventory(session, target)) return CommandOutcome.reject("inventory too far away");
        if (command.slotIndex < 0 || command.slotIndex >= target.slotCount()) return CommandOutcome.reject("invalid slot");

        long revision = world.bumpRevision();
        boolean changed = command.button == 3
            ? rightClickSlot(target, cursor, command.slotIndex, revision, tick)
            : leftClickSlot(target, cursor, command.slotIndex, revision, tick);
        if (!changed) {
            markSequence(session, sequence, tick);
            return CommandOutcome.accept();
        }
        dirty = true;
        markSequence(session, sequence, tick);
        if (events != null) events.event(session.playerId, "inventory", target.inventoryId() + ":" + command.slotIndex);
        return CommandOutcome.accept();
    }

    private CommandOutcome interactWithEntity(
            Session session,
            EntityState entity,
            long tick,
            ActionResolver.EventSink events) {
        long revision = world.bumpRevision();
        if ("boat".equals(entity.entityType())) {
            return toggleBoatRide(session, entity, revision, tick);
        }
        entity.putComponent("last_interacted_by", session.playerId, revision, tick);
        dirty = true;
        return CommandOutcome.accept();
    }

    private CommandOutcome toggleBoatRide(Session session, EntityState boat, long revision, long tick) {
        PlayerReplicaState player = world.player(session.playerId);
        if (player == null) return CommandOutcome.reject("missing player");
        long currentRide = player.ridingEntityId();
        if (currentRide == boat.entityId()) {
            double[] shore = nearestDismountLand(boat.worldX(), boat.worldY());
            if (shore == null) return CommandOutcome.reject("steer to land to disembark");
            player.setRidingEntityId(0L, tick);
            player.moveTo(player.dimensionId(), shore[0], shore[1], 0.0, 0.0, tick);
            boat.putComponent("rider", "", revision, tick);
            syncSessionToPlayer(session, player);
            dirty = true;
            return CommandOutcome.accept();
        }
        String rider = boat.component("rider");
        if (rider != null && !rider.isBlank() && !session.playerId.equals(rider)) {
            return CommandOutcome.reject("boat already occupied");
        }
        if (!withinRange(session.x, session.y, boat.worldX(), boat.worldY(), maxActionRange * 2.0)) {
            return CommandOutcome.reject("too far from boat");
        }
        player.setRidingEntityId(boat.entityId(), tick);
        boat.putComponent("rider", session.playerId, revision, tick);
        movePlayerToBoat(session, player, boat, tick);
        dirty = true;
        return CommandOutcome.accept();
    }

    private boolean tillTile(Session session, double x, double y, long tick, ActionResolver.EventSink events) {
        int tx = tileCoord(x);
        int ty = tileCoord(y);
        if (terrainRules != null && terrainRules.isWaterAt(x, y)) return false;
        long revision = world.bumpRevision();
        TileMutationState tile = world.tileMutation(DEFAULT_DIMENSION, tx, ty);
        if (tile == null) {
            tile = new TileMutationState(DEFAULT_DIMENSION, tx, ty, "farmland", revision, tick);
            world.putTileMutation(tile);
        }
        dirty = true;
        if (events != null) events.event(session.playerId, "action", "till:" + tx + "," + ty);
        return true;
    }

    private boolean waterTile(Session session, double x, double y, long tick, ActionResolver.EventSink events) {
        int tx = tileCoord(x);
        int ty = tileCoord(y);
        TileMutationState tile = world.tileMutation(DEFAULT_DIMENSION, tx, ty);
        if (tile == null) return false;
        tile.water(world.bumpRevision(), tick);
        dirty = true;
        if (events != null) events.event(session.playerId, "action", "water:" + tx + "," + ty);
        return true;
    }

    private boolean plantSeed(Session session, String seedType, double x, double y, long tick, ActionResolver.EventSink events) {
        int tx = tileCoord(x);
        int ty = tileCoord(y);
        TileMutationState tile = world.tileMutation(DEFAULT_DIMENSION, tx, ty);
        if (tile == null || !tile.cropType().isBlank()) return false;
        tile.plant(cropTypeForSeed(seedType), world.bumpRevision(), tick);
        dirty = true;
        if (events != null) events.event(session.playerId, "action", "plant:" + seedType + ":" + tx + "," + ty);
        return true;
    }

    private void attachDefaultComponents(EntityState entity, long revision, long tick) {
        entity.putComponent("dimension", entity.dimensionId(), revision, tick);
        if ("boat".equals(entity.entityType())) {
            entity.putComponent("movement", "water_only", revision, tick);
            entity.putComponent("health", "100", revision, tick);
            entity.putComponent("rider", "", revision, tick);
        } else if ("chest".equals(entity.entityType()) || "barrel".equals(entity.entityType())) {
            entity.putComponent("container", entity.entityType(), revision, tick);
        } else if ("crafting_table".equals(entity.entityType())) {
            entity.putComponent("crafting", "table", revision, tick);
        } else if (entity.entityType().contains("portal") || "door".equals(entity.entityType())) {
            entity.putComponent("portal", "pending_destination", revision, tick);
        }
    }

    private void attachInventoryIfContainer(EntityState entity, long revision, long tick) {
        int slots = containerSlots(entity.entityType());
        if (slots <= 0) return;
        InventoryState inventory = new InventoryState(entity.entityId(), entity.entityId(), entity.entityType(), slots, revision, tick);
        world.putInventory(inventory);
    }

    private InventoryState inventoryForOwner(long ownerEntityId) {
        for (InventoryState inventory : world.inventories()) {
            if (inventory.ownerEntityId() == ownerEntityId) return inventory;
        }
        return null;
    }

    private void ensurePlayerInventories(String playerId, long tick) {
        if (playerId == null || playerId.isBlank()) return;
        if (playerInventory(playerId) == null) {
            long revision = world.bumpRevision();
            InventoryState inventory = new InventoryState(
                world.allocateEntityId(), 0L, playerInventoryType(playerId), PLAYER_INVENTORY_SLOTS, revision, tick);
            seedPlayerInventory(inventory, revision, tick);
            world.putInventory(inventory);
            dirty = true;
        }
        if (cursorInventory(playerId) == null) {
            long revision = world.bumpRevision();
            InventoryState cursor = new InventoryState(
                world.allocateEntityId(), 0L, cursorInventoryType(playerId), CURSOR_SLOTS, revision, tick);
            world.putInventory(cursor);
            dirty = true;
        }
    }

    private InventoryState playerInventory(String playerId) {
        String type = playerInventoryType(playerId);
        for (InventoryState inventory : world.inventories()) {
            if (type.equals(inventory.inventoryType())) return inventory;
        }
        return null;
    }

    private InventoryState cursorInventory(String playerId) {
        String type = cursorInventoryType(playerId);
        for (InventoryState inventory : world.inventories()) {
            if (type.equals(inventory.inventoryType())) return inventory;
        }
        return null;
    }

    private String playerInventoryType(String playerId) {
        return "player:" + normalizePlayerId(playerId);
    }

    private String cursorInventoryType(String playerId) {
        return "cursor:" + normalizePlayerId(playerId);
    }

    private String normalizePlayerId(String playerId) {
        return playerId == null ? "" : playerId.trim().toLowerCase();
    }

    private void seedPlayerInventory(InventoryState inventory, long revision, long tick) {
        setSlot(inventory, 0, "hammer", 1, revision, tick);
        setSlot(inventory, 1, "axe", 1, revision, tick);
        setSlot(inventory, 2, "pickaxe", 1, revision, tick);
        setSlot(inventory, 3, "shovel", 1, revision, tick);
        setSlot(inventory, 4, "sword", 1, revision, tick);
        setSlot(inventory, 5, "block", 64, revision, tick);
        setSlot(inventory, 6, "block", 99, revision, tick);
        setSlot(inventory, 7, "block", 99, revision, tick);
        setSlot(inventory, 8, "block", 99, revision, tick);
        setSlot(inventory, 9, "torch", 10, revision, tick);
        setSlot(inventory, 10, "seeds_carrot", 16, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET, "hoe", 1, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET + 1, "watering_can", 1, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET + 2, "seeds_wheat", 16, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET + 3, "fence", 64, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET + 4, "chest", 3, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET + 5, "barrel", 4, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET + 6, "boat", 3, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET + 7, "torch", 10, revision, tick);
        setSlot(inventory, PLAYER_HOTBAR_OFFSET + 8, "sword", 1, revision, tick);
    }

    private void setSlot(InventoryState inventory, int slot, String itemType, int amount, long revision, long tick) {
        inventory.setSlot(slot, new ItemStackState(itemType, amount), revision, tick);
    }

    private boolean canAccessInventory(Session session, InventoryState inventory) {
        if (session == null || inventory == null) return false;
        String type = inventory.inventoryType();
        if (playerInventoryType(session.playerId).equals(type)) return true;
        if (cursorInventoryType(session.playerId).equals(type)) return true;
        EntityState owner = world.entity(inventory.ownerEntityId());
        return owner != null && !owner.removed()
            && withinRange(session.x, session.y, owner.worldX(), owner.worldY(), maxActionRange);
    }

    private boolean leftClickSlot(InventoryState inventory, InventoryState cursor, int slot, long revision, long tick) {
        ItemStackState hand = cursor.slot(0);
        ItemStackState mine = inventory.slot(slot);
        if (hand.isEmpty() && mine.isEmpty()) return false;
        if (hand.isEmpty()) {
            inventory.setSlot(slot, ItemStackState.EMPTY, revision, tick);
            cursor.setSlot(0, mine, revision, tick);
            return true;
        }
        if (mine.isEmpty()) {
            inventory.setSlot(slot, hand, revision, tick);
            cursor.setSlot(0, ItemStackState.EMPTY, revision, tick);
            return true;
        }
        if (hand.itemType().equals(mine.itemType())) {
            int room = Math.max(0, MAX_STACK - mine.amount());
            if (room <= 0) return false;
            int moved = Math.min(room, hand.amount());
            inventory.setSlot(slot, mine.withAmount(mine.amount() + moved), revision, tick);
            cursor.setSlot(0, hand.withAmount(hand.amount() - moved), revision, tick);
            return true;
        }
        inventory.setSlot(slot, hand, revision, tick);
        cursor.setSlot(0, mine, revision, tick);
        return true;
    }

    private boolean rightClickSlot(InventoryState inventory, InventoryState cursor, int slot, long revision, long tick) {
        ItemStackState hand = cursor.slot(0);
        ItemStackState mine = inventory.slot(slot);
        if (hand.isEmpty() && mine.isEmpty()) return false;
        if (hand.isEmpty()) {
            int take = Math.max(1, (mine.amount() + 1) / 2);
            cursor.setSlot(0, mine.withAmount(take), revision, tick);
            inventory.setSlot(slot, mine.withAmount(mine.amount() - take), revision, tick);
            return true;
        }
        if (mine.isEmpty()) {
            inventory.setSlot(slot, hand.withAmount(1), revision, tick);
            cursor.setSlot(0, hand.withAmount(hand.amount() - 1), revision, tick);
            return true;
        }
        if (!hand.itemType().equals(mine.itemType()) || mine.amount() >= MAX_STACK) return false;
        inventory.setSlot(slot, mine.withAmount(mine.amount() + 1), revision, tick);
        cursor.setSlot(0, hand.withAmount(hand.amount() - 1), revision, tick);
        return true;
    }

    private void decrementSlot(InventoryState inventory, int slot, int amount, long revision, long tick) {
        ItemStackState stack = inventory.slot(slot);
        if (stack.isEmpty()) return;
        inventory.setSlot(slot, stack.withAmount(stack.amount() - Math.max(1, amount)), revision, tick);
    }

    private int containerSlots(String type) {
        if ("chest".equals(type)) return 27;
        if ("barrel".equals(type)) return 9;
        if ("crafting_table".equals(type)) return 17;
        return 0;
    }

    private void integrateRidingPlayer(
            Session session,
            PlayerReplicaState player,
            AuthorityService authority,
            double moveSpeedPerTick,
            long tick) {
        EntityState boat = world.entity(player.ridingEntityId());
        if (boat == null || boat.removed() || !"boat".equals(boat.entityType())) {
            player.setRidingEntityId(0L, tick);
            syncSessionToPlayer(session, player);
            return;
        }

        double dx = 0.0;
        double dy = 0.0;
        if (player.up()) dy -= moveSpeedPerTick;
        if (player.down()) dy += moveSpeedPerTick;
        if (player.left()) dx -= moveSpeedPerTick;
        if (player.right()) dx += moveSpeedPerTick;
        if (dx != 0.0 && dy != 0.0) { dx *= DIAGONAL; dy *= DIAGONAL; }
        if (authority != null && !authority.canMove(dx, dy, moveSpeedPerTick)) return;

        double oldX = boat.worldX();
        double oldY = boat.worldY();
        double nextX = oldX + dx;
        double nextY = oldY + dy;
        boolean moved = false;
        if (canBoatOccupy(nextX, nextY)) {
            boat.moveTo(boat.dimensionId(), nextX, nextY, world.bumpRevision(), tick);
            moved = true;
        } else if (dx != 0.0 && canBoatOccupy(nextX, oldY)) {
            boat.moveTo(boat.dimensionId(), nextX, oldY, world.bumpRevision(), tick);
            moved = true;
        } else if (dy != 0.0 && canBoatOccupy(oldX, nextY)) {
            boat.moveTo(boat.dimensionId(), oldX, nextY, world.bumpRevision(), tick);
            moved = true;
        }
        movePlayerToBoat(session, player, boat, tick);
        if (moved) dirty = true;
    }

    private boolean canBoatOccupy(double centerX, double centerY) {
        return terrainRules == null || terrainRules.canPlaceObject("boat", centerX, centerY);
    }

    private void movePlayerToBoat(Session session, PlayerReplicaState player, EntityState boat, long tick) {
        player.moveTo(boat.dimensionId(),
            boat.worldX() + PLAYER_RIDER_OFFSET_X,
            boat.worldY() + PLAYER_RIDER_OFFSET_Y,
            boat.worldX() - session.x,
            boat.worldY() - session.y,
            tick);
        syncSessionToPlayer(session, player);
    }

    private void syncSessionToPlayer(Session session, PlayerReplicaState player) {
        session.x = player.worldX();
        session.y = player.worldY();
        session.vx = player.velocityX();
        session.vy = player.velocityY();
        session.lastChangedTick = Math.max(session.lastChangedTick, player.lastChangedTick());
    }

    private MoveResult moveWithTerrainSteps(PlayerReplicaState player, double dx, double dy, long tick) {
        if (dx == 0.0 && dy == 0.0) return MoveResult.NONE;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / MAX_MOVE_SUBSTEP));
        double stepX = dx / steps;
        double stepY = dy / steps;
        double movedX = 0.0;
        double movedY = 0.0;
        for (int i = 0; i < steps; i++) {
            double targetX = player.worldX() + stepX;
            double targetY = player.worldY() + stepY;
            if (canPlayerOccupy(player, targetX, targetY)) {
                player.moveTo(player.dimensionId(), targetX, targetY, movedX + stepX, movedY + stepY, tick);
                movedX += stepX;
                movedY += stepY;
                continue;
            }
            boolean progressed = false;
            if (stepX != 0.0 && canPlayerOccupy(player, player.worldX() + stepX, player.worldY())) {
                player.moveTo(player.dimensionId(), player.worldX() + stepX, player.worldY(), movedX + stepX, movedY, tick);
                movedX += stepX;
                progressed = true;
            }
            if (stepY != 0.0 && canPlayerOccupy(player, player.worldX(), player.worldY() + stepY)) {
                player.moveTo(player.dimensionId(), player.worldX(), player.worldY() + stepY, movedX, movedY + stepY, tick);
                movedY += stepY;
                progressed = true;
            }
            if (!progressed) break;
        }
        return new MoveResult(movedX, movedY);
    }

    private boolean canPlayerOccupy(PlayerReplicaState player, double x, double y) {
        if (player != null && player.ridingEntityId() > 0L) return true;
        return terrainRules == null || terrainRules.canPlayerOccupy(x, y);
    }

    private void ensureOnLand(PlayerReplicaState player, long tick) {
        if (player == null || terrainRules == null || canPlayerOccupy(player, player.worldX(), player.worldY())) return;
        double[] spawn = nearestLand(player.worldX(), player.worldY());
        player.moveTo(player.dimensionId(), spawn[0], spawn[1], 0.0, 0.0, tick);
    }

    private double[] nearestLand(double aroundX, double aroundY) {
        int baseX = (int) Math.rint(aroundX / TILE_SIZE) * TILE_SIZE;
        int baseY = (int) Math.rint(aroundY / TILE_SIZE) * TILE_SIZE;
        for (int radius = 0; radius <= 48; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dy) < radius) continue;
                    double x = baseX + (dx * TILE_SIZE);
                    double y = baseY + (dy * TILE_SIZE);
                    if (terrainRules == null || terrainRules.canPlayerOccupy(x, y)) return new double[] { x, y };
                }
            }
        }
        return new double[] { aroundX, aroundY };
    }

    private EntityState nearestEntity(double x, double y, double maxRange) {
        EntityState nearest = null;
        double bestDist2 = maxRange * maxRange;
        for (EntityState entity : world.entities()) {
            if (entity.removed()) continue;
            double dx = entity.worldX() - x;
            double dy = entity.worldY() - y;
            double dist2 = (dx * dx) + (dy * dy);
            if (dist2 > bestDist2) continue;
            bestDist2 = dist2;
            nearest = entity;
        }
        return nearest;
    }

    private boolean placementBlocked(double x, double y) {
        double minDist2 = MIN_ENTITY_GAP * MIN_ENTITY_GAP;
        for (EntityState entity : world.entities()) {
            if (entity.removed()) continue;
            double dx = entity.worldX() - x;
            double dy = entity.worldY() - y;
            if (((dx * dx) + (dy * dy)) <= minDist2) return true;
        }
        return false;
    }

    private boolean isInterested(Session recipient, Session candidate, double interestRadius) {
        if (recipient.playerId.equals(candidate.playerId)) return true;
        double dx = candidate.x - recipient.x;
        double dy = candidate.y - recipient.y;
        return ((dx * dx) + (dy * dy)) <= (interestRadius * interestRadius);
    }

    private ArrayList<ProtocolPayloads.WorldObjectState> compatibilityObjects(boolean baseline, long sentTick) {
        ArrayList<ProtocolPayloads.WorldObjectState> out = new ArrayList<>();
        for (EntityState entity : world.entities()) {
            if (!baseline && !entity.changedSince(sentTick)) continue;
            if (baseline && entity.removed()) continue;
            out.add(new ProtocolPayloads.WorldObjectState(
                entity.entityId(), entity.entityType(), entity.worldX(), entity.worldY(), entity.removed(), entity.revision()));
        }
        return out;
    }

    private ArrayList<ProtocolPayloads.EntityStatePayload> entityPayloads(boolean baseline, long sentTick) {
        ArrayList<ProtocolPayloads.EntityStatePayload> out = new ArrayList<>();
        for (EntityState entity : world.entities()) {
            if (!baseline && !entity.changedSince(sentTick)) continue;
            if (baseline && entity.removed()) continue;
            ArrayList<ProtocolPayloads.ComponentStatePayload> components = new ArrayList<>();
            for (Map.Entry<String, String> entry : entity.components().entrySet()) {
                components.add(new ProtocolPayloads.ComponentStatePayload(entry.getKey(), entry.getValue()));
            }
            out.add(new ProtocolPayloads.EntityStatePayload(
                entity.entityId(), entity.entityType(), entity.dimensionId(), entity.worldX(), entity.worldY(),
                entity.removed(), entity.revision(), components));
        }
        return out;
    }

    private ArrayList<ProtocolPayloads.InventoryStatePayload> inventoryPayloads(boolean baseline, long sentTick) {
        ArrayList<ProtocolPayloads.InventoryStatePayload> out = new ArrayList<>();
        for (InventoryState inventory : world.inventories()) {
            if (!baseline && !inventory.changedSince(sentTick)) continue;
            ArrayList<ProtocolPayloads.ItemStackPayload> slots = new ArrayList<>();
            List<ItemStackState> source = inventory.slots();
            for (ItemStackState stack : source) {
                slots.add(new ProtocolPayloads.ItemStackPayload(stack.itemType(), stack.amount()));
            }
            out.add(new ProtocolPayloads.InventoryStatePayload(
                inventory.inventoryId(), inventory.ownerEntityId(), inventory.inventoryType(), inventory.revision(), slots));
        }
        return out;
    }

    private ArrayList<ProtocolPayloads.TileMutationPayload> tilePayloads(boolean baseline, long sentTick) {
        ArrayList<ProtocolPayloads.TileMutationPayload> out = new ArrayList<>();
        for (TileMutationState tile : world.tileMutations()) {
            if (!baseline && !tile.changedSince(sentTick)) continue;
            out.add(new ProtocolPayloads.TileMutationPayload(
                tile.dimensionId(), tile.tileX(), tile.tileY(), tile.tileType(), tile.watered(),
                tile.cropType(), tile.cropStage(), tile.revision()));
        }
        return out;
    }

    private boolean isTilling(String type) {
        return "hoe".equals(type) || "farmland".equals(type);
    }

    private boolean isWatering(String type) {
        return "watering_can".equals(type);
    }

    private boolean isSeed(String type) {
        return type.startsWith("seeds_") || type.startsWith("crop_");
    }

    private String cropTypeForSeed(String type) {
        if (type.startsWith("seeds_")) return "crop_" + type.substring("seeds_".length());
        return type;
    }

    private boolean isServerUsableItem(String type) {
        return isTilling(type)
            || isWatering(type)
            || isSeed(type)
            || "boat".equals(type)
            || "fence".equals(type)
            || "torch".equals(type)
            || "barrel".equals(type)
            || "chest".equals(type)
            || "crafting_table".equals(type)
            || "demohouse".equals(type)
            || "block".equals(type);
    }

    private boolean consumesOnUse(String type) {
        return !isTilling(type) && !isWatering(type);
    }

    private double[] placementCenter(String type, double x, double y) {
        if (isTileSnappedPlacement(type)) {
            int tx = tileCoord(x);
            int ty = tileCoord(y);
            return new double[] { tx * TILE_SIZE + TILE_SIZE / 2.0, ty * TILE_SIZE + TILE_SIZE / 2.0 };
        }
        return new double[] { x, y };
    }

    private boolean isTileSnappedPlacement(String type) {
        return "fence".equals(type)
            || "barrel".equals(type)
            || "chest".equals(type)
            || "crafting_table".equals(type)
            || "block".equals(type);
    }

    private void markSequence(Session session, long sequence, long tick) {
        session.lastAcceptedSeq = Math.max(session.lastAcceptedSeq, sequence);
        session.lastChangedTick = Math.max(session.lastChangedTick, tick);
    }

    private boolean withinRange(double fromX, double fromY, double toX, double toY, double maxRange) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return ((dx * dx) + (dy * dy)) <= (maxRange * maxRange);
    }

    private double[] nearestDismountLand(double boatCenterX, double boatCenterY) {
        if (terrainRules == null) return new double[] { boatCenterX, boatCenterY };
        int baseX = (int) Math.rint(boatCenterX / TILE_SIZE) * TILE_SIZE;
        int baseY = (int) Math.rint(boatCenterY / TILE_SIZE) * TILE_SIZE;
        for (int radius = 1; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                    double x = baseX + dx * TILE_SIZE;
                    double y = baseY + dy * TILE_SIZE;
                    if (terrainRules.canPlayerOccupy(x, y)) return new double[] { x, y };
                }
            }
        }
        return null;
    }

    private int tileCoord(double pixel) {
        return Math.floorDiv((int) Math.floor(pixel), TILE_SIZE);
    }

    private long chunkKeyForWorld(String dimensionId, double worldX, double worldY) {
        int chunkX = Math.floorDiv((int) Math.floor(worldX), TILE_SIZE * 16);
        int chunkY = Math.floorDiv((int) Math.floor(worldY), TILE_SIZE * 16);
        return chunkKeyForTile(dimensionId, chunkX * 16, chunkY * 16);
    }

    private long chunkKeyForTile(String dimensionId, int tileX, int tileY) {
        int chunkX = Math.floorDiv(tileX, 16);
        int chunkY = Math.floorDiv(tileY, 16);
        long dim = dimensionOrdinal(dimensionId) & 0xFFL;
        long x = chunkX & 0x0FFFFFFFL;
        long y = chunkY & 0x0FFFFFFFL;
        return (dim << 56) | (x << 28) | y;
    }

    private long dimensionOrdinal(String dimensionId) {
        String dim = TileMutationState.normalizeDimension(dimensionId);
        if ("core:cave".equals(dim)) return 2L;
        if ("core:interior".equals(dim)) return 3L;
        return 1L;
    }

    private String sanitizeObjectType(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase();
        if (value.isBlank() || "empty".equals(value)) return "";
        if (value.length() > 96) value = value.substring(0, 96);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == ':';
            if (!ok) return "";
        }
        return value;
    }

    private static final class MoveResult {
        static final MoveResult NONE = new MoveResult(0.0, 0.0);
        final double movedX;
        final double movedY;

        MoveResult(double movedX, double movedY) {
            this.movedX = movedX;
            this.movedY = movedY;
        }
    }

    static final class CommandOutcome {
        final boolean accepted;
        final String reason;

        private CommandOutcome(boolean accepted, String reason) {
            this.accepted = accepted;
            this.reason = reason == null ? "" : reason;
        }

        static CommandOutcome accept() {
            return new CommandOutcome(true, "");
        }

        static CommandOutcome reject(String reason) {
            return new CommandOutcome(false, reason);
        }
    }

    private static final class ChunkPayloadBuilder {
        final ArrayList<ProtocolPayloads.WorldObjectState> compatibilityObjects = new ArrayList<>();
        final ArrayList<ProtocolPayloads.EntityStatePayload> entities = new ArrayList<>();
        final ArrayList<ProtocolPayloads.InventoryStatePayload> inventories = new ArrayList<>();
        final ArrayList<ProtocolPayloads.TileMutationPayload> tiles = new ArrayList<>();
    }
}
