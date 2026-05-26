package resources.world;

import java.awt.Point;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.Boat;
import resources.domain.object.GameObject;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;
import resources.net.event.PlaceEntityIntentEvent;
import resources.net.event.RemoveEntityIntentEvent;

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
    // BaseEntity to allow Boat (Moveable) previews alongside GameObject ones.
    private BaseEntity previewObject;
    private String previewSourceName;
    private boolean previewValid = true;

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
        if (entity == null) return false;
        if (!panel.authority().canPlace(entity)) return false;
        PlaceEntityIntentEvent intent = new PlaceEntityIntentEvent(
            entity.getName(), entity.getPoint());
        if (!panel.authority().authorize(intent)) return false;
        if (solidCollision(entity.getHitBox())) return false;
        // If the target tile is outside the loaded chunk bounds, refuse the
        // placement so the caller can roll back item consumption. Previously
        // we ignored the chunk-system result and the item was silently lost.
        if (!chunkSystem.addEntity(entity)) return false;
        panel.events().publish(intent);
        return true;
    }

    public void removeEntity(BaseEntity entity) {
        if (entity == null) return;
        if (!panel.authority().canRemove(entity)) return;
        RemoveEntityIntentEvent intent = new RemoveEntityIntentEvent(
            entity.getName(), entity.getPoint());
        if (!panel.authority().authorize(intent)) return;
        chunkSystem.removeEntity(entity);
        panel.events().publish(intent);
    }

    /** Maximum world-space distance (px) between the player's center and the
     *  mouse cursor at which placement is allowed. Wide enough to cover any
     *  spot on the visible screen but still bounded so a player can't place
     *  off-camera. */
    private static final double PLACEMENT_REACH_PX = 12 * 64;

    public boolean tryPlaceEntity(Stack equipped) {
        if (equipped == null || equipped.isEmpty()) return false;
        BaseEntity item = equipped.getItem();
        if (item == null) return false;
        if (!withinPlacementReach()) return false;
        BaseEntity representation = ((Item) item).getPhysicalRepresentation();
        if (representation instanceof Boat) {
            return tryPlaceBoat((Boat) representation, equipped);
        }
        if (!(representation instanceof GameObject)) return false;
        GameObject source = (GameObject) representation;
        GameObject placed = source.placementCandidate(panel);
        placed.setWorldX(panel.mouse.getMouseWorldX() - placed.getWidth()  / 2);
        placed.setWorldY(panel.mouse.getMouseWorldY() - placed.getHeight() / 2);
        if (solidCollision(placed.getHitBox())) return false;
        if (!placeEntity(placed)) return false;
        equipped.removeOneItem();
        return true;
    }

    private boolean withinPlacementReach() {
        if (panel.player == null) return true; // no player → no gate
        double px = panel.player.getWorldX() + panel.player.getWidth()  / 2.0;
        double py = panel.player.getWorldY() + panel.player.getHeight() / 2.0;
        double dx = panel.mouse.getMouseWorldX() - px;
        double dy = panel.mouse.getMouseWorldY() - py;
        return (dx * dx + dy * dy) <= (PLACEMENT_REACH_PX * PLACEMENT_REACH_PX);
    }

    /**
     * Drop a fresh Boat at the mouse position. Boats aren't GameObjects so they
     * skip the regular placementCandidate flow; we build a new instance,
     * require every hitbox corner to land on water, and reject otherwise.
     */
    private boolean tryPlaceBoat(Boat source, Stack equipped) {
        int x = (int) (panel.mouse.getMouseWorldX() - source.getWidth()  / 2.0);
        int y = (int) (panel.mouse.getMouseWorldY() - source.getHeight() / 2.0);
        // Player-placed boats sit still until boarded. AI patrol re-attaches
        // automatically on dismount via Boat.dismount().
        Boat placed = new Boat(panel, x, y, false);
        if (!isAllWater(placed.getHitBox())) return false;
        // Don't use solidCollision here — water tiles are marked solid (to
        // block the player from walking on water), so it would always reject
        // every spot of ocean. We only care that no *entity* (another boat,
        // a mob) occupies the spot.
        if (entityCollision(placed.getHitBox())) return false;
        if (!placeEntityNoSolidCheck(placed)) return false;
        equipped.removeOneItem();
        return true;
    }

    /** Same as solidCollision, but ignores Tile entities — used for placeables
     *  whose allowed surface is gated separately (e.g. boats on water). */
    private boolean entityCollision(HitBox hitbox) {
        for (BaseEntity be : index.entities()) {
            if (!be.isSolid()) continue;
            if (be.getHitBox() == hitbox) continue;
            if (hitbox.collision(be.getHitBox())) return true;
        }
        return false;
    }

    /** placeEntity but skipping the world's blanket solid-collision check; the
     *  caller has already validated placement via a tile/entity rule of its own. */
    private boolean placeEntityNoSolidCheck(BaseEntity entity) {
        if (entity == null) return false;
        if (!panel.authority().canPlace(entity)) return false;
        resources.net.event.PlaceEntityIntentEvent intent =
            new resources.net.event.PlaceEntityIntentEvent(entity.getName(), entity.getPoint());
        if (!panel.authority().authorize(intent)) return false;
        if (!chunkSystem.addEntity(entity)) return false;
        panel.events().publish(intent);
        return true;
    }

    private boolean isAllWater(resources.geometry.HitBox hb) {
        int[] xs = { hb.x, hb.x + hb.width - 1 };
        int[] ys = { hb.y, hb.y + hb.height - 1 };
        for (int x : xs) {
            for (int y : ys) {
                Tile t = tileAt(new java.awt.Point(x, y));
                if (t == null) return false;
                String n = t.getName();
                if (!("ocean".equals(n) || "river".equals(n))) return false;
            }
        }
        return true;
    }

    public void addObjectPreview(Stack equipped) {
        if (equipped == null || equipped.getItem() == null) {
            clearPreviewObject();
            return;
        }
        BaseEntity representation = equipped.getItem().getPhysicalRepresentation();
        if (representation instanceof Boat) {
            addBoatPreview((Boat) representation);
            return;
        }
        if (!(representation instanceof GameObject)) {
            clearPreviewObject();
            return;
        }
        GameObject source = (GameObject) representation;
        refreshPreviewObject(source);

        previewObject.setWorldX(panel.mouse.getMouseWorldX() - previewObject.getWidth() / 2);
        previewObject.setWorldY(panel.mouse.getMouseWorldY() - previewObject.getHeight() / 2);
        previewValid = withinPlacementReach()
                    && !solidCollision(previewObject.getHitBox());
        if (panel.camera != null) {
            panel.camera.setPreviewObject(previewObject);
            panel.camera.setPreviewValid(previewValid);
        }
    }

    /**
     * Track-the-mouse ghost for boat placement. Reuses the boat representation
     * directly rather than cloning under a ",preview" name — Boat's images
     * come from the artist-provided ship folder, not the standard
     * objects/<name>/ structure the preview-suffix mechanism relies on, so
     * we draw it at full opacity and rely on the invalid-overlay tint to
     * communicate "can't place here". The preview's hitbox is checked against
     * water + solid-collision so the user can see at a glance whether the
     * click will succeed.
     */
    private void addBoatPreview(Boat source) {
        if (previewObject == null || !"boat".equals(previewSourceName)) {
            clearPreviewObject();
            // Preview boat must never tick or patrol; it's only a sprite.
            previewObject = new Boat(panel, 0, 0, false);
            previewSourceName = "boat";
        }
        double w = previewObject.getWidth();
        double h = previewObject.getHeight();
        previewObject.setWorldX(panel.mouse.getMouseWorldX() - w / 2.0);
        previewObject.setWorldY(panel.mouse.getMouseWorldY() - h / 2.0);
        previewObject.getHitBox().updateCoords();
        // Mirror tryPlaceBoat exactly: water-only surface + no entity overlap.
        // Do NOT call solidCollision here — water tiles are solid.
        previewValid = withinPlacementReach()
                    && isAllWater(previewObject.getHitBox())
                    && !entityCollision(previewObject.getHitBox());
        if (panel.camera != null) {
            panel.camera.setPreviewObject(previewObject);
            panel.camera.setPreviewValid(previewValid);
        }
    }

    /** Whether the current preview's hitbox is free of solid collisions. */
    public boolean isPreviewValid() { return previewValid; }

    private void refreshPreviewObject(GameObject source) {
        String sourceName = source.getName();
        if (previewObject != null && sourceName.equals(previewSourceName)) return;
        clearPreviewObject();
        previewObject = source.getPreviewObject(panel);
        previewSourceName = sourceName;
    }

    private void clearPreviewObject() {
        previewObject = null;
        previewSourceName = null;
        if (panel.camera != null) panel.camera.setPreviewObject(null);
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
            if (!panel.authority().canRemove(ent)) continue;
            RemoveEntityIntentEvent intent = new RemoveEntityIntentEvent(
                ent.getName(), ent.getPoint());
            if (!panel.authority().authorize(intent)) continue;
            index.sortedVisible().remove(ent);
            chunkSystem.removeEntity(ent);
            panel.events().publish(intent);
        }
        index.removalQueue().clear();
    }
}
