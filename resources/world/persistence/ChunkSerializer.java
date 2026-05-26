package resources.world.persistence;

import resources.world.Chunk;

/**
 * Snapshot / restore for a single {@link Chunk}. Implementations decide where
 * the bytes live — in-memory cache for now, disk later. Keeping the contract
 * narrow lets the rest of the codebase swap backends without touching chunk
 * lifecycle code.
 *
 * Note: restoring is structural — tiles + entities are placed back exactly
 * as captured. Anything stateful that should outlive a load/unload cycle has
 * to live on the snapshot or be re-derived from it.
 */
public interface ChunkSerializer {

    /** Capture the chunk's current state. Never mutates the chunk. */
    ChunkSnapshot snapshot(Chunk chunk);

    /** Replace the chunk's state with the snapshot's contents. */
    void restore(Chunk chunk, ChunkSnapshot snapshot);
}
