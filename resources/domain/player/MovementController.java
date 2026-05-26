package resources.domain.player;

import resources.domain.entity.component.TerrainSpeedComponent;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;
import resources.geometry.Vector;

/**
 * Applies velocity / path to a {@link Moveable}, honouring solid collisions
 * via the world. Owning the movement loop here keeps Moveable focused on
 * state + sprite selection.
 *
 * The strategy is intentionally simple (velocity → step → collide test → apply)
 * so terrain-aware variants (boats, slowed/iced terrain) can compose by
 * decorating this rather than re-implementing the loop.
 */
public final class MovementController {

    private final Moveable owner;

    public MovementController(Moveable owner) {
        this.owner = owner;
    }

    /** One simulation step of movement. */
    public void step() {
        owner.resetInteractionHitBox();
        Vector step = nextStep();
        owner.getDirection().set(step.x, step.y);

        if (canMoveTo(owner.getHitboxInfront())) {
            owner.moveBy(step.x, step.y);
        }

        consumeReachedWaypoint();
    }

    private Vector nextStep() {
        double speed = owner.getMovementSpeed() * terrainMultiplier();
        if (owner.path.isEmpty()) {
            return owner.getVelocity().transfer(speed * 10);
        }
        return owner.path.get(0).transfer(speed * 2);
    }

    private double terrainMultiplier() {
        TerrainSpeedComponent comp = owner.getComponent(TerrainSpeedComponent.class);
        if (comp == null) return 1.0;
        Tile tile = owner.panel.world.getTile(owner.getPoint());
        return comp.multiplierFor(tile == null ? null : tile.getName());
    }

    private void consumeReachedWaypoint() {
        if (!owner.path.isEmpty() && owner.path.get(0).hasNoVelocity()) {
            owner.path.remove(0);
        }
    }

    private boolean canMoveTo(HitBox candidate) {
        return !owner.panel.world.solidCollision(candidate, owner);
    }
}
