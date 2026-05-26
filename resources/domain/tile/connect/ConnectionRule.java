package resources.domain.tile.connect;

import resources.domain.entity.BaseEntity;

/**
 * Predicate that decides whether two adjacent things should be drawn as one
 * connected piece — same cliff face, same fence run, same wall row.
 *
 * Lives separately from {@link ConnectingSprite} so a single sprite atlas can
 * be reused under different connection policies (e.g. cliffs connect to other
 * cliffs; fences also connect to walls but not to bushes).
 */
@FunctionalInterface
public interface ConnectionRule {
    boolean connects(BaseEntity self, BaseEntity neighbour);
}
