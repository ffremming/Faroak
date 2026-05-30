package resources.world;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.entity.component.HealthComponent;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.Boat;
import resources.domain.object.GameObject;
import resources.domain.combat.TransientWorldEntity;
import resources.domain.farming.FarmTile;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;
import resources.net.event.PlaceEntityIntentEvent;
import resources.net.event.RemoveEntityIntentEvent;
import resources.world.placement.PlacementAction;
import resources.world.placement.PlacementRegistry;
import resources.world.placement.PlacementSpec;
import resources.world.placement.TileRules;

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

    /** Loaded chunk whose bounds contain the world point, or null. */
    private Chunk chunkContaining(Point p) {
        for (Chunk chunk : index.chunks()) {
            if (chunk.contains(p)) return chunk;
        }
        return null;
    }

    /**
     * Hoe the tile under {@code worldPt} in place: replace the grass {@link Tile}
     * in its chunk grid with a {@link FarmTile} and invalidate the chunk's render
     * bake so the new soil sprite shows. Returns the FarmTile on success (an
     * already-tilled FarmTile is returned as-is), or null if the tile isn't
     * tillable / not loaded.
     *
     * The change lives on the tile layer (the ground itself becomes soil) rather
     * than placing an object on top.
     */
    public FarmTile tillTileAt(Point worldPt) {
        Chunk chunk = chunkContaining(worldPt);
        if (chunk == null) return null;
        Tile tile = chunk.getTile(worldPt);
        if (tile == null) return null;
        if (tile instanceof FarmTile) return (FarmTile) tile; // already soil
        if (!TileRules.isTillable(tile.getName())) return null;

        FarmTile farm = FarmTile.from(tile);
        chunk.addTile(farm);            // overwrites the grid slot for this cell
        farm.setNeighBors();            // wire the new tile to its neighbours
        if (panel.camera != null) panel.camera.invalidateChunkBake(chunk);
        return farm;
    }

    /** The {@link FarmTile} under the point, or null if that cell isn't tilled. */
    public FarmTile farmTileAt(Point worldPt) {
        Tile t = tileAt(worldPt);
        return (t instanceof FarmTile) ? (FarmTile) t : null;
    }

    public boolean solidCollision(HitBox hitbox) {
        return solidCollision(hitbox, null);
    }

    /** Solid-collision check that excludes {@code mover} from the candidate set.
     *  Use when testing a hypothetical hitbox (e.g. "can this NPC step forward?")
     *  whose underlying entity would otherwise collide with itself — the
     *  candidate is a freshly-allocated HitBox so the reference-equality skip
     *  in {@link #entitiesCollidedWith(HitBox)} doesn't catch it. */
    public boolean solidCollision(HitBox hitbox, BaseEntity mover) {
        for (BaseEntity be : entitiesCollidedWith(hitbox)) {
            if (be == mover) continue;
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

    /**
     * Place an entity while skipping the generic solid-collision gate. Use
     * only for short-lived runtime actors (projectiles/VFX) that may occupy
     * water tiles or other solid terrain by design. Enforced via the
     * {@link TransientWorldEntity} marker.
     */
    public boolean placeEntityIgnoringTerrainCollision(BaseEntity entity) {
        if (!(entity instanceof TransientWorldEntity)) return false;
        return placeEntityNoSolidCheck(entity);
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

        // Boats keep their dedicated water-only path; the surface rule is too
        // specialised to express cleanly via SurfaceRule.
        BaseEntity representation = ((Item) item).getPhysicalRepresentation();
        if (representation instanceof Boat) {
            return tryPlaceBoat((Boat) representation, equipped);
        }

        // Everything else flows through the placement registry: factory +
        // surface rule + snap policy + click action.
        PlacementSpec spec = PlacementRegistry.get(equipped.getName());
        if (spec == null) return legacyPlaceFromRepresentation(representation, equipped);

        GameContext ctx = panel;
        double mx = panel.mouse.getMouseWorldX();
        double my = panel.mouse.getMouseWorldY();

        // Self-managing actions (farming): the action itself spawns/mutates and
        // handles any stack consumption (seeds) or leaves the tool intact
        // (hoe/watering can). We must NOT run the entity-placement pipeline or
        // decrement the stack here. Surface rule still gates the click.
        if (spec.action != PlacementAction.PLACE_ENTITY) {
            Point target = WorldCoord.snapToTile(mx, my, panel.tileSize);
            int cx = target.x + panel.tileSize / 2;
            int cy = target.y + panel.tileSize / 2;
            HitBox cell = new HitBox(target.x, target.y, panel.tileSize, panel.tileSize);
            if (!spec.surface.allows(ctx, target.x, target.y, cell)) return false;
            return spec.action.execute(ctx, panel.player, spec, new Point(cx, cy));
        }

        BaseEntity candidate = spec.factory.apply(panel);
        if (!(candidate instanceof GameObject)) return false;
        GameObject placed = (GameObject) candidate;

        int px, py;
        if (spec.snap == PlacementSpec.SnapPolicy.TILE) {
            Point snap = WorldCoord.snapToTile(mx, my, panel.tileSize);
            px = snap.x; py = snap.y;
        } else {
            px = (int) (mx - placed.getWidth()  / 2.0);
            py = (int) (my - placed.getHeight() / 2.0);
        }
        placed.setWorldX(px);
        placed.setWorldY(py);
        placed.getHitBox().updateCoords();

        if (!spec.surface.allows(ctx, px, py, placed.getHitBox())) return false;
        if (solidCollision(placed.getHitBox())) return false;
        if (!placeEntity(placed)) return false;
        equipped.removeOneItem();
        return true;
    }

    /**
     * Legacy fallback for items not in the {@link PlacementRegistry}: place
     * the equipped item's physical representation free-positioned at the
     * cursor, preserving the pre-registry behaviour. Kept so items like
     * "hammer"/"demoHouse"/"block" continue to work via their existing
     * physicalRepresentations entries without forcing every legacy item
     * into the registry up front.
     */
    private boolean legacyPlaceFromRepresentation(BaseEntity representation, Stack equipped) {
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

    /**
     * Mouse-targeted harvest: chop/mine/harvest the topmost harvestable
     * entity under the cursor (within the player's interaction reach).
     * Tool gating still applies via {@link HarvestableComponent#hit}, so
     * an axe-equipped click on a stone has no effect.
     */
    public boolean tryHarvestAtMouse(Playable player, GameContext ctx) {
        if (player == null) return false;
        int wx = (int) panel.camera.getWorldX() + panel.mouse.getX();
        int wy = (int) panel.camera.getWorldY() + panel.mouse.getY();
        BaseEntity target = harvestableAtPoint(new Point(wx, wy), player.getInteractionHitBox());
        if (target == null) return false;
        return player.harvestService().attackEntity(player, ctx, target) != null;
    }

    /**
     * Topmost harvestable entity (skipping mobs, which route via
     * CombatService) whose hitbox contains {@code worldPt} AND intersects
     * the player's {@code reach}. Walks the sorted-visible list back to
     * front so overlapping sprites resolve to the one drawn on top.
     */
    public BaseEntity harvestableAtPoint(Point worldPt, HitBox reach) {
        List<Entity> sorted = index.sortedVisible();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            BaseEntity e = sorted.get(i);
            if (!(e instanceof GameObject)) continue;
            if (e.getComponent(HarvestableComponent.class) == null) continue;
            if (e.getComponent(HealthComponent.class) != null) continue;
            if (!e.getHitBox().contains(worldPt)) continue;
            if (!e.getHitBox().intersects(reach)) continue;
            return e;
        }
        return null;
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
                if (!TileRules.isWater(n)) return false;
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

        // Items routed through the registry (seeds, plant-on-farmland, future
        // showGhost=false actions) suppress the ghost entirely. The placement
        // call site explains via its own UX (e.g. hover-highlight) what's
        // about to happen.
        PlacementSpec spec = PlacementRegistry.get(equipped.getName());
        if (spec != null && !spec.showGhost) {
            clearPreviewObject();
            return;
        }

        GameObject source = (GameObject) representation;
        refreshPreviewObject(source);

        double mx = panel.mouse.getMouseWorldX();
        double my = panel.mouse.getMouseWorldY();
        int px, py;
        if (spec != null && spec.snap == PlacementSpec.SnapPolicy.TILE) {
            Point snap = WorldCoord.snapToTile(mx, my, panel.tileSize);
            px = snap.x; py = snap.y;
        } else {
            px = (int) (mx - previewObject.getWidth()  / 2.0);
            py = (int) (my - previewObject.getHeight() / 2.0);
        }
        previewObject.setWorldX(px);
        previewObject.setWorldY(py);
        previewObject.getHitBox().updateCoords();

        boolean reachable    = withinPlacementReach();
        boolean surfaceOk    = spec == null
                            || spec.surface.allows(panel, px, py, previewObject.getHitBox());
        boolean collisionOk  = !solidCollision(previewObject.getHitBox());
        previewValid = reachable && surfaceOk && collisionOk;

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
            boolean removed = chunkSystem.removeEntity(ent);
            if (!removed) removed = removeFromLoadedChunks(ent);
            index.sortedVisible().remove(ent);
            index.entities().remove(ent);
            if (removed) {
                ent.remove(); // lifecycle hook (e.g. Crop frees its FarmTile)
                panel.events().publish(intent);
            }
        }
        index.removalQueue().clear();
    }

    /**
     * Fallback for entities whose world position moved out of their stored chunk
     * before a chunk flush ran. Removes by object identity from loaded chunks.
     */
    private boolean removeFromLoadedChunks(BaseEntity entity) {
        boolean removed = false;
        for (Chunk chunk : index.chunks()) {
            if (chunk.removeEntity(entity)) removed = true;
        }
        return removed;
    }
}
