package resources.domain.ai;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;

/**
 * Steps the host toward the target entity at {@code stepPixels} per tick.
 * Stops when within {@code engageDistance} (caller decides what happens then
 * — typically swaps in an attack behavior).
 *
 * Doesn't pathfind; that's a separate concern handled by the existing
 * path system on Moveable. Use a higher-level behavior to compose them
 * when obstacles matter.
 */
public final class ChaseBehavior implements AIBehavior {

    private final BaseEntity target;
    private final int        stepPixels;
    private final int        engageDistance;

    public ChaseBehavior(BaseEntity target, int stepPixels, int engageDistance) {
        this.target         = target;
        this.stepPixels     = stepPixels;
        this.engageDistance = engageDistance;
    }

    @Override
    public void tick(BaseEntity host, GameContext ctx) {
        if (target == null) return;
        double dx = target.getWorldX() - host.getWorldX();
        double dy = target.getWorldY() - host.getWorldY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= engageDistance || dist == 0) return;
        host.setWorldX(host.getWorldX() + (dx / dist) * stepPixels);
        host.setWorldY(host.getWorldY() + (dy / dist) * stepPixels);
    }

    public BaseEntity target()         { return target; }
    public int        engageDistance() { return engageDistance; }
}
