package resources.testing.probes;

import resources.app.GameContext;
import resources.domain.entity.component.GrowableComponent;
import resources.domain.farming.Crop;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Pins the crop growth pace to "day-paced": a freshly planted {@link Crop}
 * must advance one stage per in-game day, not per few-hundred ticks. We assert
 * the crop is still at stage 0 after ticking a budget far smaller than one
 * in-game day — under the old 600-ticks-per-stage cadence it would already have
 * advanced several stages.
 */
public final class CropGrowthPaceProbe implements Probe {

    private static final Logger LOG = Logger.forClass(CropGrowthPaceProbe.class);

    @Override public String name() { return "crop-growth-pace"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();

        int px = (int) ctx.player().getWorldX() + 96;
        int py = (int) ctx.player().getWorldY();
        Crop crop = new Crop(ctx.player().panel, "crop_emberwheat", px, py);
        if (!ctx.world().placeEntity(crop)) {
            return ProbeResult.skip(name() + " could not place crop");
        }
        ctx.world().update(ctx.player().getPoint());

        long ticksPerDay = ctx.clock().ticksPerDay();
        // A budget well under a day: a day-paced crop stays at stage 0 here.
        int budget = (int) Math.min(ticksPerDay / 4, 5_000);
        harness.tick(budget);

        GrowableComponent g = crop.getComponent(GrowableComponent.class);
        int stage = g == null ? -1 : g.currentStage();

        String detail = String.format(
            "ticks=%d, ticksPerDay=%d, stage-after=%d", budget, ticksPerDay, stage);
        LOG.info(detail);

        ctx.world().removeEntity(crop);

        if (stage != 0) {
            return ProbeResult.fail(name() + " crop advanced before one in-game day (not day-paced)", detail);
        }
        return ProbeResult.pass(name(), detail);
    }
}
