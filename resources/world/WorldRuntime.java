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

    // ---- farming (tile-layer) ----
    /** Hoe the tile under the point into a {@link resources.domain.farming.FarmTile}
     *  in place; returns it (or an existing one), or null if not tillable. */
    resources.domain.farming.FarmTile tillTileAt(Point worldPoint);
    /** The {@link resources.domain.farming.FarmTile} under the point, or null. */
    resources.domain.farming.FarmTile farmTileAt(Point worldPoint);

    // ---- placement / collision ----
    boolean placeEntity(BaseEntity entity);
    /**
     * Insert an entity from an authoritative external source (e.g. multiplayer
     * snapshot) without re-validating local placement rules.
     */
    boolean placeEntityAuthoritative(BaseEntity entity);
    /**
     * Insert a transient entity (projectiles/VFX) without the blanket
     * solid-collision gate used by {@link #placeEntity(BaseEntity)}.
     * Implementations may reject non-transient entities.
     */
    boolean placeEntityIgnoringTerrainCollision(BaseEntity entity);
    /**
     * Place a boat/ship on water: validates that its whole hull sits on water
     * and no other entity occupies the spot, then inserts it bypassing the
     * blanket solid-collision gate (water tiles are marked solid). The single
     * supported spawn path for both player-placed boats and NPC ships.
     */
    boolean placeShipOnWater(resources.domain.object.Boat ship);
    void removeEntity(BaseEntity entity);
    boolean solidCollision(HitBox hitbox);
    boolean solidCollision(HitBox hitbox, BaseEntity mover);
    ArrayList<BaseEntity> getEntitiesCollidedWith(HitBox hitbox);
    ArrayList<BaseEntity> getEntitiesCollidedWith(Point worldPoint);
    void addToRemovalQueue(BaseEntity entity);
}
