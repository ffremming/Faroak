package resources.net.multiplayer.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import resources.domain.farming.CropRegistry;
import resources.domain.combat.WeaponProfile;
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
    private static final int CRAFTING_INPUT_SLOTS = 16;
    private static final int CRAFTING_OUTPUT_SLOT = 16;
    private static final double PLAYER_RIDER_OFFSET_X = -24.0;
    private static final double PLAYER_RIDER_OFFSET_Y = -64.0;
    private static final int CROP_MATURE_STAGE = 3;
    private static final long CROP_STAGE_TICKS = 150L;
    private static final int CROP_HARVEST_AMOUNT = 1;
    private static final double PLAYER_CENTER_OFFSET_X = 24.0;
    private static final double PLAYER_CENTER_OFFSET_Y = 48.0;
    private static final int PROJECTILE_SIZE = 28;
    private static final double PROJECTILE_HIT_RADIUS = 20.0;
    private static final long MOB_AI_TICK_INTERVAL = 2L;
    private static final double MOB_DETECTION_RANGE = 640.0;

    private final WorldState world = new WorldState();
    private final ServerTerrainRules terrainRules;
    private final double maxActionRange;
    private long respawnDelayTicks = 150L; // ~5s at 30Hz; overridden by the lobby
    private boolean pvpEnabled = true;
    private boolean dirty;

    AuthoritativeGameHost(ServerTerrainRules terrainRules, double maxActionRange) {
        this.terrainRules = terrainRules;
        this.maxActionRange = Math.max(1.0, maxActionRange);
    }

    void setRespawnDelayTicks(long ticks) { this.respawnDelayTicks = Math.max(1L, ticks); }
    void setPvpEnabled(boolean enabled) { this.pvpEnabled = enabled; }

    /** Live sessions map (owned by the lobby) so combat can target players for PvP. */
    private Map<String, Session> activeSessions = java.util.Collections.emptyMap();
    void setActiveSessions(Map<String, Session> sessions) {
        this.activeSessions = (sessions == null) ? java.util.Collections.emptyMap() : sessions;
    }

    WorldState world() { return world; }
    boolean dirty() { return dirty; }
    void clearDirty() { dirty = false; }

    /**
     * Add an authoritative harvestable world object (tree/rock/ore) at a position.
     * Gets a health component so it can be attacked and drops loot via {@link #lootFor}.
     * Used by {@link ServerWorldPopulator} at fresh-world seeding.
     */
    long seedHarvestable(String type, double x, double y, long tick) {
        long revision = world.bumpRevision();
        EntityState entity = new EntityState(world.allocateEntityId(), type, DEFAULT_DIMENSION, x, y, revision, tick);
        int max = defaultMaxHealth(type);
        entity.putComponent("dimension", DEFAULT_DIMENSION, revision, tick);
        entity.putComponent("health", max + "/" + max, revision, tick);
        entity.putComponent("max_health", Integer.toString(max), revision, tick);
        entity.putComponent("harvestable", type, revision, tick);
        world.putEntity(entity);
        dirty = true;
        return entity.entityId();
    }

    /** Spawn an authoritative mob (goblin/spider/deer) with full combat components. */
    long spawnMob(String type, double x, double y, long tick) {
        long revision = world.bumpRevision();
        EntityState entity = new EntityState(world.allocateEntityId(), type, DEFAULT_DIMENSION, x, y, revision, tick);
        int max = defaultMaxHealth(type);
        entity.putComponent("dimension", DEFAULT_DIMENSION, revision, tick);
        entity.putComponent("health", max + "/" + max, revision, tick);
        entity.putComponent("max_health", Integer.toString(max), revision, tick);
        entity.putComponent("mob", type, revision, tick);
        entity.putComponent("attack_cooldown", "0", revision, tick);
        world.putEntity(entity);
        dirty = true;
        return entity.entityId();
    }

    /** Live (non-removed) mob count, for the spawner's population cap. */
    int mobCount() {
        int n = 0;
        for (EntityState e : world.entities()) {
            if (!e.removed() && isMob(e)) n++;
        }
        return n;
    }

    /** Count live entities of a given type — for probes/spawner bookkeeping. */
    int entityCountOfType(String type) {
        int n = 0;
        for (EntityState e : world.entities()) {
            if (!e.removed() && e.entityType().equals(type)) n++;
        }
        return n;
    }

    /** Remove mobs with no session within {@code despawnRadius} — for the spawner. */
    void despawnDistantMobs(Map<String, Session> sessions, double despawnRadius, long tick) {
        double r2 = despawnRadius * despawnRadius;
        for (EntityState mob : world.entities()) {
            if (mob.removed() || !isMob(mob)) continue;
            boolean near = false;
            for (Session s : sessions.values()) {
                double dx = playerCenterX(s) - mob.worldX();
                double dy = playerCenterY(s) - mob.worldY();
                if (dx * dx + dy * dy <= r2) { near = true; break; }
            }
            if (!near) {
                mob.markRemoved(world.bumpRevision(), tick);
                dirty = true;
            }
        }
    }

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
            if (!session.alive) {
                // Dead players freeze in place until respawned.
                player.setInput(false, false, false, false, session.lastAcceptedSeq, tick);
                continue;
            }
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

    void tickWorld(Map<String, Session> sessions, long tick) {
        world.setTick(tick);
        advanceCrops(tick);
        tickProjectiles(tick);
        if (tick % MOB_AI_TICK_INTERVAL == 0L) tickMobs(sessions, tick);
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
        if (isCombatCommand(command.commandType)) {
            CommandOutcome harvest = harvestCrop(session, command.targetX, command.targetY, "", tick, events);
            if (harvest.accepted && !ProtocolPayloads.CommandRequest.ATTACK_RANGED_AT.equals(command.commandType)) {
                markSequence(session, sequence, tick);
                return CommandOutcome.accept();
            }
            CommandOutcome combat = applyCombatCommand(session, command, sequence, tick, events);
            if (!combat.accepted) return combat;
            markSequence(session, sequence, tick);
            return CommandOutcome.accept();
        }
        if (ProtocolPayloads.CommandRequest.INVENTORY_CLICK.equals(command.commandType)) {
            return applyInventoryClick(session, command, sequence, tick, events);
        }
        if (ProtocolPayloads.CommandRequest.RESPAWN.equals(command.commandType)) {
            if (!session.alive) session.respawnRequested = true;
            markSequence(session, sequence, tick);
            return CommandOutcome.accept();
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
            if (isTransientEntity(entity)) continue;
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

    private CommandOutcome applyCombatCommand(
            Session session,
            ProtocolPayloads.CommandRequest command,
            long sequence,
            long tick,
            ActionResolver.EventSink events) {
        if (session == null || command == null || !command.hasTarget) return CommandOutcome.reject("missing target");
        if (!withinRange(playerCenterX(session), playerCenterY(session),
                command.targetX, command.targetY, maxActionRange * 2.0)) {
            return CommandOutcome.reject("too far away");
        }
        String weaponType = selectedWeaponType(session, command);
        WeaponProfile weapon = WeaponProfile.forItem(weaponType);
        if (ProtocolPayloads.CommandRequest.ATTACK_RANGED_AT.equals(command.commandType)) {
            return fireCombatProjectile(session, command, weapon, weaponType, tick, events);
        }
        return applyMeleeCombat(session, command, weapon, weaponType, tick, events);
    }

    private CommandOutcome applyMeleeCombat(
            Session session,
            ProtocolPayloads.CommandRequest command,
            WeaponProfile weapon,
            String weaponType,
            long tick,
            ActionResolver.EventSink events) {
        boolean heavy = ProtocolPayloads.CommandRequest.ATTACK_HEAVY_AT.equals(command.commandType);
        int damage = heavy ? weapon.heavyDamage : weapon.lightDamage;
        int range = heavy ? weapon.heavyRangePx : weapon.lightRangePx;
        double arc = heavy ? weapon.heavyArcDegrees : weapon.lightArcDegrees;
        EntityState target = targetInArc(session, command.targetX, command.targetY, range, arc);
        if (target == null) {
            // No entity in range — try a player (PvP).
            Session victim = pvpTargetNear(session, command.targetX, command.targetY, range);
            if (victim != null) {
                damageSession(victim, damage, tick);
                if (events != null) {
                    events.event(session.playerId, "combat", "pvp:" + victim.playerId + ":" + victim.health);
                }
                dirty = true;
                return CommandOutcome.accept();
            }
            if (events != null) events.event(session.playerId, "combat", "melee:none");
            return CommandOutcome.reject("nothing to attack");
        }
        DamageResult result = applyDamageToEntity(session.playerId, target, damage, tick, events);
        if (!result.hit) return CommandOutcome.reject("target cannot be damaged");
        if (events != null) {
            events.event(session.playerId, "combat",
                "melee:" + weaponType + ":" + target.entityId() + ":" + result.currentHealth);
        }
        dirty = true;
        return CommandOutcome.accept();
    }

    private CommandOutcome fireCombatProjectile(
            Session session,
            ProtocolPayloads.CommandRequest command,
            WeaponProfile weapon,
            String weaponType,
            long tick,
            ActionResolver.EventSink events) {
        double sx = playerCenterX(session);
        double sy = playerCenterY(session);
        double[] dir = normalizedDirection(sx, sy, command.targetX, command.targetY, session);
        long revision = world.bumpRevision();
        EntityState projectile = new EntityState(
            world.allocateEntityId(), "combat_bolt", DEFAULT_DIMENSION, sx, sy, revision, tick);
        projectile.putComponent("projectile", "true", revision, tick);
        projectile.putComponent("owner", session.playerId, revision, tick);
        projectile.putComponent("weapon", weaponType == null ? "" : weaponType, revision, tick);
        projectile.putComponent("damage", Integer.toString(Math.max(1, weapon.rangedDamage)), revision, tick);
        projectile.putComponent("vx", Double.toString(dir[0] * Math.max(1.0, weapon.projectileSpeedPxPerTick)), revision, tick);
        projectile.putComponent("vy", Double.toString(dir[1] * Math.max(1.0, weapon.projectileSpeedPxPerTick)), revision, tick);
        projectile.putComponent("life", Integer.toString(Math.max(1, weapon.projectileLifeTicks)), revision, tick);
        projectile.putComponent("age", "0", revision, tick);
        projectile.putComponent("radius", Double.toString(PROJECTILE_HIT_RADIUS), revision, tick);
        projectile.putComponent("transient", "true", revision, tick);
        world.putEntity(projectile);
        dirty = true;
        if (events != null) events.event(session.playerId, "combat", "projectile:" + projectile.entityId());
        return CommandOutcome.accept();
    }

    private void tickProjectiles(long tick) {
        ArrayList<EntityState> projectiles = new ArrayList<>();
        for (EntityState entity : world.entities()) {
            if (entity.removed()) continue;
            if (isProjectile(entity)) projectiles.add(entity);
        }
        for (EntityState projectile : projectiles) {
            int age = parseInt(projectile.component("age"), 0) + 1;
            int life = parseInt(projectile.component("life"), 1);
            if (age > life) {
                projectile.markRemoved(world.bumpRevision(), tick);
                dirty = true;
                continue;
            }
            double vx = parseDouble(projectile.component("vx"), 0.0);
            double vy = parseDouble(projectile.component("vy"), 0.0);
            double nextX = projectile.worldX() + vx;
            double nextY = projectile.worldY() + vy;
            long revision = world.bumpRevision();
            projectile.moveTo(projectile.dimensionId(), nextX, nextY, revision, tick);
            projectile.putComponent("age", Integer.toString(age), revision, tick);

            EntityState target = projectileImpactTarget(projectile);
            if (target != null) {
                String owner = projectile.component("owner");
                int damage = parseInt(projectile.component("damage"), 1);
                applyDamageToEntity(owner, target, damage, tick, null);
                projectile.markRemoved(world.bumpRevision(), tick);
            }
            dirty = true;
        }
    }

    private EntityState projectileImpactTarget(EntityState projectile) {
        double radius = parseDouble(projectile.component("radius"), PROJECTILE_HIT_RADIUS);
        EntityState nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (EntityState target : world.entities()) {
            if (target == null || target.removed() || target == projectile) continue;
            if (isProjectile(target) || !isDamageable(target)) continue;
            double max = radius + entityRadius(target);
            double dx = target.worldX() - projectile.worldX();
            double dy = target.worldY() - projectile.worldY();
            double dist2 = dx * dx + dy * dy;
            if (dist2 > max * max || dist2 >= best) continue;
            best = dist2;
            nearest = target;
        }
        return nearest;
    }

    private void tickMobs(Map<String, Session> sessions, long tick) {
        for (EntityState mob : world.entities()) {
            if (mob.removed() || !isMob(mob)) continue;
            if (!isDamageable(mob)) continue;
            Session target = nearestSession(mob, sessions);
            if (target == null) continue;
            double tx = playerCenterX(target);
            double ty = playerCenterY(target);
            double dx = tx - mob.worldX();
            double dy = ty - mob.worldY();
            double dist = Math.hypot(dx, dy);
            if (dist > MOB_DETECTION_RANGE || dist < 0.0001) continue;

            int cooldown = Math.max(0, parseInt(mob.component("attack_cooldown"), 0) - (int) MOB_AI_TICK_INTERVAL);
            long revision = world.bumpRevision();
            mob.putComponent("attack_cooldown", Integer.toString(cooldown), revision, tick);

            int meleeRange = mobAttackRange(mob.entityType());
            if (dist <= meleeRange && cooldown <= 0) {
                mob.putComponent("attack_cooldown", Integer.toString(mobAttackCooldown(mob.entityType())), world.bumpRevision(), tick);
                damageSession(target, mobDamage(mob.entityType()), tick);
                mob.putComponent("last_hit_player", target.playerId, world.bumpRevision(), tick);
                dirty = true;
                continue;
            }

            double speed = mobSpeed(mob.entityType()) * MOB_AI_TICK_INTERVAL;
            double nx = mob.worldX() + (dx / dist) * speed;
            double ny = mob.worldY() + (dy / dist) * speed;
            if (terrainRules == null || !terrainRules.isWaterAt(nx, ny)) {
                mob.moveTo(mob.dimensionId(), nx, ny, world.bumpRevision(), tick);
                dirty = true;
            }
        }
    }

    private DamageResult applyDamageToEntity(
            String sourcePlayerId,
            EntityState target,
            int damage,
            long tick,
            ActionResolver.EventSink events) {
        if (target == null || target.removed() || damage <= 0 || !isDamageable(target)) return DamageResult.NONE;
        int maxHealth = maxHealth(target);
        int health = currentHealth(target, maxHealth);
        int next = Math.max(0, health - damage);
        long revision = world.bumpRevision();
        target.putComponent("health", next + "/" + maxHealth, revision, tick);
        target.putComponent("last_damage", Integer.toString(damage), revision, tick);
        if (sourcePlayerId != null) target.putComponent("last_attacker", sourcePlayerId, revision, tick);
        if (next <= 0) {
            target.markRemoved(world.bumpRevision(), tick);
            awardDeathLoot(sourcePlayerId, target, tick);
            if (events != null) events.event(sourcePlayerId, "combat", "death:" + target.entityId());
        }
        dirty = true;
        return new DamageResult(true, next, maxHealth, next <= 0);
    }

    /** Nearest living OTHER player whose center is within {@code range} of the
     *  attack point, when PvP is enabled. Null otherwise. */
    private Session pvpTargetNear(Session attacker, double targetX, double targetY, int range) {
        if (!pvpEnabled) return null;
        Session best = null;
        double bestDist2 = (double) range * range;
        for (Session s : activeSessions.values()) {
            if (s == attacker || !s.alive) continue;
            double dx = playerCenterX(s) - targetX;
            double dy = playerCenterY(s) - targetY;
            double d2 = dx * dx + dy * dy;
            if (d2 <= bestDist2) { bestDist2 = d2; best = s; }
        }
        return best;
    }

    private void damageSession(Session session, int damage, long tick) {
        if (session == null || damage <= 0 || !session.alive || session.health <= 0) return;
        session.health = Math.max(0, session.health - damage);
        session.lastChangedTick = Math.max(session.lastChangedTick, tick);
        if (session.health <= 0) {
            session.alive = false;
            session.respawnAtTick = tick + respawnDelayTicks;
            session.up = session.left = session.down = session.right = false;
            session.vx = 0.0; session.vy = 0.0;
        }
        dirty = true;
    }

    /**
     * Advance player death/respawn each tick: respawn dead players whose timer has
     * elapsed or who requested it, restoring them to full health at a valid spawn.
     */
    void tickPlayerLifecycle(Map<String, Session> sessions, long tick) {
        for (Session s : sessions.values()) {
            if (s.alive) continue;
            if (s.respawnRequested || tick >= s.respawnAtTick) {
                respawnSession(s, tick);
            }
        }
    }

    private void respawnSession(Session session, long tick) {
        double[] spawn = nearestLand(0.0, 0.0);
        session.x = spawn[0];
        session.y = spawn[1];
        session.vx = 0.0; session.vy = 0.0;
        session.up = session.left = session.down = session.right = false;
        session.health = session.maxHealth;
        session.alive = true;
        session.respawnRequested = false;
        session.respawnAtTick = 0L;
        session.lastChangedTick = Math.max(session.lastChangedTick, tick);
        PlayerReplicaState player = world.player(session.playerId);
        if (player != null) player.moveTo(DEFAULT_DIMENSION, session.x, session.y, 0.0, 0.0, tick);
        dirty = true;
    }

    private void awardDeathLoot(String playerId, EntityState target, long tick) {
        if (playerId == null || playerId.isBlank() || target == null) return;
        InventoryState inventory = playerInventory(playerId);
        if (inventory == null) return;
        for (ItemStackState loot : lootFor(target.entityType())) {
            if (loot == null || loot.isEmpty()) continue;
            if (canFitStack(inventory, loot.itemType(), loot.amount())) {
                addStackToInventory(inventory, loot.itemType(), loot.amount(), world.bumpRevision(), tick);
            }
        }
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
        if (selected.isEmpty()) {
            CommandOutcome harvest = harvestCrop(session, command.targetX, command.targetY, "", tick, events);
            if (harvest.accepted) {
                markSequence(session, sequence, tick);
                return CommandOutcome.accept();
            }
            return CommandOutcome.reject("empty hand");
        }
        if (!command.itemType.isBlank() && !command.itemType.equals(selected.itemType())) {
            return CommandOutcome.reject("selected item changed");
        }
        String type = selected.itemType();
        CommandOutcome harvest = harvestCrop(session, command.targetX, command.targetY, type, tick, events);
        if (harvest.accepted) {
            markSequence(session, sequence, tick);
            return CommandOutcome.accept();
        }
        if (!isServerUsableItem(type)) {
            ProtocolPayloads.CommandRequest attack = new ProtocolPayloads.CommandRequest(
                ProtocolPayloads.CommandRequest.ATTACK_LIGHT_AT,
                true, command.targetX, command.targetY, 0L, type, slotIndex, 0L, -1, 0);
            return applyCombatCommand(session, attack, sequence, tick, events);
        }
        boolean applied = applyPlaceType(session, type, command.targetX, command.targetY, tick, events);
        if (!applied) {
            harvest = harvestCrop(session, command.targetX, command.targetY, type, tick, events);
            if (harvest.accepted) {
                markSequence(session, sequence, tick);
                return CommandOutcome.accept();
            }
            return CommandOutcome.reject("cannot use item here");
        }
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
        if ("crafting_table".equals(target.inventoryType())) {
            return applyCraftingTableClick(session, command, sequence, tick, events, target, cursor);
        }

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

    private void advanceCrops(long tick) {
        for (TileMutationState tile : world.tileMutations()) {
            if (tile == null || tile.cropType().isBlank()) continue;
            if (!tile.watered()) continue;
            if (tile.cropStage() >= CROP_MATURE_STAGE) continue;
            if ((tick - tile.lastChangedTick()) < CROP_STAGE_TICKS) continue;
            tile.advanceCrop(tile.cropStage() + 1, world.bumpRevision(), tick);
            dirty = true;
        }
    }

    private CommandOutcome harvestCrop(
            Session session,
            double x,
            double y,
            String toolType,
            long tick,
            ActionResolver.EventSink events) {
        int tx = tileCoord(x);
        int ty = tileCoord(y);
        TileMutationState tile = world.tileMutation(DEFAULT_DIMENSION, tx, ty);
        if (tile == null || tile.cropType().isBlank()) return CommandOutcome.reject("no crop here");
        if (tile.cropStage() < CROP_MATURE_STAGE) return CommandOutcome.reject("crop is not mature");

        CropRegistry.Entry crop = CropRegistry.get(tile.cropType());
        String requiredTool = crop == null ? null : crop.requiredTool;
        if (requiredTool != null && !requiredTool.equals(toolType)) {
            return CommandOutcome.reject("wrong harvest tool");
        }
        String produce = crop == null ? produceForCrop(tile.cropType()) : crop.produceName;
        InventoryState inventory = playerInventory(session.playerId);
        if (inventory == null) return CommandOutcome.reject("missing inventory");
        if (!canFitStack(inventory, produce, CROP_HARVEST_AMOUNT)) {
            return CommandOutcome.reject("inventory full");
        }

        long revision = world.bumpRevision();
        addStackToInventory(inventory, produce, CROP_HARVEST_AMOUNT, revision, tick);
        tile.clearCrop(revision, tick);
        dirty = true;
        if (events != null) events.event(session.playerId, "action", "harvest:" + produce + ":" + tx + "," + ty);
        return CommandOutcome.accept();
    }

    private String produceForCrop(String cropType) {
        if (cropType == null || cropType.isBlank()) return "crop";
        String value = cropType.trim().toLowerCase();
        return value.startsWith("crop_") ? value.substring("crop_".length()) : value;
    }

    private boolean canFitStack(InventoryState inventory, String itemType, int amount) {
        if (inventory == null || itemType == null || itemType.isBlank() || amount <= 0) return false;
        int remaining = amount;
        for (ItemStackState slot : inventory.slots()) {
            if (slot == null || slot.isEmpty()) {
                remaining -= MAX_STACK;
            } else if (itemType.equals(slot.itemType())) {
                remaining -= Math.max(0, MAX_STACK - slot.amount());
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    private boolean addStackToInventory(InventoryState inventory, String itemType, int amount, long revision, long tick) {
        if (!canFitStack(inventory, itemType, amount)) return false;
        int remaining = amount;
        for (int i = 0; i < inventory.slotCount() && remaining > 0; i++) {
            ItemStackState slot = inventory.slot(i);
            if (slot.isEmpty() || !itemType.equals(slot.itemType())) continue;
            int moved = Math.min(remaining, Math.max(0, MAX_STACK - slot.amount()));
            if (moved <= 0) continue;
            inventory.setSlot(i, slot.withAmount(slot.amount() + moved), revision, tick);
            remaining -= moved;
        }
        for (int i = 0; i < inventory.slotCount() && remaining > 0; i++) {
            ItemStackState slot = inventory.slot(i);
            if (!slot.isEmpty()) continue;
            int moved = Math.min(remaining, MAX_STACK);
            inventory.setSlot(i, new ItemStackState(itemType, moved), revision, tick);
            remaining -= moved;
        }
        return remaining <= 0;
    }

    private void attachDefaultComponents(EntityState entity, long revision, long tick) {
        entity.putComponent("dimension", entity.dimensionId(), revision, tick);
        if ("boat".equals(entity.entityType())) {
            entity.putComponent("movement", "water_only", revision, tick);
            entity.putComponent("health", "100/100", revision, tick);
            entity.putComponent("max_health", "100", revision, tick);
            entity.putComponent("rider", "", revision, tick);
        } else if ("chest".equals(entity.entityType()) || "barrel".equals(entity.entityType())) {
            entity.putComponent("container", entity.entityType(), revision, tick);
        } else if ("crafting_table".equals(entity.entityType())) {
            entity.putComponent("crafting", "table", revision, tick);
        } else if (entity.entityType().contains("portal") || "door".equals(entity.entityType())) {
            entity.putComponent("portal", "pending_destination", revision, tick);
        } else if (isMob(entity)) {
            int max = defaultMaxHealth(entity.entityType());
            entity.putComponent("health", max + "/" + max, revision, tick);
            entity.putComponent("max_health", Integer.toString(max), revision, tick);
            entity.putComponent("mob", entity.entityType(), revision, tick);
            entity.putComponent("attack_cooldown", "0", revision, tick);
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
        setSlot(inventory, 11, "wheat", 6, revision, tick);
        setSlot(inventory, 12, "stone", 4, revision, tick);
        setSlot(inventory, 13, "crafting_table", 1, revision, tick);
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

    private CommandOutcome applyCraftingTableClick(
            Session session,
            ProtocolPayloads.CommandRequest command,
            long sequence,
            long tick,
            ActionResolver.EventSink events,
            InventoryState table,
            InventoryState cursor) {
        if (command.slotIndex == CRAFTING_OUTPUT_SLOT) {
            CommandOutcome outcome = takeCraftingOutput(table, cursor, tick);
            if (!outcome.accepted) return outcome;
            dirty = true;
            markSequence(session, sequence, tick);
            if (events != null) events.event(session.playerId, "crafting", table.inventoryId() + ":output");
            return CommandOutcome.accept();
        }
        if (command.slotIndex >= CRAFTING_INPUT_SLOTS) {
            return CommandOutcome.reject("invalid crafting slot");
        }

        long revision = world.bumpRevision();
        boolean changed = command.button == 3
            ? rightClickSlot(table, cursor, command.slotIndex, revision, tick)
            : leftClickSlot(table, cursor, command.slotIndex, revision, tick);
        if (changed) {
            refreshCraftingOutput(table, world.bumpRevision(), tick);
            dirty = true;
            if (events != null) events.event(session.playerId, "crafting", table.inventoryId() + ":" + command.slotIndex);
        }
        markSequence(session, sequence, tick);
        return CommandOutcome.accept();
    }

    private CommandOutcome takeCraftingOutput(InventoryState table, InventoryState cursor, long tick) {
        CraftRecipe recipe = matchingCraftRecipe(table);
        if (recipe == null) {
            refreshCraftingOutput(table, world.bumpRevision(), tick);
            return CommandOutcome.reject("no matching recipe");
        }
        ItemStackState hand = cursor.slot(0);
        if (!hand.isEmpty() && !recipe.outputType.equals(hand.itemType())) {
            return CommandOutcome.reject("cursor holds another item");
        }
        int cursorRoom = hand.isEmpty() ? MAX_STACK : Math.max(0, MAX_STACK - hand.amount());
        if (cursorRoom < recipe.outputAmount) return CommandOutcome.reject("cursor full");

        long revision = world.bumpRevision();
        if (hand.isEmpty()) {
            cursor.setSlot(0, new ItemStackState(recipe.outputType, recipe.outputAmount), revision, tick);
        } else {
            cursor.setSlot(0, hand.withAmount(hand.amount() + recipe.outputAmount), revision, tick);
        }
        consumeCraftingInputs(table, recipe, revision, tick);
        refreshCraftingOutput(table, world.bumpRevision(), tick);
        return CommandOutcome.accept();
    }

    private void refreshCraftingOutput(InventoryState table, long revision, long tick) {
        if (table == null || table.slotCount() <= CRAFTING_OUTPUT_SLOT) return;
        CraftRecipe recipe = matchingCraftRecipe(table);
        if (recipe == null) {
            table.setSlot(CRAFTING_OUTPUT_SLOT, ItemStackState.EMPTY, revision, tick);
        } else {
            table.setSlot(CRAFTING_OUTPUT_SLOT,
                new ItemStackState(recipe.outputType, recipe.outputAmount), revision, tick);
        }
    }

    private CraftRecipe matchingCraftRecipe(InventoryState table) {
        if (table == null) return null;
        LinkedHashMap<String, Integer> bag = craftingInputBag(table);
        if (bag.isEmpty()) return null;
        for (CraftRecipe recipe : CRAFT_RECIPES) {
            if (recipe.matches(bag)) return recipe;
        }
        return null;
    }

    private LinkedHashMap<String, Integer> craftingInputBag(InventoryState table) {
        LinkedHashMap<String, Integer> bag = new LinkedHashMap<>();
        int limit = Math.min(CRAFTING_INPUT_SLOTS, table.slotCount());
        for (int i = 0; i < limit; i++) {
            ItemStackState slot = table.slot(i);
            if (slot == null || slot.isEmpty()) continue;
            bag.merge(slot.itemType(), slot.amount(), Integer::sum);
        }
        return bag;
    }

    private void consumeCraftingInputs(InventoryState table, CraftRecipe recipe, long revision, long tick) {
        LinkedHashMap<String, Integer> remaining = new LinkedHashMap<>(recipe.ingredients);
        int limit = Math.min(CRAFTING_INPUT_SLOTS, table.slotCount());
        for (int i = 0; i < limit; i++) {
            ItemStackState slot = table.slot(i);
            if (slot == null || slot.isEmpty()) continue;
            Integer need = remaining.get(slot.itemType());
            if (need == null || need <= 0) continue;
            int consumed = Math.min(need, slot.amount());
            table.setSlot(i, slot.withAmount(slot.amount() - consumed), revision, tick);
            int left = need - consumed;
            if (left <= 0) remaining.remove(slot.itemType());
            else remaining.put(slot.itemType(), left);
        }
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

    private EntityState targetInArc(Session session, double targetX, double targetY, int rangePx, double arcDegrees) {
        double sx = playerCenterX(session);
        double sy = playerCenterY(session);
        double[] dir = normalizedDirection(sx, sy, targetX, targetY, session);
        double halfArc = Math.max(1.0, arcDegrees) / 2.0;
        EntityState best = null;
        double bestDist2 = Double.POSITIVE_INFINITY;
        for (EntityState entity : world.entities()) {
            if (entity == null || entity.removed() || !isDamageable(entity) || isProjectile(entity)) continue;
            double dx = entity.worldX() - sx;
            double dy = entity.worldY() - sy;
            double dist2 = dx * dx + dy * dy;
            double maxRange = rangePx + entityRadius(entity);
            if (dist2 > maxRange * maxRange) continue;
            double dist = Math.sqrt(Math.max(0.0001, dist2));
            double dot = ((dx * dir[0]) + (dy * dir[1])) / dist;
            dot = Math.max(-1.0, Math.min(1.0, dot));
            double angle = Math.toDegrees(Math.acos(dot));
            if (angle > halfArc) continue;
            if (dist2 < bestDist2) {
                bestDist2 = dist2;
                best = entity;
            }
        }
        return best;
    }

    private String selectedWeaponType(Session session, ProtocolPayloads.CommandRequest command) {
        InventoryState inventory = playerInventory(session.playerId);
        if (inventory != null && command.selectedSlot >= 0 && command.selectedSlot < inventory.slotCount()) {
            ItemStackState selected = inventory.slot(command.selectedSlot);
            if (!selected.isEmpty()) {
                if (command.itemType == null || command.itemType.isBlank() || command.itemType.equals(selected.itemType())) {
                    return selected.itemType();
                }
            }
        }
        return command.itemType == null || command.itemType.isBlank() ? "sword" : command.itemType;
    }

    private double[] normalizedDirection(double fromX, double fromY, double toX, double toY, Session session) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double len = Math.hypot(dx, dy);
        if (len > 0.0001) return new double[] { dx / len, dy / len };
        if (session != null && Math.hypot(session.vx, session.vy) > 0.0001) {
            double vLen = Math.hypot(session.vx, session.vy);
            return new double[] { session.vx / vLen, session.vy / vLen };
        }
        return new double[] { 1.0, 0.0 };
    }

    private Session nearestSession(EntityState entity, Map<String, Session> sessions) {
        if (sessions == null || sessions.isEmpty()) return null;
        Session nearest = null;
        double best = MOB_DETECTION_RANGE * MOB_DETECTION_RANGE;
        for (Session session : sessions.values()) {
            if (session == null || session.health <= 0) continue;
            double dx = playerCenterX(session) - entity.worldX();
            double dy = playerCenterY(session) - entity.worldY();
            double dist2 = dx * dx + dy * dy;
            if (dist2 >= best) continue;
            best = dist2;
            nearest = session;
        }
        return nearest;
    }

    private boolean isCombatCommand(String commandType) {
        return ProtocolPayloads.CommandRequest.ATTACK_AT.equals(commandType)
            || ProtocolPayloads.CommandRequest.ATTACK_LIGHT_AT.equals(commandType)
            || ProtocolPayloads.CommandRequest.ATTACK_HEAVY_AT.equals(commandType)
            || ProtocolPayloads.CommandRequest.ATTACK_RANGED_AT.equals(commandType);
    }

    private boolean isDamageable(EntityState entity) {
        return entity != null && !entity.removed() && entity.component("health") != null;
    }

    private boolean isProjectile(EntityState entity) {
        return entity != null && ("true".equals(entity.component("projectile"))
            || "combat_bolt".equals(entity.entityType())
            || "boat_projectile".equals(entity.entityType()));
    }

    private boolean isTransientEntity(EntityState entity) {
        return entity != null && ("true".equals(entity.component("transient")) || isProjectile(entity));
    }

    private boolean isMob(EntityState entity) {
        if (entity == null) return false;
        String type = entity.entityType();
        return "goblin".equals(type) || "spider".equals(type) || "deer".equals(type);
    }

    private double entityRadius(EntityState entity) {
        if (entity == null) return 32.0;
        String type = entity.entityType();
        if ("boat".equals(type)) return 96.0;
        if ("combat_bolt".equals(type) || "boat_projectile".equals(type)) return PROJECTILE_SIZE / 2.0;
        if ("goblin".equals(type) || "spider".equals(type) || "deer".equals(type)) return 34.0;
        return 32.0;
    }

    private int maxHealth(EntityState entity) {
        int parsed = parseInt(entity.component("max_health"), -1);
        if (parsed > 0) return parsed;
        String health = entity.component("health");
        int slash = health == null ? -1 : health.indexOf('/');
        if (slash >= 0) return parseInt(health.substring(slash + 1), defaultMaxHealth(entity.entityType()));
        return Math.max(parseInt(health, defaultMaxHealth(entity.entityType())), defaultMaxHealth(entity.entityType()));
    }

    private int currentHealth(EntityState entity, int fallbackMax) {
        String health = entity.component("health");
        if (health == null || health.isBlank()) return fallbackMax;
        int slash = health.indexOf('/');
        if (slash >= 0) return parseInt(health.substring(0, slash), fallbackMax);
        return parseInt(health, fallbackMax);
    }

    private int defaultMaxHealth(String type) {
        if ("boat".equals(type)) return 100;
        if ("goblin".equals(type)) return 10;
        if ("spider".equals(type)) return 5;
        if ("deer".equals(type)) return 8;
        if ("tree".equals(type)) return 5;
        if ("rock".equals(type)) return 6;
        if ("ore".equals(type)) return 8;
        return 1;
    }

    private int mobAttackRange(String type) {
        if ("spider".equals(type)) return 40;
        if ("goblin".equals(type)) return 50;
        return 34;
    }

    private int mobAttackCooldown(String type) {
        if ("spider".equals(type)) return 28;
        if ("goblin".equals(type)) return 24;
        return 45;
    }

    private int mobDamage(String type) {
        if ("spider".equals(type)) return 1;
        if ("goblin".equals(type)) return 2;
        return 0;
    }

    private double mobSpeed(String type) {
        if ("spider".equals(type)) return 1.2;
        if ("goblin".equals(type)) return 0.9;
        if ("deer".equals(type)) return 0.7;
        return 0.6;
    }

    private List<ItemStackState> lootFor(String entityType) {
        ArrayList<ItemStackState> loot = new ArrayList<>();
        if ("deer".equals(entityType)) {
            loot.add(new ItemStackState("hide", 1));
            loot.add(new ItemStackState("meat", 1));
        } else if ("goblin".equals(entityType)) {
            loot.add(new ItemStackState("stone", 1));
        } else if ("spider".equals(entityType)) {
            loot.add(new ItemStackState("stone", 1));
        } else if ("tree".equals(entityType)) {
            loot.add(new ItemStackState("wood", 3));
        } else if ("rock".equals(entityType)) {
            loot.add(new ItemStackState("stone", 3));
        } else if ("ore".equals(entityType)) {
            loot.add(new ItemStackState("iron", 2));
        }
        return loot;
    }

    private double playerCenterX(Session session) {
        return session == null ? 0.0 : session.x + PLAYER_CENTER_OFFSET_X;
    }

    private double playerCenterY(Session session) {
        return session == null ? 0.0 : session.y + PLAYER_CENTER_OFFSET_Y;
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return Integer.parseInt(raw.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return Double.parseDouble(raw.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
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
            || "stone_wall".equals(type)
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

    private static final List<CraftRecipe> CRAFT_RECIPES = craftRecipes();

    private static List<CraftRecipe> craftRecipes() {
        ArrayList<CraftRecipe> recipes = new ArrayList<>();
        recipes.add(recipe("axe", 1, "stone", 3, "wheat", 2));
        recipes.add(recipe("pickaxe", 1, "stone", 3, "iron_ore", 1));
        recipes.add(recipe("hammer", 1, "stone", 4, "hide", 1));
        recipes.add(recipe("barrel", 1, "wheat", 6));
        recipes.add(recipe("fence", 2, "wheat", 2));
        recipes.add(recipe("torch", 4, "meat", 1, "wheat", 1));
        recipes.add(recipe("sword", 1, "iron_ore", 2, "wheat", 1));
        recipes.add(recipe("crafting_table", 1, "wheat", 4));
        return java.util.Collections.unmodifiableList(recipes);
    }

    private static CraftRecipe recipe(String outputType, int outputAmount, Object... ingredients) {
        LinkedHashMap<String, Integer> bag = new LinkedHashMap<>();
        for (int i = 0; i + 1 < ingredients.length; i += 2) {
            String itemType = String.valueOf(ingredients[i]).trim().toLowerCase();
            int amount = ((Number) ingredients[i + 1]).intValue();
            bag.merge(itemType, amount, Integer::sum);
        }
        return new CraftRecipe(outputType, outputAmount, bag);
    }

    private static final class CraftRecipe {
        final String outputType;
        final int outputAmount;
        final LinkedHashMap<String, Integer> ingredients;

        CraftRecipe(String outputType, int outputAmount, LinkedHashMap<String, Integer> ingredients) {
            this.outputType = sanitizeRecipeName(outputType);
            this.outputAmount = Math.max(1, outputAmount);
            this.ingredients = new LinkedHashMap<>(ingredients);
        }

        boolean matches(Map<String, Integer> bag) {
            if (bag == null || bag.size() != ingredients.size()) return false;
            for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
                Integer have = bag.get(entry.getKey());
                if (have == null || have.intValue() != entry.getValue().intValue()) return false;
            }
            return true;
        }

        private static String sanitizeRecipeName(String raw) {
            return raw == null || raw.isBlank() ? "empty" : raw.trim().toLowerCase();
        }
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

    private static final class DamageResult {
        static final DamageResult NONE = new DamageResult(false, 0, 0, false);
        final boolean hit;
        final int currentHealth;
        final int maxHealth;
        final boolean killed;

        DamageResult(boolean hit, int currentHealth, int maxHealth, boolean killed) {
            this.hit = hit;
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
            this.killed = killed;
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
