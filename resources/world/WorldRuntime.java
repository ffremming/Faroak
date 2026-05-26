package resources.world;

import java.awt.Point;
import java.util.ArrayList;

import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;
import resources.presentation.camera.Camera;

/**
 * The simulation-facing contract for the world. {@link WorkingMemory} is the
 * current implementation. Subsystems that just need to query/place/sort/render
 * entities should depend on this interface; only the bootstrap layer should
 * touch WorkingMemory's concrete chunk-management internals.
 *
 * Keeping the interface narrow makes it easy to introduce alternative worlds
 * (mocked for tests, multi-dimension, replay) without dragging chunk plumbing
 * along.
 */
public interface WorldRuntime {

    /** Bring chunks + working set up to date for the given world coordinate. */
    void update(Point worldPoint);

    /** Tick non-tile entities for one logical frame. */
    void simulate();

    /** Step animation frames for entities and animated tiles. */
    void animate(int frame);

    // ---- queries ----
    ArrayList<Tile> getTiles();
    ArrayList<Entity> getEntities();
    ArrayList<Chunk> getChunks();
    ArrayList<Entity> getVisibleEntities(Camera camera);
    ArrayList<BaseEntity> getVisibleTiles(Camera camera);
    Tile getTile(Point worldPoint);
    BaseEntity getHoveredEntity();
    void setHoveredEntity(int screenX, int screenY);

    // ---- placement / collision ----
    boolean placeEntity(BaseEntity entity);
    void removeEntity(BaseEntity entity);
    boolean solidCollision(HitBox hitbox);
    ArrayList<BaseEntity> getEntitiesCollidedWith(HitBox hitbox);
    ArrayList<BaseEntity> getEntitiesCollidedWith(Point worldPoint);
    void addToRemovalQueue(BaseEntity entity);
}
