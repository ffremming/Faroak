package resources.net.multiplayer.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import resources.net.multiplayer.MultiplayerAction;

/**
 * Payload value objects for protocol v1.
 */
public final class ProtocolPayloads {

    private ProtocolPayloads() {}

    public static final class InputState {
        public final boolean up;
        public final boolean left;
        public final boolean down;
        public final boolean right;
        // Client-authoritative movement: the client (which does precise local
        // collision) reports its actual resolved position. The server adopts it
        // (clamped) instead of re-simulating movement with a different collision
        // model — that mismatch caused the reconciliation teleport near obstacles.
        // hasPosition=false on legacy/keys-only messages.
        public final boolean hasPosition;
        public final double posX;
        public final double posY;

        public InputState(boolean up, boolean left, boolean down, boolean right) {
            this(up, left, down, right, false, 0.0, 0.0);
        }

        public InputState(boolean up, boolean left, boolean down, boolean right,
                          boolean hasPosition, double posX, double posY) {
            this.up = up;
            this.left = left;
            this.down = down;
            this.right = right;
            this.hasPosition = hasPosition;
            this.posX = posX;
            this.posY = posY;
        }
    }

    public static final class ActionRequest {
        public final MultiplayerAction action;
        public final boolean hasTarget;
        public final double targetX;
        public final double targetY;
        public final String argument;

        public ActionRequest(MultiplayerAction action, boolean hasTarget, double targetX, double targetY) {
            this(action, hasTarget, targetX, targetY, "");
        }

        public ActionRequest(
                MultiplayerAction action,
                boolean hasTarget,
                double targetX,
                double targetY,
                String argument) {
            this.action = action;
            this.hasTarget = hasTarget;
            this.targetX = targetX;
            this.targetY = targetY;
            this.argument = (argument == null) ? "" : argument;
        }
    }

    public static final class CommandRequest {
        public static final String USE_EQUIPPED_AT = "use_equipped_at";
        public static final String INTERACT_ENTITY = "interact_entity";
        public static final String INTERACT_AT = "interact_at";
        public static final String ATTACK_AT = "attack_at";
        public static final String ATTACK_LIGHT_AT = "attack_light_at";
        public static final String ATTACK_HEAVY_AT = "attack_heavy_at";
        public static final String ATTACK_RANGED_AT = "attack_ranged_at";
        public static final String FIRE_BROADSIDE = "fire_broadside";
        public static final String INVENTORY_CLICK = "inventory_click";
        public static final String RESPAWN = "respawn";

        public final String commandType;
        public final boolean hasTarget;
        public final double targetX;
        public final double targetY;
        public final long targetEntityId;
        public final String itemType;
        public final int selectedSlot;
        public final long inventoryId;
        public final int slotIndex;
        public final int button;

        public CommandRequest(
                String commandType,
                boolean hasTarget,
                double targetX,
                double targetY,
                long targetEntityId,
                String itemType,
                int selectedSlot,
                long inventoryId,
                int slotIndex,
                int button) {
            this.commandType = normalize(commandType);
            this.hasTarget = hasTarget;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetEntityId = Math.max(0L, targetEntityId);
            this.itemType = (itemType == null) ? "" : itemType.trim().toLowerCase();
            this.selectedSlot = Math.max(-1, selectedSlot);
            this.inventoryId = Math.max(0L, inventoryId);
            this.slotIndex = Math.max(-1, slotIndex);
            this.button = Math.max(0, button);
        }

        public static CommandRequest useEquippedAt(double x, double y, String itemType, int selectedSlot) {
            return new CommandRequest(USE_EQUIPPED_AT, true, x, y, 0L, itemType, selectedSlot, 0L, -1, 0);
        }

        public static CommandRequest interactEntity(long entityId, double x, double y) {
            return new CommandRequest(INTERACT_ENTITY, true, x, y, entityId, "", -1, 0L, -1, 0);
        }

        public static CommandRequest interactAt(double x, double y) {
            return new CommandRequest(INTERACT_AT, true, x, y, 0L, "", -1, 0L, -1, 0);
        }

