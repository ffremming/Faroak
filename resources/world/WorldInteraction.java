package resources.world;

import java.awt.Point;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.GameObject;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;

/**
 * Placement, collision, hover, removal. The "what the player can do with the
 * world" surface, pulled out of {@link WorkingMemory} so the coordinator
 * doesn't pile up unrelated state.
 */
public final class WorldInteraction {

    private final EntityIndex index;
    private final ChunkSystem chunkSystem;
    private final GamePanel panel;

    private BaseEntity hoveredEntity;

    public WorldInteraction(EntityIndex index, ChunkSystem chunkSystem, GamePanel panel) {
        this.index = index;
        this.chunkSystem = chunkSystem;
        this.panel = panel;
    }

    public Tile tileAt(Point p) {
        for (Chunk chunk : index.chunks()) {
            if (chunk.contains(p)) return chunk.getTile(p);
        }
        return chunkSystem.getTile(p);
    }

    public boolean solidCollision(HitBox hitbox) {
        for (BaseEntity be : entitiesCollidedWith(hitbox)) {
            if (be.isSolid()) return true;
        }
        return false;
    }

    public ArrayList<BaseEntity> entitiesCollidedWith(HitBox hitBox) {
        ArrayList<BaseEntity> collided = new ArrayList<>();
        for (BaseEntity be : index.entities()) {
            if (hitBox.collision(be.getHitBox()) && be.getHitBox() != hitBox) {
                collided.add(be);
            }
        }
        for (Chunk chunk : index.chunks()) {
            if (chunk.collision(hitBox)) collided.addAll(chunk.getTilesCollidedWith(hitBox));
        }
        return collided;
    }

    public ArrayList<BaseEntity> entitiesCollidedWith(Point p) {
        ArrayList<BaseEntity> collided = new ArrayList<>();
        for (BaseEntity be : index.entities()) {
            if (be.getHitBox().collision(p)) collided.add(be);
        }
        for (Chunk chunk : index.chunks()) {
            if (chunk.collision(p)) collided.add(chunk.getTile(p));
        }
        return collided;
    }

    public boolean placeEntity(BaseEntity entity) {
        if (solidCollision(entity.getHitBox())) return false;
        chunkSystem.addEntity(entity);
        return true;
    }

    public void removeEntity(BaseEntity entity) {
        if (entity == null) return;
        chunkSystem.removeEntity(entity);
    }

    public boolean tryPlaceEntity(Stack equipped) {
        if (equipped == null || equipped.isEmpty()) return false;
        BaseEntity item = equipped.getItem();
        if (item == null) return false;
        BaseEntity gameObject = ((Item) item).getPhysicalRepresentation();
        if (!(gameObject instanceof GameObject)) return false;
        GameObject pr = (GameObject) gameObject;
        pr.setWorldX(panel.mouse.getMouseWorldX() - pr.getWidth()  / 2);
        pr.setWorldY(panel.mouse.getMouseWorldY() - pr.getHeight() / 2);
        if (solidCollision(pr.getHitBox())) return false;
        placeEntity(pr);
        equipped.removeOneItem();
        return true;
    }

    public void addObjectPreview(Stack equipped) {
        if (equipped.getItem() == null) return;
        GameObject source = (GameObject) equipped.getItem().getPhysicalRepresentation();
        if (source == null) return;
        GameObject preview = source.getPreviewObject(panel);
        panel.camera.setPreviewObject(preview);
        placeEntity(preview);
    }

    public void setHoveredEntity(int screenX, int screenY) {
        int worldX = (int) panel.camera.getWorldX() + screenX;
        int worldY = (int) panel.camera.getWorldY() + screenY;
        ArrayList<BaseEntity> hits = entitiesCollidedWith(new Point(worldX, worldY));
        for (BaseEntity be : hits) {
            if (hits.size() == 1) { hoveredEntity = be; return; }
            if (!(be instanceof Tile)) hoveredEntity = be;
        }
    }

    public BaseEntity getHoveredEntity() { return hoveredEntity; }

    public void setHoveredEntity(BaseEntity entity) { this.hoveredEntity = entity; }

    public void addToRemovalQueue(BaseEntity entity) {
        index.removalQueue().add(entity);
    }

    public void clearRemovalQueue() {
        for (BaseEntity ent : index.removalQueue()) {
            index.sortedVisible().remove(ent);
            chunkSystem.removeEntity(ent);
        }
        index.removalQueue().clear();
    }
}
