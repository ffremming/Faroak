package resources.net.multiplayer.state;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Process-local authoritative world state. It is deliberately independent of rendering.
 */
public final class WorldState {

    private final LinkedHashMap<String, PlayerReplicaState> players = new LinkedHashMap<>();
    private final LinkedHashMap<Long, EntityState> entities = new LinkedHashMap<>();
    private final LinkedHashMap<Long, InventoryState> inventories = new LinkedHashMap<>();
    private final LinkedHashMap<String, TileMutationState> tileMutations = new LinkedHashMap<>();

    private long tick;
    private long revision;
    private long nextEntityId = 1L;

    public long tick() { return tick; }
    public long revision() { return revision; }
    public long nextEntityId() { return nextEntityId; }

    public void setCounters(long tick, long revision, long nextEntityId) {
        this.tick = Math.max(0L, tick);
        this.revision = Math.max(0L, revision);
        this.nextEntityId = Math.max(1L, nextEntityId);
    }

    public void setTick(long tick) {
        this.tick = Math.max(0L, tick);
    }

    public long bumpRevision() {
        revision++;
        return revision;
    }

    public long allocateEntityId() {
        return nextEntityId++;
    }

    public void observeEntityId(long entityId) {
        if (entityId >= nextEntityId) nextEntityId = entityId + 1L;
    }

    public PlayerReplicaState player(String playerId) {
        return players.get(playerId);
    }

    public void putPlayer(PlayerReplicaState player) {
        if (player != null && !player.playerId().isBlank()) players.put(player.playerId(), player);
    }

    public PlayerReplicaState removePlayer(String playerId) {
        return players.remove(playerId);
    }

    public EntityState entity(long entityId) {
        return entities.get(entityId);
    }

    public void putEntity(EntityState entity) {
        if (entity == null) return;
        entities.put(entity.entityId(), entity);
        observeEntityId(entity.entityId());
    }

    public EntityState removeEntity(long entityId) {
        return entities.remove(entityId);
    }

    public void putInventory(InventoryState inventory) {
        if (inventory != null) inventories.put(inventory.inventoryId(), inventory);
    }

    public InventoryState inventory(long inventoryId) {
        return inventories.get(inventoryId);
    }

    public void putTileMutation(TileMutationState mutation) {
        if (mutation != null) tileMutations.put(mutation.key(), mutation);
    }

    public TileMutationState tileMutation(String dimensionId, int tileX, int tileY) {
        return tileMutations.get(TileMutationState.key(dimensionId, tileX, tileY));
    }

    public Collection<PlayerReplicaState> players() { return Collections.unmodifiableCollection(players.values()); }
    public Collection<EntityState> entities() { return Collections.unmodifiableCollection(entities.values()); }
    public Collection<InventoryState> inventories() { return Collections.unmodifiableCollection(inventories.values()); }
    public Collection<TileMutationState> tileMutations() { return Collections.unmodifiableCollection(tileMutations.values()); }

    public Map<String, PlayerReplicaState> playerMap() { return Collections.unmodifiableMap(players); }
    public Map<Long, EntityState> entityMap() { return Collections.unmodifiableMap(entities); }
    public Map<Long, InventoryState> inventoryMap() { return Collections.unmodifiableMap(inventories); }
    public Map<String, TileMutationState> tileMutationMap() { return Collections.unmodifiableMap(tileMutations); }
}
