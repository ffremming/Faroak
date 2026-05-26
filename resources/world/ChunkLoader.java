package resources.world;

import resources.domain.entity.BaseEntity;
import resources.domain.tile.Tile;
import resources.world.persistence.ChunkSerializer;
import resources.world.persistence.ChunkSnapshot;

/**
 * Generates tiles + entities for a {@link Chunk} the first time it loads,
 * and reloads tiles on subsequent loads.
 *
 * Extracted so the chunk class doesn't have to know about the generation
 * pipeline. New strategies (persisted-only, mock for tests, network-streamed)
 * become alternate implementations of this loader.
 */
public final class ChunkLoader {

    private final Chunk chunk;

    public ChunkLoader(Chunk chunk) {
        this.chunk = chunk;
    }

    /** First-time load: generate tiles and entities, mark chunk generated. */
    public void initialLoad() {
        generateTiles();
        generateEntities();
        chunk.markGenerated();
    }

    /**
     * Re-load: tiles regenerate deterministically; entities are restored from
     * the chunk system's serializer if a snapshot exists, otherwise left empty
     * (persisted state wins over re-generation on a previously-loaded chunk).
     */
    public void repeatedLoad() {
        generateTiles();
        ChunkSerializer serializer = chunk.chunkS.serializer();
        if (serializer == null) return;
        ChunkSnapshot snapshot = serializer.get(chunk.x, chunk.y);
        if (snapshot != null) serializer.restore(chunk, snapshot);
    }

    private void generateTiles() {
        int tileSize = chunk.chunkS.panel.tileSize;
        for (int x2 = 0; x2 < chunk.width; x2 += tileSize) {
            for (int y2 = 0; y2 < chunk.width; y2 += tileSize) {
                Tile tile = chunk.chunkS.entityFactory.getTile(chunk.x + x2, chunk.y + y2);
                if (tile != null) chunk.addEntity(tile);
            }
        }
    }

    private void generateEntities() {
        int tileSize = chunk.chunkS.panel.tileSize;
        for (int x2 = 0; x2 < chunk.width; x2 += tileSize) {
            for (int y2 = 0; y2 < chunk.width; y2 += tileSize) {
                BaseEntity entity = chunk.chunkS.entityFactory.getEntity(chunk.x + x2, chunk.y + y2);
                if (entity != null) chunk.addEntity(entity);
            }
        }
    }
}
