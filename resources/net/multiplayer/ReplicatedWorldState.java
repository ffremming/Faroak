package resources.net.multiplayer;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.farming.FarmTile;
import resources.domain.object.Boat;
import resources.domain.object.GameObject;
import resources.net.multiplayer.message.WorldObjectStateMessage;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Client-side replica of authoritative world state. Rendering still uses local
 * entity instances, while authoritative inventories/tile/component state stays
 * in lightweight snapshot maps for UI and future systems to consume.
 */
final class ReplicatedWorldState {

    private static final int FALLBACK_SIZE = 64;
    private static final int TILE_SIZE = 64;

    private final GameContext ctx;
    private final Map<Long, BaseEntity> entitiesById = new HashMap<>();
    private final Map<BaseEntity, Long> idsByEntity = new IdentityHashMap<>();
    private final Map<Long, ProtocolPayloads.EntityStatePayload> entityStateById = new HashMap<>();
    private final Map<Long, ProtocolPayloads.InventoryStatePayload> inventoriesById = new HashMap<>();
    private final Map<String, ProtocolPayloads.TileMutationPayload> tileMutationsByKey = new HashMap<>();

    ReplicatedWorldState(GameContext ctx) {
        this.ctx = ctx;
    }

    void applySnapshot(
            boolean baseline,
            List<WorldObjectStateMessage> compatibilityObjects,
            List<ProtocolPayloads.EntityStatePayload> entities,
            List<ProtocolPayloads.InventoryStatePayload> inventories,
            List<ProtocolPayloads.TileMutationPayload> tileMutations) {
        Set<Long> seen = baseline ? new HashSet<>() : null;
        if (compatibilityObjects != null) {
            for (WorldObjectStateMessage state : compatibilityObjects) {
                if (state == null) continue;
                if (baseline) seen.add(state.objectId());
                if (state.removed()) removeEntity(state.objectId());
                else upsertCompatibility(state);
            }
        }
        if (entities != null) {
            for (ProtocolPayloads.EntityStatePayload state : entities) {
                if (state == null) continue;
                if (baseline) seen.add(state.entityId);
                entityStateById.put(state.entityId, state);
                if (state.removed) removeEntity(state.entityId);
                else upsertEntity(state);
            }
        }
        if (inventories != null) {
            for (ProtocolPayloads.InventoryStatePayload inventory : inventories) {
                if (inventory == null) continue;
                inventoriesById.put(inventory.inventoryId, inventory);
            }
        }
        if (tileMutations != null) {
            for (ProtocolPayloads.TileMutationPayload tile : tileMutations) {
                if (tile == null) continue;
                tileMutationsByKey.put(tileKey(tile), tile);
                applyTileMutation(tile);
            }
        }
        if (baseline) removeMissing(seen);
    }

    int entityCount() {
        return entitiesById.size();
    }

    /** Test-only: human-readable dump of an entity's replicated type + components. */
    String debugEntityState(long entityId) {
        ProtocolPayloads.EntityStatePayload s = entityStateById.get(entityId);
        if (s == null) return "id=" + entityId + " <no snapshot state>";
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(s.entityId).append(" type=").append(s.entityType)
          .append(" removed=").append(s.removed).append(" comps={");
        if (s.components != null) {
            for (ProtocolPayloads.ComponentStatePayload c : s.components) {
                if (c == null) continue;
                sb.append(c.key).append('=').append(c.value).append(", ");
            }
        }
        return sb.append('}').toString();
    }

    int inventoryCount() {
        return inventoriesById.size();
    }

    int tileMutationCount() {
        return tileMutationsByKey.size();
    }

    ProtocolPayloads.InventoryStatePayload inventoryForOwner(long ownerEntityId) {
        for (ProtocolPayloads.InventoryStatePayload inventory : inventoriesById.values()) {
            if (inventory.ownerEntityId == ownerEntityId) return inventory;
        }
        return null;
    }

    ProtocolPayloads.InventoryStatePayload inventory(long inventoryId) {
        return inventoriesById.get(inventoryId);
    }