        public static CommandRequest attackAt(double x, double y) {
            return new CommandRequest(ATTACK_AT, true, x, y, 0L, "", -1, 0L, -1, 0);
        }

        public static CommandRequest fireBroadside(double x, double y) {
            return new CommandRequest(FIRE_BROADSIDE, true, x, y, 0L, "", -1, 0L, -1, 0);
        }

        public static CommandRequest lightAttackAt(double x, double y, String itemType, int selectedSlot) {
            return new CommandRequest(ATTACK_LIGHT_AT, true, x, y, 0L, itemType, selectedSlot, 0L, -1, 0);
        }

        public static CommandRequest heavyAttackAt(double x, double y, String itemType, int selectedSlot) {
            return new CommandRequest(ATTACK_HEAVY_AT, true, x, y, 0L, itemType, selectedSlot, 0L, -1, 0);
        }

        public static CommandRequest rangedAttackAt(double x, double y, String itemType, int selectedSlot) {
            return new CommandRequest(ATTACK_RANGED_AT, true, x, y, 0L, itemType, selectedSlot, 0L, -1, 0);
        }

        public static CommandRequest inventoryClick(long inventoryId, int slotIndex, int button) {
            return new CommandRequest(INVENTORY_CLICK, false, 0.0, 0.0, 0L, "", -1, inventoryId, slotIndex, button);
        }

        public static CommandRequest respawn() {
            return new CommandRequest(RESPAWN, false, 0.0, 0.0, 0L, "", -1, 0L, -1, 0);
        }

        private static String normalize(String raw) {
            if (raw == null || raw.isBlank()) return "";
            return raw.trim().toLowerCase();
        }
    }

    public static final class JoinRequest {
        public final boolean hasSpawn;
        public final double spawnX;
        public final double spawnY;

        public JoinRequest(boolean hasSpawn, double spawnX, double spawnY) {
            this.hasSpawn = hasSpawn;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
        }
    }

    public static final class Ack {
        public final long acknowledgedSequence;

        public Ack(long acknowledgedSequence) {
            this.acknowledgedSequence = Math.max(0L, acknowledgedSequence);
        }
    }

    public static final class Presence {
        public final String playerId;
        public final boolean joined;

        public Presence(String playerId, boolean joined) {
            this.playerId = (playerId == null) ? "" : playerId;
            this.joined = joined;
        }
    }

    public static final class Reject {
        public final String reason;

        public Reject(String reason) {
            this.reason = (reason == null) ? "" : reason;
        }
    }

    public static final class PlayerState {
        public final String playerId;
        public final double worldX;
        public final double worldY;
        public final double velocityX;
        public final double velocityY;
        public final long processedSequence;
        public final int health;
        public final int maxHealth;
        // Appearance / status — carried in a trailing, backward-compatible snapshot
        // section. Defaults match a freshly-spawned local player.
        public final int facing;          // 0=up,1=right,2=down,3=left
        public final boolean moving;
        public final String spriteName;
        public final String displayName;
        public final boolean alive;
        // Item the player currently has selected in their hotbar (the equipped tool/weapon),
        // by item name; "" when nothing is equipped. Carried in the trailing appearance
        // section so guests can render what a remote player is holding. Default empty.
        public final String equippedItem;

        public PlayerState(
                String playerId,
                double worldX,
                double worldY,
                double velocityX,
                double velocityY,
                long processedSequence) {
            this(playerId, worldX, worldY, velocityX, velocityY, processedSequence, 20, 20);
        }

        public PlayerState(
                String playerId,
                double worldX,
                double worldY,
                double velocityX,
                double velocityY,
                long processedSequence,
                int health,
                int maxHealth) {
            this(playerId, worldX, worldY, velocityX, velocityY, processedSequence, health, maxHealth,
                2, false, "red", "", true);
        }

        public PlayerState(
                String playerId,
                double worldX,
                double worldY,
                double velocityX,
                double velocityY,
                long processedSequence,
                int health,
                int maxHealth,
                int facing,
                boolean moving,
                String spriteName,
                String displayName,
                boolean alive) {
            this(playerId, worldX, worldY, velocityX, velocityY, processedSequence, health, maxHealth,
                facing, moving, spriteName, displayName, alive, "");
        }

