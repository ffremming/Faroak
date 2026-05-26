package resources.domain.ai;

import java.util.Random;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;

/**
 * Picks a random direction every {@code repickTicks} ticks and inches toward
 * it at {@code stepPixels} per tick. No solid-collision check yet — the
 * MovementController is the right home for that and the wander predates it.
 *
 * Stateful: holds RNG + current heading + cooldown.
 */
public final class WanderBehavior implements AIBehavior {

    private final Random rng;
    private final int    stepPixels;
    private final int    repickTicks;

    private double dx, dy;
    private int    cooldown;

    public WanderBehavior(long seed, int stepPixels, int repickTicks) {
        this.rng         = new Random(seed);
        this.stepPixels  = stepPixels;
        this.repickTicks = Math.max(1, repickTicks);
    }

    @Override
    public void tick(BaseEntity host, GameContext ctx) {
        if (cooldown-- <= 0) {
            double angle = rng.nextDouble() * Math.PI * 2;
            dx = Math.cos(angle);
            dy = Math.sin(angle);
            cooldown = repickTicks;
        }
        host.setWorldX(host.getWorldX() + dx * stepPixels);
        host.setWorldY(host.getWorldY() + dy * stepPixels);
    }
}
