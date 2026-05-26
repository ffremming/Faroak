package resources.world.persistence;

import java.util.ArrayList;
import java.util.List;

import resources.domain.entity.Entity;
import resources.domain.tile.Tile;

/**
 * Frozen, immutable copy of a {@link resources.world.Chunk}'s state at the
 * moment of capture: tile grid + entity bag. Memento pattern — the chunk
 * decides when to capture / restore; the snapshot itself is dumb storage.
 *
 * Kept structural rather than serialised: a future on-disk codec can convert
 * this to/from bytes without snapshots having to know about file formats.
 */
public final class ChunkSnapshot {

    private final int   chunkX;
    private final int   chunkY;
    private final Tile[][] tiles;
    private final List<Entity> entities;

    public ChunkSnapshot(int chunkX, int chunkY, Tile[][] tiles, List<Entity> entities) {
        this.chunkX   = chunkX;
        this.chunkY   = chunkY;
        this.tiles    = tiles;
        this.entities = new ArrayList<>(entities);
    }

    public int          chunkX()   { return chunkX; }
    public int          chunkY()   { return chunkY; }
    public Tile[][]     tiles()    { return tiles; }
    public List<Entity> entities() { return entities; }
}
