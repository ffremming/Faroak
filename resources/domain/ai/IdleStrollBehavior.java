package resources.domain.ai;

import java.util.Random;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.player.Moveable;
import resources.geometry.Vector;

/**
 * Calm wander for NPCs: picks a cardinal heading, strolls that way for a while,
 * then idles before picking again. Drives the host's velocity (instead of
 * teleporting world coords) so {@link Moveable}'s controller handles collision
 * and the directional animation lines up with the motion.
 */
public final class IdleStrollBehavior implements AIBehavior {

    private final Random rng;
    private final double stepMagnitude;
    private final int    strollTicks;
    private final int    idleTicks;

    private double dx, dy;
    private int    cooldown;
    private boolean idling;

    public IdleStrollBehavior(long seed) {
        this(seed, 0.6, 120, 90);
    }

    public IdleStrollBehavior(long seed, double stepMagnitude,
                              int strollTicks, int idleTicks) {
        this.rng           = new Random(seed);
        this.stepMagnitude = stepMagnitude;
        this.strollTicks   = Math.max(1, strollTicks);
        this.idleTicks     = Math.max(1, idleTicks);
    }

    @Override
    public void tick(BaseEntity host, GameContext ctx) {
        if (!(host instanceof Moveable)) return;
        Moveable mob = (Moveable) host;

        if (cooldown-- > 0) {
            if (!idling) mob.addVelocity(new Vector(dx, dy));
            return;
        }

        idling = !idling;
        if (idling) {
            cooldown = idleTicks;
            dx = dy = 0;
            return;
        }

        switch (rng.nextInt(4)) {
            case 0: dx =  stepMagnitude; dy = 0; break;
            case 1: dx = -stepMagnitude; dy = 0; break;
            case 2: dx = 0; dy =  stepMagnitude; break;
            default: dx = 0; dy = -stepMagnitude; break;
        }
        cooldown = strollTicks;
        mob.addVelocity(new Vector(dx, dy));
    }
}
