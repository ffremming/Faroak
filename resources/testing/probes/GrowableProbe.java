package resources.testing.probes;

import resources.app.GameContext;
import resources.domain.entity.component.GrowableComponent;
import resources.domain.object.GameObject;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Attaches a tick-paced {@link GrowableComponent} to a fresh GameObject,
 * ticks the world forward, and verifies the stage advances through every
 * value up to maturity, in order, exactly once each.
 *
 * Catches regressions where the component is attached but never ticked, or
 * where the tick logic clamps incorrectly at the final stage.
 */
public final class GrowableProbe implements Probe {

    private static final Logger LOG = Logger.forClass(GrowableProbe.class);

    private static final int  STAGES         = 4;
    private static final long TICKS_PER_STAGE = 5;
    private static final int  TICK_BUDGET    = (int) (STAGES * TICKS_PER_STAGE + 5);

    @Override public String name() { return "growable"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();

        int px = (int) ctx.player().getWorldX() + 96;
        int py = (int) ctx.player().getWorldY();
        GameObject crop = new GameObject(ctx.player().panel, "wildGrass",
            px, py, 64, 64, 32, 32, 0, 0, false);

        GrowableComponent growable =
            GrowableComponent.perTicks(ctx.clock(), STAGES, TICKS_PER_STAGE);
        crop.components().add(growable);

        if (!ctx.world().placeEntity(crop)) {
            return ProbeResult.fail(name() + " could not place crop");
        }
        ctx.world().update(ctx.player().getPoint());

        boolean[] reached = new boolean[STAGES];
        int    lastStage  = growable.currentStage();
        reached[lastStage] = true;

        for (int i = 0; i < TICK_BUDGET; i++) {
            harness.tick(1);
            int s = growable.currentStage();
            if (s != lastStage) {
                if (s != lastStage + 1) {
                    return ProbeResult.fail(name() + " stage skipped",
                        String.format("prev=%d, curr=%d", lastStage, s));
                }
                reached[s] = true;
                lastStage = s;
            }
            if (growable.isMature()) break;
        }
        ctx.world().removeEntity(crop);

        int missed = 0;
        for (boolean r : reached) if (!r) missed++;
        String detail = String.format("stages=%d, final=%d, missed=%d, mature=%s",
            STAGES, growable.currentStage(), missed, growable.isMature());
        LOG.info(detail);
        if (missed > 0)             return ProbeResult.fail(name() + " missed stages", detail);
        if (!growable.isMature())   return ProbeResult.fail(name() + " did not mature", detail);
        return ProbeResult.pass(name(), detail);
    }
}
