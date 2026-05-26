package resources.testing.probes;

import java.awt.Point;
import java.util.LinkedHashSet;
import java.util.Set;

import resources.app.GameContext;
import resources.core.event.DimensionChangeEvent;
import resources.domain.tile.Tile;
import resources.generation.dimension.DimensionRegistry;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies that switching to the cave dimension produces a world made of
 * cave_* tiles. Sample a handful of points near origin and assert each tile's
 * name belongs to the cave palette {cave_stone, cave_wall, cave_dirt}.
 *
 * Always switches back to OVERWORLD before returning so downstream probes
 * (especially {@code DimensionProbe}) see the unperturbed home dimension.
 */
public final class CaveProbe implements Probe {

    private static final Logger LOG = Logger.forClass(CaveProbe.class);

    private static final Set<String> EXPECTED = caveTiles();
    private static final int[][] SAMPLE_OFFSETS = {
        {  0,    0}, { 64,   0}, {-64,   0}, {  0,  64}, {  0, -64},
        {128,  64}, {-128, 64}, { 64, 128}, {-64,-128}, {192,   0},
    };

    @Override public String name() { return "cave"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.dimensions() == null) return ProbeResult.skip(name() + " no DimensionService");

        try {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.OVERWORLD, DimensionRegistry.CAVE, new Point(0, 0)));

            if (!DimensionRegistry.CAVE.equals(ctx.dimensions().currentDimension())) {
                return ProbeResult.fail(name() + " did not switch to cave");
            }

            int sampled = 0;
            int matched = 0;
            Set<String> seen = new LinkedHashSet<>();
            for (int[] o : SAMPLE_OFFSETS) {
                Tile t = ctx.world().getTile(new Point(o[0], o[1]));
                if (t == null) continue;
                sampled++;
                String name = t.getName();
                seen.add(name);
                if (EXPECTED.contains(name)) matched++;
            }

            String detail = String.format("sampled=%d, matched=%d, names=%s",
                sampled, matched, seen);
            LOG.info(detail);

            if (sampled == 0)        return ProbeResult.fail(name() + " no tiles sampled", detail);
            if (matched < sampled)   return ProbeResult.fail(name() + " non-cave tile encountered", detail);
            return ProbeResult.pass(name(), detail);
        } finally {
            // Always return to overworld so subsequent probes see a stable state.
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.CAVE, DimensionRegistry.OVERWORLD, new Point(0, 0)));
        }
    }

    private static Set<String> caveTiles() {
        Set<String> s = new LinkedHashSet<>();
        s.add("cave_stone");
        s.add("cave_dirt");
        // Walls are CaveWallTile instances whose base sprite key is
        // "rockCliff0". CaveWallTile may swap to other rockCliff variants per
        // neighbour bitmask, but getName() reflects the construction name.
        s.add("rockCliff0");
        return s;
    }
}