    ProtocolPayloads.InventoryStatePayload inventoryForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) return null;
        String key = "player:" + normalizePlayerId(playerId);
        for (ProtocolPayloads.InventoryStatePayload inventory : inventoriesById.values()) {
            if (inventory == null) continue;
            if (key.equals(inventory.inventoryType)) return inventory;
        }
        return null;
    }

    ProtocolPayloads.InventoryStatePayload cursorForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) return null;
        String key = "cursor:" + normalizePlayerId(playerId);
        for (ProtocolPayloads.InventoryStatePayload inventory : inventoriesById.values()) {
            if (inventory == null) continue;
            if (key.equals(inventory.inventoryType)) return inventory;
        }
        return null;
    }

    long entityIdFor(BaseEntity entity) {
        if (entity == null) return 0L;
        Long id = idsByEntity.get(entity);
        return id == null ? 0L : id.longValue();
    }

    /** Replicated rider id for an entity (null if unknown), used to skip occupied boats. */
    String riderOf(long entityId) {
        ProtocolPayloads.EntityStatePayload s = entityStateById.get(entityId);
        if (s == null || s.components == null) return null;
        for (ProtocolPayloads.ComponentStatePayload c : s.components) {
            if (c != null && "rider".equals(c.key)) return c.value;
        }
        return null;
    }

    long ridingEntityIdFor(String playerId) {
        if (playerId == null || playerId.isBlank()) return 0L;
        for (ProtocolPayloads.EntityStatePayload entity : entityStateById.values()) {
            if (entity == null || entity.removed) continue;
            for (ProtocolPayloads.ComponentStatePayload component : entity.components) {
                if (component == null) continue;
                if ("rider".equals(component.key) && playerId.equals(component.value)) {
                    return entity.entityId;
                }
            }
        }
        return 0L;
    }

    /** The replicated Boat entity the given player is riding, or null. Used by the client
     *  to glue its own local player to the boat after the boat's snapshot position lands. */
    Boat ridingBoatFor(String playerId) {
        long id = ridingEntityIdFor(playerId);
        if (id <= 0L) return null;
        BaseEntity e = entitiesById.get(id);
        return (e instanceof Boat) ? (Boat) e : null;
    }

    private void upsertCompatibility(WorldObjectStateMessage state) {
        BaseEntity entity = entitiesById.get(state.objectId());
        if (entity == null) {
            entity = spawn(state.objectType());
            if (entity == null) return;
            positionCentered(entity, state.worldX(), state.worldY());
            if (!ctx.world().placeEntityAuthoritative(entity)) return;
            entitiesById.put(state.objectId(), entity);
            idsByEntity.put(entity, state.objectId());
            return;
        }
        positionCentered(entity, state.worldX(), state.worldY());
    }

    private void upsertEntity(ProtocolPayloads.EntityStatePayload state) {
        BaseEntity entity = entitiesById.get(state.entityId);
        if (entity == null) {
            entity = spawn(state.entityType);
            if (entity == null) return;
            positionCentered(entity, state.worldX, state.worldY);
            if (!ctx.world().placeEntityAuthoritative(entity)) return;
            entitiesById.put(state.entityId, entity);
            idsByEntity.put(entity, state.entityId);
            return;
        }
        positionCentered(entity, state.worldX, state.worldY);
    }

    private BaseEntity spawn(String rawType) {
        GamePanel panel = panel();
        if (panel == null) return null;
        String objectType = sanitizeType(rawType);

        BaseEntity template = null;
        if (ctx.items() != null && !objectType.isBlank()) {
            template = ctx.items().getPhysicalRepresentation(objectType);
        }

        if (template instanceof GameObject) {
            GameObject placed = ((GameObject) template).placementCandidate(panel);
            placed.getHitBox().updateCoords();
            return placed;
        }
        if (template instanceof Boat || "boat".equals(objectType)) {
            Boat boat = new Boat(panel, 0, 0, false);
            boat.getHitBox().updateCoords();
            return boat;
        }
        if ("combat_bolt".equals(objectType) || "boat_projectile".equals(objectType)) {
            return new GameObject(panel, objectType, 0, 0,
                28, 28, 20, 20, 4, 4, false);
        }
        if ("goblin".equals(objectType) || "spider".equals(objectType) || "deer".equals(objectType)) {
            return new GameObject(panel, objectType, 0, 0,
                48, 64, 32, 48, 8, 8, false);
        }

        return new GameObject(panel, objectType.isBlank() ? "block" : objectType, 0, 0,
            FALLBACK_SIZE, FALLBACK_SIZE, FALLBACK_SIZE, FALLBACK_SIZE, 0, 0, true);
    }

    private void removeEntity(long entityId) {
        entityStateById.remove(entityId);
        BaseEntity existing = entitiesById.remove(entityId);
        if (existing != null) {
            idsByEntity.remove(existing);
            ctx.world().removeEntity(existing);
        }
    }

    private void removeMissing(Set<Long> seen) {
        ArrayList<Long> toRemove = new ArrayList<>();
        for (Long entityId : entitiesById.keySet()) {
            if (!seen.contains(entityId)) toRemove.add(entityId);
        }
        for (Long entityId : toRemove) removeEntity(entityId.longValue());
    }

    private void applyTileMutation(ProtocolPayloads.TileMutationPayload tile) {
        if (ctx.world() == null) return;
        int worldX = tile.tileX * TILE_SIZE;
        int worldY = tile.tileY * TILE_SIZE;
        Point center = new Point(worldX + TILE_SIZE / 2, worldY + TILE_SIZE / 2);
        if ("farmland".equals(tile.tileType) || "farmland_watered".equals(tile.tileType)) {
            FarmTile farm = ctx.world().farmTileAt(center);
            if (farm == null) farm = ctx.world().tillTileAt(center);
            if (farm != null && tile.watered) farm.water();
            if (farm != null) farm.syncCrop(tile.cropType, tile.cropStage);
        }
    }

    private void positionCentered(BaseEntity entity, double centerX, double centerY) {
        if (entity == null) return;
        entity.setWorldX(centerX - (entity.getWidth() / 2.0));
        entity.setWorldY(centerY - (entity.getHeight() / 2.0));
        if (entity.getHitBox() != null) entity.getHitBox().updateCoords();
    }

    private GamePanel panel() {
        if (ctx instanceof GamePanel) return (GamePanel) ctx;
        if (ctx.player() != null) return ctx.player().panel;
        return null;
    }

    private String sanitizeType(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase();
        if (value.length() > 96) value = value.substring(0, 96);
        return value;
    }

    private String tileKey(ProtocolPayloads.TileMutationPayload tile) {
        return tile.dimensionId + ":" + tile.tileX + ":" + tile.tileY;
    }

    private String normalizePlayerId(String playerId) {
        return playerId == null ? "" : playerId.trim().toLowerCase();
    }
}
