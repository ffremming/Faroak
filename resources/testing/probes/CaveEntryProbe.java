package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.core.event.DimensionChangeEvent;
import resources.domain.entity.Entity;
import resources.domain.player.Playable;
import resources.generation.dimension.DimensionRegistry;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Regression for the "player disappears when entering a cave" bug. After a
 * dimension swap, the player must:
 *   - sit at the arrival point,
 *   - be present in the entity index (so simulate/render can find them),
 *   - have at least one loaded chunk adjacent to the arrival point (no
 *     standing-in-a-void).
 *
 * Runs before {@link DimensionProbe} so the world is in a clean state, and
 * cleans up by swapping back to overworld.
 */
public final class CaveEntryProbe implements Probe {

    private static final Logger LOG = Logger.forClass(CaveEntryProbe.class);

    @Override public String name() { return "cave-entry"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.dimensions() == null) return ProbeResult.skip(name() + " no DimensionService");
        Playable player = (Playable) ctx.player();
        if (player == null) return ProbeResult.skip(name() + " no player");

        Point arrival = new Point(0, 0);
        try {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.OVERWORLD, DimensionRegistry.CAVE, arrival));

            boolean atArrival = (int) player.getWorldX() == arrival.x
                             && (int) player.getWorldY() == arrival.y;
            boolean inIndex = entityIndexed(ctx, player);
            boolean chunkLoaded = !ctx.world().getChunks().isEmpty();

            String detail = String.format(
                "at-arrival=%s, in-index=%s, chunks-loaded=%d",
                atArrival, inIndex, ctx.world().getChunks().size());
            LOG.info(detail);

            if (!atArrival)    return ProbeResult.fail(name() + " player not at arrival point", detail);
            if (!chunkLoaded)  return ProbeResult.fail(name() + " no chunks around arrival", detail);
            if (!inIndex)      return ProbeResult.fail(name() + " player missing from entity index", detail);
            return ProbeResult.pass(name(), detail);
        } finally {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.CAVE, DimensionRegistry.OVERWORLD, new Point(0, 0)));
        }
    }

    private static boolean entityIndexed(GameContext ctx, Playable player) {
        for (Entity e : ctx.world().getEntities()) {
            if (e == player) return true;
        }
        return false;
    }
}
