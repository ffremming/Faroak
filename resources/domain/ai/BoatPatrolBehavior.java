package resources.domain.ai;

import java.awt.Point;
import java.util.Random;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.tile.Tile;
import resources.world.placement.TileRules;

/**
 * Drifts the host along a randomly chosen heading, refusing to step onto
 * land. Each tick we sample the tile under the *next* position; if it's not
 * water we roll a new heading and try again on the following tick — keeps the
 * boat patrolling its lake/ocean cell without any pathfinding.
 *
 * Heading reroll also fires every {@code repickTicks} ticks unconditionally so
 * a long open stretch still feels like patrolling rather than a straight line.
 *
 * Stateful: holds RNG + current heading + cooldown + retry guard.
 */
public final class BoatPatrolBehavior implements AIBehavior {

    private static final double STEP_PIXELS  = 0.5;
    private static final int    REPICK_TICKS = 240;
    private static final int    RETRY_LIMIT  = 12;

    private final Random rng;
    private double dx;
    private double dy;
    private int    cooldown;

    public BoatPatrolBehavior(long seed) {
        this.rng = new Random(seed);
        rerollHeading();
        this.cooldown = REPICK_TICKS;
    }

    @Override
    public void tick(BaseEntity host, GameContext ctx) {
        if (cooldown-- <= 0) {
            rerollHeading();
            cooldown = REPICK_TICKS;
        }

        double nextX = host.getWorldX() + dx * STEP_PIXELS;
        double nextY = host.getWorldY() + dy * STEP_PIXELS;

        if (isWaterAt(ctx, (int) nextX, (int) nextY)) {
            host.setWorldX(nextX);
            host.setWorldY(nextY);
            return;
        }

        // Land ahead: search for a fresh heading that lands on water.
        for (int i = 0; i < RETRY_LIMIT; i++) {
            rerollHeading();
            double tryX = host.getWorldX() + dx * STEP_PIXELS;
            double tryY = host.getWorldY() + dy * STEP_PIXELS;
            if (isWaterAt(ctx, (int) tryX, (int) tryY)) {
                cooldown = REPICK_TICKS;
                return;
            }
        }
        // Penned in on all sides — sit tight; we'll try again next tick.
    }

    private void rerollHeading() {
        double angle = rng.nextDouble() * Math.PI * 2;
        dx = Math.cos(angle);
        dy = Math.sin(angle);
    }

    private static boolean isWaterAt(GameContext ctx, int x, int y) {
        Tile t = ctx.world().getTile(new Point(x, y));
        if (t == null) return false;
        String name = t.getName();
        return TileRules.isWater(name);
    }
}