        public PlayerState(
                String playerId,
                double worldX,
                double worldY,
                double velocityX,
                double velocityY,
                long processedSequence,
                int health,
                int maxHealth,
                int facing,
                boolean moving,
                String spriteName,
                String displayName,
                boolean alive,
                String equippedItem) {
            this.playerId = (playerId == null) ? "" : playerId;
            this.worldX = worldX;
            this.worldY = worldY;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.processedSequence = Math.max(0L, processedSequence);
            this.maxHealth = Math.max(1, maxHealth);
            this.health = Math.max(0, Math.min(this.maxHealth, health));
            this.facing = (facing < 0 || facing > 3) ? 2 : facing;
            this.moving = moving;
            this.spriteName = (spriteName == null || spriteName.isBlank()) ? "red" : spriteName;
            this.displayName = (displayName == null) ? "" : displayName;
            this.alive = alive;
            this.equippedItem = (equippedItem == null) ? "" : equippedItem;
        }

        /** Copy this row with appearance/status fields overridden (used when zipping
         *  the trailing appearance section back onto a decoded player list). */
        public PlayerState withAppearance(int facing, boolean moving, String spriteName,
                                          String displayName, boolean alive) {
            return withAppearance(facing, moving, spriteName, displayName, alive, this.equippedItem);
        }

        /** As {@link #withAppearance(int,boolean,String,String,boolean)} but also carries the
         *  equipped item name (the trailing appearance section now includes it). */
        public PlayerState withAppearance(int facing, boolean moving, String spriteName,
                                          String displayName, boolean alive, String equippedItem) {
            return new PlayerState(playerId, worldX, worldY, velocityX, velocityY,
                processedSequence, health, maxHealth, facing, moving, spriteName, displayName, alive,
                equippedItem);
        }
    }

    public static final class Snapshot {
        public final boolean baseline;
        public final long acknowledgedSequence;
        public final List<PlayerState> players;
        public final List<WorldObjectState> worldObjects;
        public final List<EntityStatePayload> entities;
        public final List<InventoryStatePayload> inventories;
        public final List<TileMutationPayload> tileMutations;
        /** Authoritative world-clock tick (server GameClock). 0 = unknown/legacy.
         *  Carried in a trailing, backward-compatible codec section. */
        public long worldTimeTicks;

        public Snapshot(boolean baseline, long acknowledgedSequence, List<PlayerState> players) {
            this(baseline, acknowledgedSequence, players, new ArrayList<>());
        }

