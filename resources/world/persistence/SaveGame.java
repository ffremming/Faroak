package resources.world.persistence;

import java.util.HashMap;
import java.util.Map;

import resources.core.id.Identifier;
import resources.domain.inventory.Inventory;

/**
 * Aggregate of everything needed to recreate a session: per-dimension chunk
 * snapshots, the player's inventory, and the clock tick at capture.
 *
 * Skeleton — captures the data structure the rest of the persistence layer
 * will fill in. Disk format / file location concerns belong elsewhere.
 */
public final class SaveGame {

    private final Map<Identifier, Map<Long, ChunkSnapshot>> dimensions = new HashMap<>();
    private final Inventory inventory;
    private final long      capturedAtTick;

    public SaveGame(Inventory inventory, long capturedAtTick) {
        this.inventory      = inventory;
        this.capturedAtTick = capturedAtTick;
    }

    public void addChunk(Identifier dim, long key, ChunkSnapshot snapshot) {
        dimensions.computeIfAbsent(dim, k -> new HashMap<>()).put(key, snapshot);
    }

    public Map<Long, ChunkSnapshot> chunksFor(Identifier dim) {
        return dimensions.getOrDefault(dim, new HashMap<>());
    }

    public Inventory inventory()      { return inventory; }
    public long      capturedAtTick() { return capturedAtTick; }
    public int       dimensionCount() { return dimensions.size(); }
}
