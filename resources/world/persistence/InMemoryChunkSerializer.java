package resources.world.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import resources.domain.entity.Entity;
import resources.world.Chunk;

/**
 * Reference implementation of {@link ChunkSerializer}: snapshots live in a
 * process-local map keyed by chunk coordinates. Validates the memento
 * contract end-to-end (snapshot → mutate → restore → original state) without
 * coupling tests to disk I/O.
 *
 * Swap for a disk-backed variant in production save flows.
 */
public final class InMemoryChunkSerializer implements ChunkSerializer {

    private final Map<Long, ChunkSnapshot> store = new HashMap<>();

    @Override
    public ChunkSnapshot snapshot(Chunk chunk) {
        ChunkSnapshot snap = new ChunkSnapshot(
            chunk.x, chunk.y,
            chunk.tiles,
            new ArrayList<Entity>(chunk.getEntities()));
        store.put(key(chunk.x, chunk.y), snap);
        return snap;
    }

    @Override
    public void restore(Chunk chunk, ChunkSnapshot snapshot) {
        chunk.getEntities().clear();
        chunk.getEntities().addAll(snapshot.entities());
    }

    public ChunkSnapshot get(int x, int y) {
        return store.get(key(x, y));
    }

    private static long key(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xFFFFFFFFL);
    }
}
