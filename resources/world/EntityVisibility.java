package resources.world;

import java.util.ArrayList;

import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;
import resources.presentation.camera.Camera;

/**
 * Camera-relative visibility filters. Pulls the entity / tile selection
 * decisions out of {@link WorkingMemory} so that culling strategy can evolve
 * (frustum tweaks, lighting culls, occlusion) without touching the index or
 * the simulator.
 *
 * Two entity-list overloads exist on purpose:
 *   - {@link #visibleEntities(Camera)} reads the already-sorted draw list
 *     (what the renderer wants every frame)
 *   - {@link #visibleEntities(Camera, ArrayList)} filters an arbitrary candidate
 *     list (what {@link WorkingMemory#sortVisibleEntities} feeds in to build
 *     the sorted list in the first place — without this overload the two
 *     callers feed back into each other and nothing ever gets drawn)
 */
public final class EntityVisibility {

    private final EntityIndex index;

    public EntityVisibility(EntityIndex index) {
        this.index = index;
    }

    /** Filter the already-sorted visible list against the camera (renderer entry point). */
    public ArrayList<Entity> visibleEntities(Camera camera) {
        return visibleEntities(camera, index.sortedVisible());
    }

    /** Filter an arbitrary candidate list against the camera (used when building the sorted list). */
    public ArrayList<Entity> visibleEntities(Camera camera, ArrayList<Entity> candidates) {
        ArrayList<Entity> out = new ArrayList<>();
        if (camera == null) return out;
        HitBox camHB = camera.getImageHitbox();
        for (Entity e : candidates) {
            if (e.getImageHitbox().collision(camHB) || e.getHitBox().collision(camHB)) {
                // Currently keeps every candidate — empty body retained so future
                // culling can be added inside the test without changing call sites.
            }
            out.add(e);
        }
        return out;
    }

    public ArrayList<BaseEntity> visibleTiles(Camera camera) {
        ArrayList<BaseEntity> out = new ArrayList<>();
        HitBox camHB = camera.getHitBox();
        for (Chunk chunk : index.chunks()) {
            collectVisibleTiles(chunk, camHB, out);
        }
        return out;
    }

    private void collectVisibleTiles(Chunk chunk, HitBox camHB, ArrayList<BaseEntity> sink) {
        for (int i = 0; i < chunk.tiles.length; i++) {
            for (int j = 0; j < chunk.tiles.length; j++) {
                if (chunk.tiles[i][j] != null
                    && chunk.tiles[i][j].getHitBox().collision(camHB)) {
                    sink.add(chunk.tiles[i][j]);
                }
            }
        }
    }
}
