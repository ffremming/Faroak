package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.core.event.DimensionChangeEvent;
import resources.domain.tile.Tile;
import resources.generation.dimension.DimensionRegistry;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the interior dimension generator places the expected tile palette
 * at known coordinates: a wood-floor cell at the room centre, the south door
 * cell, and a perimeter wall on the east side.
 *
 * Always switches back to OVERWORLD before returning so the dimension probe
 * (which runs last) sees a clean baseline.
 */
public final class InteriorProbe implements Probe {

    private static final Logger LOG = Logger.forClass(InteriorProbe.class);

    private static final String FLOOR_NAME = "floor_wood";
    private static final String WALL_NAME  = "wall_indoor";
    private static final int    TILE       = 64;

    @Override public String name() { return "interior"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.dimensions() == null) return ProbeResult.skip(name() + " no DimensionService");

        try {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.OVERWORLD, DimensionRegistry.INTERIOR, new Point(0, 0)));

            if (!DimensionRegistry.INTERIOR.equals(ctx.dimensions().currentDimension())) {
                return ProbeResult.fail(name() + " did not switch to interior");
            }

            String center = nameAt(ctx, 0, 0);
            String door   = nameAt(ctx, 0, 4 * TILE);
            String wall   = nameAt(ctx, 4 * TILE, 0);

            String detail = String.format("center=%s, door=%s, wall=%s", center, door, wall);
            LOG.info(detail);

            if (!FLOOR_NAME.equals(center)) return ProbeResult.fail(name() + " centre is not floor", detail);
            if (!FLOOR_NAME.equals(door))   return ProbeResult.fail(name() + " door cell is not floor", detail);
            if (!WALL_NAME.equals(wall))    return ProbeResult.fail(name() + " east wall not wall_indoor", detail);
            return ProbeResult.pass(name(), detail);
        } finally {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.INTERIOR, DimensionRegistry.OVERWORLD, new Point(0, 0)));
        }
    }

    private static String nameAt(GameContext ctx, int x, int y) {
        Tile t = ctx.world().getTile(new Point(x, y));
        return t == null ? null : t.getName();
    }
}
