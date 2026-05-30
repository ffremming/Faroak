package resources.world;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.net.event.RemoveEntityIntentEvent;

/**
 * Entity removal — immediate and deferred (queued) — extracted from
 * {@link WorldInteraction}. Wraps the removal-intent authorization, chunk
 * removal, and the per-tick removal-queue drain so the coordinator only
 * delegates. Behaviour is identical to the inlined version.
 */
final class EntityRemovalQueue {

    private final EntityIndex index;
    private final ChunkSystem chunkSystem;
    private final GamePanel panel;

    EntityRemovalQueue(EntityIndex index, ChunkSystem chunkSystem, GamePanel panel) {
        this.index = index;
        this.chunkSystem = chunkSystem;
        this.panel = panel;
    }

    void removeEntity(BaseEntity entity) {
        if (entity == null) return;
        if (!panel.authority().canRemove(entity)) return;
        RemoveEntityIntentEvent intent = new RemoveEntityIntentEvent(
            entity.getName(), entity.getPoint());
        if (!panel.authority().authorize(intent)) return;
        chunkSystem.removeEntity(entity);
        panel.events().publish(intent);
    }

    void addToRemovalQueue(BaseEntity entity) {
        index.removalQueue().add(entity);
    }

    void clearRemovalQueue() {
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
