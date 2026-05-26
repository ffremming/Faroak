package resources.generation.interior;

import resources.domain.entity.BaseEntity;
import resources.domain.tile.Tile;
import resources.generation.WorldGenerator;

/**
 * Per-tile/per-entity generator for the interior dimension. All layout
 * decisions live in {@link InteriorManager} — this class is a thin adapter
 * that forwards {@link WorldGenerator} queries.
 *
 * No procedural generation: the manager looks up a hand-authored
 * {@link Interior} from {@link InteriorRegistry} for the slot containing the
 * queried world coordinate.
 */
public final class InteriorGenerator implements WorldGenerator {

    private final InteriorManager manager;

    public InteriorGenerator(InteriorManager manager) {
        this.manager = manager;
        InteriorTileTypes.bootstrap();
    }

    @Override
    public Tile getTile(int worldX, int worldY) {
        return manager.tileAt(worldX, worldY);
    }

    @Override
    public BaseEntity getEntity(int worldX, int worldY) {
        return manager.entityAt(worldX, worldY);
    }
}
