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
        // Copy the OUTER + INNER tile arrays so adding/removing rows or
        // columns to the live chunk does not retroactively edit the snapshot.
        // Individual Tile objects are intentionally still shared by reference
        // (Tile state-changes — e.g. farmland watering — mutate the Tile in
        // place, and we want both views to stay in sync until a real "rewind
        // to checkpoint" is implemented).
        resources.domain.tile.Tile[][] tilesCopy =
            new resources.domain.tile.Tile[chunk.tiles.length][];
        for (int i = 0; i < chunk.tiles.length; i++) {
            tilesCopy[i] = chunk.tiles[i] == null
                ? null
                : chunk.tiles[i].clone();
        }
        ChunkSnapshot snap = new ChunkSnapshot(
            chunk.x, chunk.y,
            tilesCopy,
            new ArrayList<Entity>(chunk.getEntities()));
        store.put(key(chunk.x, chunk.y), snap);
        return snap;
    }

    @Override
    public void restore(Chunk chunk, ChunkSnapshot snapshot) {
        // Restore both halves of the chunk state. Tiles were not previously
        // written back, so any structural change to the grid (added/removed
        // tiles) would survive a "restore". `chunk.tiles` is final so we
        // copy element-wise rather than reassigning the reference.
        resources.domain.tile.Tile[][] snapTiles = snapshot.tiles();
        if (snapTiles != null) {
            int rows = Math.min(snapTiles.length, chunk.tiles.length);
            for (int i = 0; i < rows; i++) {
                if (snapTiles[i] == null || chunk.tiles[i] == null) continue;
                int cols = Math.min(snapTiles[i].length, chunk.tiles[i].length);
                System.arraycopy(snapTiles[i], 0, chunk.tiles[i], 0, cols);
            }
        }
        chunk.getEntities().clear();
        chunk.getEntities().addAll(snapshot.entities());
    }

    @Override
    public ChunkSnapshot get(int x, int y) {
        return store.get(key(x, y));
    }

    private static long key(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xFFFFFFFFL);
    }
}
