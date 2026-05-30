package resources.world;

import java.awt.Point;
import java.util.ArrayList;

import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;

/**
 * Collision queries over the world's entities and chunk tiles, extracted from
 * {@link WorldInteraction} so the coordinator stays focused on placement /
 * preview / removal. Pure read-only queries: it only reads from the supplied
 * {@link EntityIndex}, never mutating world state.
 */
final class CollisionChecker {

    private final EntityIndex index;

    CollisionChecker(EntityIndex index) {
        this.index = index;
    }

    boolean solidCollision(HitBox hitbox) {
        return solidCollision(hitbox, null);
    }

    /** Solid-collision check that excludes {@code mover} from the candidate set.
     *  Use when testing a hypothetical hitbox (e.g. "can this NPC step forward?")
     *  whose underlying entity would otherwise collide with itself — the
     *  candidate is a freshly-allocated HitBox so the reference-equality skip
     *  in {@link #entitiesCollidedWith(HitBox)} doesn't catch it. */
    boolean solidCollision(HitBox hitbox, BaseEntity mover) {
        for (BaseEntity be : entitiesCollidedWith(hitbox)) {
            if (be == mover) continue;
            if (be.isSolid()) return true;
        }
        return false;
    }

    ArrayList<BaseEntity> entitiesCollidedWith(HitBox hitBox) {
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

    ArrayList<BaseEntity> entitiesCollidedWith(Point p) {
        ArrayList<BaseEntity> collided = new ArrayList<>();
        for (BaseEntity be : index.entities()) {
            if (be.getHitBox().collision(p)) collided.add(be);
        }
        for (Chunk chunk : index.chunks()) {
            if (chunk.collision(p)) collided.add(chunk.getTile(p));
        }
        return collided;
    }

    /** Same as solidCollision, but ignores Tile entities — used for placeables
     *  whose allowed surface is gated separately (e.g. boats on water). */
    boolean entityCollision(HitBox hitbox) {
        for (BaseEntity be : index.entities()) {
            if (!be.isSolid()) continue;
            if (be.getHitBox() == hitbox) continue;
            if (hitbox.collision(be.getHitBox())) return true;
        }
        return false;
    }
}
