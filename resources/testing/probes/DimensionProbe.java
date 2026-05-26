package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.core.event.DimensionChangeEvent;
import resources.generation.dimension.DimensionRegistry;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;
import resources.world.ChunkSystem;

/**
 * Verifies the dimension-switching plumbing end-to-end:
 *   - publishing a DimensionChangeEvent flips the active chunk system,
 *   - the player is teleported to the event's arrival point,
 *   - switching back to the original dimension reuses the cached system.
 *
 * Catches regressions where the event bus loses the subscription, where the
 * working memory holds onto its old chunk system, or where the player
 * position update is dropped.
 */
public final class DimensionProbe implements Probe {

    private static final Logger LOG = Logger.forClass(DimensionProbe.class);

    @Override public String name() { return "dimension"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.dimensions() == null) return ProbeResult.fail(name() + " no DimensionService");

        ChunkSystem before = ctx.dimensions().currentDimension() == null
            ? null
            : harness.panel().world.chunkSystem();

        Point caveSpawn = new Point(0, 0);
        ctx.events().publish(new DimensionChangeEvent(
            DimensionRegistry.OVERWORLD, DimensionRegistry.CAVE, caveSpawn));

        ChunkSystem afterCave = harness.panel().world.chunkSystem();
        boolean playerMoved =
            (int) ctx.player().getWorldX() == caveSpawn.x
         && (int) ctx.player().getWorldY() == caveSpawn.y;
        boolean dimChanged  = !DimensionRegistry.OVERWORLD.equals(ctx.dimensions().currentDimension());
        boolean systemSwapped = afterCave != before;

        // Round-trip: caching test.
        ctx.events().publish(new DimensionChangeEvent(
            DimensionRegistry.CAVE, DimensionRegistry.OVERWORLD, new Point(0, 0)));
        ChunkSystem afterReturn = harness.panel().world.chunkSystem();
        boolean returned = afterReturn == before;

        String detail = String.format(
            "system-swapped=%s, player-moved=%s, dim-changed=%s, returned-to-cached=%s",
            systemSwapped, playerMoved, dimChanged, returned);
        LOG.info(detail);

        if (!systemSwapped) return ProbeResult.fail(name() + " chunk system not swapped", detail);
        if (!playerMoved)   return ProbeResult.fail(name() + " player not teleported", detail);
        if (!dimChanged)    return ProbeResult.fail(name() + " current dimension unchanged", detail);
        if (!returned)      return ProbeResult.fail(name() + " return trip rebuilt instead of cached", detail);
        return ProbeResult.pass(name(), detail);
    }
}
