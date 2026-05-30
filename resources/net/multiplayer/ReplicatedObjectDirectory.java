package resources.net.multiplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.object.Boat;
import resources.domain.object.GameObject;
import resources.net.multiplayer.message.WorldObjectStateMessage;

/**
 * Applies authoritative world-object snapshots to local render entities.
 */
final class ReplicatedObjectDirectory {

    private static final int FALLBACK_SIZE = 64;

    private final GameContext ctx;
    private final Map<Long, BaseEntity> byObjectId = new HashMap<>();

    ReplicatedObjectDirectory(GameContext ctx) {
        this.ctx = ctx;
    }

    void applySnapshot(boolean baseline, List<WorldObjectStateMessage> worldObjects) {
        if (worldObjects == null) return;
        Set<Long> seen = baseline ? new HashSet<>() : null;
        for (WorldObjectStateMessage state : worldObjects) {
            if (state == null) continue;
            if (baseline) seen.add(state.objectId());
            if (state.removed()) {
                remove(state.objectId());
            } else {
                upsert(state);
            }
        }
        if (baseline) removeMissing(seen);
    }

    private void upsert(WorldObjectStateMessage state) {
        BaseEntity entity = byObjectId.get(state.objectId());
        if (entity == null) {
            entity = spawn(state);
            if (entity == null) return;
            positionCentered(entity, state.worldX(), state.worldY());
            if (!ctx.world().placeEntity(entity)) return;
            byObjectId.put(state.objectId(), entity);
            return;
        }
        positionCentered(entity, state.worldX(), state.worldY());
    }

    private BaseEntity spawn(WorldObjectStateMessage state) {
        GamePanel panel = panel();
        if (panel == null) return null;
        String objectType = sanitizeType(state.objectType());

        BaseEntity template = null;
        if (ctx.items() != null && !objectType.isBlank()) {
            template = ctx.items().getPhysicalRepresentation(objectType);
        }

        if (template instanceof GameObject) {
            GameObject placed = ((GameObject) template).placementCandidate(panel);
            placed.getHitBox().updateCoords();
            return placed;
        }
        if (template instanceof Boat) {
            Boat boat = new Boat(panel, 0, 0, false);
            boat.getHitBox().updateCoords();
            return boat;
        }

        return new GameObject(panel, objectType.isBlank() ? "block" : objectType, 0, 0,
            FALLBACK_SIZE, FALLBACK_SIZE, FALLBACK_SIZE, FALLBACK_SIZE, 0, 0, true);
    }

    private void remove(long objectId) {
        BaseEntity existing = byObjectId.remove(objectId);
        if (existing != null) ctx.world().removeEntity(existing);
    }

    private void removeMissing(Set<Long> seen) {
        ArrayList<Long> toRemove = new ArrayList<>();
        for (Long objectId : byObjectId.keySet()) {
            if (!seen.contains(objectId)) toRemove.add(objectId);
        }
        for (Long objectId : toRemove) remove(objectId.longValue());
    }

    private void positionCentered(BaseEntity entity, double centerX, double centerY) {
        if (entity == null) return;
        entity.setWorldX(centerX - (entity.getWidth() / 2.0));
        entity.setWorldY(centerY - (entity.getHeight() / 2.0));
        if (entity.getHitBox() != null) entity.getHitBox().updateCoords();
    }

    private GamePanel panel() {
        if (ctx instanceof GamePanel) return (GamePanel) ctx;
        if (ctx.player() != null) return ctx.player().panel;
        return null;
    }

    private String sanitizeType(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase();
        if (value.length() > 64) value = value.substring(0, 64);
        return value;
    }

    int size() {
        return byObjectId.size();
    }
}