        public Snapshot(
                boolean baseline,
                long acknowledgedSequence,
                List<PlayerState> players,
                List<WorldObjectState> worldObjects) {
            this(baseline, acknowledgedSequence, players, worldObjects,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public Snapshot(
                boolean baseline,
                long acknowledgedSequence,
                List<PlayerState> players,
                List<WorldObjectState> worldObjects,
                List<EntityStatePayload> entities,
                List<InventoryStatePayload> inventories,
                List<TileMutationPayload> tileMutations) {
            this.baseline = baseline;
            this.acknowledgedSequence = Math.max(0L, acknowledgedSequence);
            List<PlayerState> safe = (players == null) ? new ArrayList<>() : new ArrayList<>(players);
            this.players = Collections.unmodifiableList(safe);
            List<WorldObjectState> safeObjects = (worldObjects == null) ? new ArrayList<>() : new ArrayList<>(worldObjects);
            this.worldObjects = Collections.unmodifiableList(safeObjects);
            List<EntityStatePayload> safeEntities = (entities == null) ? new ArrayList<>() : new ArrayList<>(entities);
            this.entities = Collections.unmodifiableList(safeEntities);
            List<InventoryStatePayload> safeInventories = (inventories == null) ? new ArrayList<>() : new ArrayList<>(inventories);
            this.inventories = Collections.unmodifiableList(safeInventories);
            List<TileMutationPayload> safeTiles = (tileMutations == null) ? new ArrayList<>() : new ArrayList<>(tileMutations);
            this.tileMutations = Collections.unmodifiableList(safeTiles);
        }

        public Snapshot withWorldTime(long worldTimeTicks) {
            this.worldTimeTicks = Math.max(0L, worldTimeTicks);
            return this;
        }
    }

    public static final class WorldObjectState {
        public final long objectId;
        public final String objectType;
        public final double worldX;
        public final double worldY;
        public final boolean removed;
        public final long revision;

        public WorldObjectState(
                long objectId,
                String objectType,
                double worldX,
                double worldY,
                boolean removed,
                long revision) {
            this.objectId = Math.max(0L, objectId);
            this.objectType = (objectType == null) ? "" : objectType;
            this.worldX = worldX;
            this.worldY = worldY;
            this.removed = removed;
            this.revision = Math.max(0L, revision);
        }
    }

    public static final class EntityStatePayload {
        public final long entityId;
        public final String entityType;
        public final String dimensionId;
        public final double worldX;
        public final double worldY;
        public final boolean removed;
        public final long revision;
        public final List<ComponentStatePayload> components;

        public EntityStatePayload(
                long entityId,
                String entityType,
                String dimensionId,
                double worldX,
                double worldY,
                boolean removed,
                long revision,
                List<ComponentStatePayload> components) {
            this.entityId = Math.max(0L, entityId);
            this.entityType = (entityType == null) ? "" : entityType;
            this.dimensionId = (dimensionId == null || dimensionId.isBlank()) ? "core:overworld" : dimensionId;
            this.worldX = worldX;
            this.worldY = worldY;
            this.removed = removed;
            this.revision = Math.max(0L, revision);
            List<ComponentStatePayload> safe = (components == null) ? new ArrayList<>() : new ArrayList<>(components);
            this.components = Collections.unmodifiableList(safe);
        }
    }

    public static final class ComponentStatePayload {
        public final String key;
        public final String value;

        public ComponentStatePayload(String key, String value) {
            this.key = (key == null) ? "" : key;
            this.value = (value == null) ? "" : value;
        }
    }

    public static final class InventoryStatePayload {
        public final long inventoryId;
        public final long ownerEntityId;
        public final String inventoryType;
        public final long revision;
        public final List<ItemStackPayload> slots;

        public InventoryStatePayload(
                long inventoryId,
                long ownerEntityId,
                String inventoryType,
                long revision,
                List<ItemStackPayload> slots) {
            this.inventoryId = Math.max(0L, inventoryId);
            this.ownerEntityId = Math.max(0L, ownerEntityId);
            this.inventoryType = (inventoryType == null) ? "" : inventoryType;
            this.revision = Math.max(0L, revision);
            List<ItemStackPayload> safe = (slots == null) ? new ArrayList<>() : new ArrayList<>(slots);
            this.slots = Collections.unmodifiableList(safe);
        }
    }

    public static final class ItemStackPayload {
        public final String itemType;
        public final int amount;

        public ItemStackPayload(String itemType, int amount) {
            this.itemType = (itemType == null || itemType.isBlank()) ? "empty" : itemType;
            this.amount = Math.max(0, amount);
        }
    }

    public static final class TileMutationPayload {
        public final String dimensionId;
        public final int tileX;
        public final int tileY;
        public final String tileType;
        public final boolean watered;
        public final String cropType;
        public final int cropStage;
        public final long revision;

        public TileMutationPayload(
                String dimensionId,
                int tileX,
                int tileY,
                String tileType,
                boolean watered,
                String cropType,
                int cropStage,
                long revision) {
            this.dimensionId = (dimensionId == null || dimensionId.isBlank()) ? "core:overworld" : dimensionId;
            this.tileX = tileX;
            this.tileY = tileY;
            this.tileType = (tileType == null) ? "" : tileType;
            this.watered = watered;
            this.cropType = (cropType == null) ? "" : cropType;
            this.cropStage = Math.max(0, cropStage);
            this.revision = Math.max(0L, revision);
        }
    }

    public static final class CommandResult {
        public final long commandSequence;
        public final boolean accepted;
        public final String reason;

        public CommandResult(long commandSequence, boolean accepted, String reason) {
            this.commandSequence = Math.max(0L, commandSequence);
            this.accepted = accepted;
            this.reason = (reason == null) ? "" : reason;
        }
    }
}
