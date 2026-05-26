package resources.world;

import java.util.ArrayList;

import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.tile.Tile;

/**
 * Snapshot of the entities, tiles, and chunks currently loaded in the working
 * window. Mutated by chunk-lifecycle code; read by simulation, rendering, and
 * interaction.
 *
 * Owning this as a dedicated object — rather than as fields on
 * {@link WorkingMemory} — gives every collaborator a single, focused contract
 * to depend on instead of the full god class.
 */
public final class EntityIndex {

    private final ArrayList<Chunk>  chunks          = new ArrayList<>();
    private final ArrayList<Entity> entities        = new ArrayList<>();
    private final ArrayList<Tile>   tiles           = new ArrayList<>();
    private final ArrayList<Entity> sortedVisible   = new ArrayList<>();
    private final ArrayList<BaseEntity> removalQueue = new ArrayList<>();

    public ArrayList<Chunk>  chunks()        { return chunks; }
    public ArrayList<Entity> entities()      { return entities; }
    public ArrayList<Tile>   tiles()         { return tiles; }
    public ArrayList<Entity> sortedVisible() { return sortedVisible; }
    public ArrayList<BaseEntity> removalQueue() { return removalQueue; }

    public void setChunks(ArrayList<Chunk> v) {
        chunks.clear(); chunks.addAll(v);
    }

    public void setEntities(ArrayList<Entity> v) {
        entities.clear(); entities.addAll(v);
    }

    public void setTiles(ArrayList<Tile> v) {
        tiles.clear(); tiles.addAll(v);
    }

    public void setSortedVisible(ArrayList<Entity> v) {
        sortedVisible.clear(); sortedVisible.addAll(v);
    }

    /** Flatten the per-chunk tile grids into a single list. */
    public ArrayList<Tile> harvestTilesFromChunks() {
        ArrayList<Tile> out = new ArrayList<>();
        for (Chunk chunk : chunks) {
            for (int i = 0; i < chunk.tiles.length; i++) {
                for (int j = 0; j < chunk.tiles.length; j++) {
                    out.add(chunk.tiles[i][j]);
                }
            }
        }
        return out;
    }
}
