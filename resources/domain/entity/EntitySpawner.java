package resources.domain.entity;

import java.util.function.Function;

import resources.app.GameContext;
import resources.domain.entity.component.EntityComponent;
import resources.domain.object.GameObject;

/**
 * Centralises construction of entities from an {@link EntityType} definition.
 * Replaces dozens of ad-hoc {@code new GameObject(panel, "name", x, y, w, h, ...)}
 * calls scattered through the generation/placement code.
 *
 * Responsibilities:
 *   1. Instantiate the right base class (currently GameObject; AI mobs later).
 *   2. Run every component factory declared on the EntityType so the spawned
 *      entity carries the right behaviours (light, growth, harvestable, …).
 *
 * Keeping spawning behind one call lets us swap the underlying base class,
 * add event publication (entity-spawned), pool instances, or rebind
 * dependencies without touching callers.
 */
public final class EntitySpawner {

    private final GameContext context;

    public EntitySpawner(GameContext context) {
        this.context = context;
    }

    /** Spawn an instance of {@code type} at the given world coordinate. */
    public BaseEntity spawn(EntityType type, int worldX, int worldY) {
        GameObject obj = new GameObject(
            (resources.app.GamePanel) context, // panel still threaded through legacy ctors
            type.spriteName(),
            worldX, worldY,
            type.width(), type.height(),
            type.hitBoxWidth(), type.hitBoxHeight(),
            0, 0,
            type.solid());
        attachComponents(obj, type);
        return obj;
    }

    private void attachComponents(BaseEntity entity, EntityType type) {
        for (Function<GameContext, EntityComponent> factory : type.componentFactories()) {
            EntityComponent component = factory.apply(context);
            if (component != null) entity.addComponent(component);
        }
    }
}
