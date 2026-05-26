package resources.domain.player;

import resources.app.GamePanel;
import resources.domain.ai.AIBehavior;
import resources.domain.entity.Tickable;
import resources.domain.entity.component.AIComponent;
import resources.domain.entity.component.EntityComponent;
import resources.domain.entity.component.HealthComponent;

/**
 * Non-player character. Reuses {@link Moveable}'s directional sprite + collision
 * pipeline (so NPCs look and pathfind like the player) and hangs an
 * {@link AIBehavior} off an {@link AIComponent} for movement decisions.
 *
 * Sprites load from {@code resources/images/playable/<name>/} using the same
 * 12-frame layout as the player — the behavior nudges velocity, and
 * Moveable's controller produces both motion and the matching animation frame.
 *
 * Moveable.update() doesn't call super.update(), so component tickers
 * (AIComponent, HealthComponent) won't fire unless we tick them here first.
 */
public class Npc extends Moveable {

    public Npc(GamePanel panel, String spriteName, int worldX, int worldY,
               int maxHealth, AIBehavior behavior) {
        super(panel, spriteName, worldX, worldY,
              (short) 48, (short) 96, (short) 36, (short) 32,
              (short) 6, (short) 64);
        this.solid = true;
        setMovementSpeed(0.4);
        addComponent(new HealthComponent(maxHealth));
        addComponent(new AIComponent(panel, behavior));
    }

    @Override
    public void update() {
        for (EntityComponent c : components().all()) {
            if (c instanceof Tickable) ((Tickable) c).update();
        }
        super.update();
    }
}
