package resources.testing.probes;

import resources.app.GameContext;
import resources.domain.ai.ChaseBehavior;
import resources.domain.ai.WanderBehavior;
import resources.domain.entity.component.AIComponent;
import resources.domain.object.GameObject;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the AI plumbing end-to-end:
 *   - WanderBehavior advances the host's position over time.
 *   - ChaseBehavior monotonically reduces distance to the player.
 *
 * Catches regressions where the AIComponent forgets to forward ticks, or
 * where the behavior reads/writes state in the wrong place.
 */
public final class AIProbe implements Probe {

    private static final Logger LOG = Logger.forClass(AIProbe.class);

    private static final int WANDER_TICKS = 60;
    private static final int CHASE_TICKS  = 40;
    private static final int CHASE_STEP   = 4;

    @Override public String name() { return "ai"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();

        GameObject wanderer = spawnAt(ctx, "wildGrass", 800, 800);
        wanderer.components().add(new AIComponent(ctx,
            new WanderBehavior(42L, 2, 10)));
        double wanderStartX = wanderer.getWorldX();
        double wanderStartY = wanderer.getWorldY();

        GameObject chaser = spawnNearby(ctx, "wildGrass",
            (int) ctx.player().getWorldX(),
            (int) ctx.player().getWorldY(),
            300);
        chaser.components().add(new AIComponent(ctx,
            new ChaseBehavior(ctx.player(), CHASE_STEP, 16)));
        double chaseStartDist = distance(chaser, ctx);

        for (int i = 0; i < Math.max(WANDER_TICKS, CHASE_TICKS); i++) harness.tick(1);

        double wanderDrift  = Math.hypot(
            wanderer.getWorldX() - wanderStartX,
            wanderer.getWorldY() - wanderStartY);
        double chaseEndDist = distance(chaser, ctx);

        ctx.world().removeEntity(wanderer);
        ctx.world().removeEntity(chaser);

        String detail = String.format("wander-drift=%.1f, chase-start=%.1f, chase-end=%.1f",
            wanderDrift, chaseStartDist, chaseEndDist);
        LOG.info(detail);
        if (wanderDrift < 1.0)             return ProbeResult.fail(name() + " wander did not move host", detail);
        if (chaseEndDist >= chaseStartDist) return ProbeResult.fail(name() + " chase did not close distance", detail);
        return ProbeResult.pass(name(), detail);
    }

    private GameObject spawnAt(GameContext ctx, String asset, int x, int y) {
        GameObject obj = new GameObject(ctx.player().panel, asset,
            x, y, 64, 64, 32, 32, 0, 0, false);
        ctx.world().placeEntity(obj);
        ctx.world().update(ctx.player().getPoint());
        return obj;
    }

    /**
     * Try spiral offsets around (cx,cy) at the given radius until a placement
     * succeeds — most tiles in the test world are non-solid so this terminates
     * fast; the few mountain/water cells get skipped.
     */
    private GameObject spawnNearby(GameContext ctx, String asset, int cx, int cy, int radius) {
        int[] dxs = { 1, 1, 0, -1, -1, -1, 0, 1 };
        int[] dys = { 0, 1, 1,  1,  0, -1, -1, -1 };
        for (int i = 0; i < dxs.length; i++) {
            int x = cx + dxs[i] * radius;
            int y = cy + dys[i] * radius;
            GameObject obj = new GameObject(ctx.player().panel, asset,
                x, y, 64, 64, 32, 32, 0, 0, false);
            if (ctx.world().placeEntity(obj)) {
                ctx.world().update(ctx.player().getPoint());
                return obj;
            }
        }
        throw new IllegalStateException("could not place chaser near (" + cx + "," + cy + ")");
    }

    private double distance(GameObject from, GameContext ctx) {
        double dx = ctx.player().getWorldX() - from.getWorldX();
        double dy = ctx.player().getWorldY() - from.getWorldY();
        return Math.hypot(dx, dy);
    }
}
