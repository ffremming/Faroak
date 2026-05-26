package resources.generation;

import resources.domain.entity.BaseEntity;
import resources.domain.tile.Tile;

/**
 * The per-coordinate contract used by the chunk loader. An implementation
 * decides what ground tile sits at a given world coordinate and what (if any)
 * decorative or interactive entity is placed on top of it.
 *
 * Implementations must be deterministic in {@code (worldX, worldY)} alone so
 * that re-loading a chunk reproduces the same world.
 *
 * Current implementation: {@link resources.generation.factory.EntityFactory}.
 */
public interface WorldGenerator {

    /** Ground tile for the given world coordinate. Must never return null. */
    Tile getTile(int worldX, int worldY);

    /** Optional decoration / actor on top of the tile. May return null for empty cells. */
    BaseEntity getEntity(int worldX, int worldY);
}
