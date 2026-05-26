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
 */
public final class EntityVisibility {

    private final EntityIndex index;

    public EntityVisibility(EntityIndex index) {
        this.index = index;
    }

    public ArrayList<Entity> visibleEntities(Camera camera) {
        ArrayList<Entity> out = new ArrayList<>();
        if (camera == null) return out;
        HitBox camHB = camera.getImageHitbox();
        for (Entity e : index.sortedVisible()) {
            if (e.getImageHitbox().collision(camHB) || e.getHitBox().collision(camHB)) {
                // Always added below — keeping the test for future culling parity.
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
