package resources.domain.mob;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.ai.AIBehavior;
import resources.domain.entity.Tickable;
import resources.domain.entity.component.AIComponent;
import resources.domain.entity.component.EntityComponent;
import resources.domain.entity.component.HealthComponent;
import resources.domain.player.Moveable;

/**
 * A self-moving, damageable creature. Composes the existing {@link Moveable}
 * locomotion + animation pipeline with a {@link HealthComponent} for HP and
 * an {@link AIComponent} hosting whatever {@link AIBehavior} drives this
 * particular mob.
 *
 * Deliberately doesn't subclass anything player-specific — a Mob has no
 * inventory or input wiring; combat is delegated to behavior + combat systems.
 *
 * Sprites and the walk cycle come straight from {@link Moveable}: each mob ships
 * a 12-frame directional set under {@code resources/images/playable/<name>/}
 * (up/right/down/left × 3) loaded by the same playable loader the player uses,
 * so the directional animation is shared, not duplicated here.
 */
public class Mob extends Moveable {

    private static final short BODY_W   = 48;
    private static final short BODY_H   = 64;
    private static final short HITBOX_W = 32;
    private static final short HITBOX_H = 48;
    private static final short OFFSET_X = 0;
    private static final short OFFSET_Y = 0;

    public Mob(GamePanel panel, String name, int worldX, int worldY,
               int maxHealth, AIBehavior behavior) {
        super(panel, name, worldX, worldY,
              BODY_W, BODY_H, HITBOX_W, HITBOX_H, OFFSET_X, OFFSET_Y);
        this.solid = false;
        addComponent(new HealthComponent(maxHealth));
        addComponent(new AIComponent(panel, behavior));
    }

    public HealthComponent health() { return getComponent(HealthComponent.class); }
    public AIComponent     ai()     { return getComponent(AIComponent.class); }

    @Override
    public void update() {
        for (EntityComponent c : components().all()) {
            if (c instanceof Tickable) ((Tickable) c).update();
        }
        super.update();
    }
}
