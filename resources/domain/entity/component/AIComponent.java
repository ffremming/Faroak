package resources.domain.entity.component;

import resources.app.GameContext;
import resources.domain.ai.AIBehavior;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Tickable;

/**
 * Hosts the {@link AIBehavior} currently driving an entity. Forwards each
 * tick into the behavior; the component carries no decision state of its own
 * — the behavior holds it.
 *
 * Why a component rather than a subclass: a wandering deer and a wandering
 * goblin share zero gameplay code apart from "wanders," and we want to swap
 * behavior at runtime (peaceful → fleeing → aggressive) without juggling
 * class hierarchies.
 */
public final class AIComponent implements EntityComponent, Tickable {

    private final GameContext ctx;
    private AIBehavior behavior;
    private BaseEntity host;

    public AIComponent(GameContext ctx, AIBehavior behavior) {
        this.ctx      = ctx;
        this.behavior = behavior;
    }

    @Override public void onAttach(BaseEntity owner) { this.host = owner; }
    @Override public void onDetach(BaseEntity owner) { this.host = null; }

    @Override
    public void update() {
        if (host == null || behavior == null) return;
        behavior.tick(host, ctx);
    }

    public AIBehavior behavior()                 { return behavior; }
    public void setBehavior(AIBehavior behavior) { this.behavior = behavior; }
}
