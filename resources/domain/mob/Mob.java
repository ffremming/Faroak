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
 * Override {@link #getImages()} to pull sprites via {@code getObjectImages},
 * since the playable-directory loader won't find mob sprites.
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

    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> out = new ArrayList<>();
        ArrayList<BufferedImage> src = panel.imageContainer.getObjectImages(getName());
        if (src != null && !src.isEmpty()) out.add(src.get(0));
        return out;
    }
}
