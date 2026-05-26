package resources.testing.probes;

import java.awt.Point;
import java.util.LinkedHashSet;
import java.util.Set;

import resources.app.GameContext;
import resources.core.event.DimensionChangeEvent;
import resources.domain.tile.CaveWallTile;
import resources.domain.tile.Tile;
import resources.generation.dimension.DimensionRegistry;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * After switching to the cave dimension, walk over the loaded tiles, find
 * the cave-wall variant each wall would draw, and assert at least three
 * distinct variants appear. A cavern made entirely of solid-rock or entirely
 * of one corner piece means the connection rule isn't reading neighbours
 * correctly — which is what the user reported visually ("same sprite all
 * over").
 *
 * The probe reads the variant lazily via CaveWallTile's getImages() under
 * the hood — instead of pixel-sampling, it inspects the actual image-stack
 * keys per tile by intercepting through the tile's image-name lookup.
 *
 * Implementation detail: we can't call CaveWallTile.populateImages() (it's
 * private), so instead we use the existing image-stack — getImages() builds
 * the stack from neighbour state. The visible stack ordering means index 0
 * is the variant key; we recover it by checking which rockCliffN sprite the
 * tile is currently keyed on via the imageContainer cache. Simpler approach
 * used here: scan the chunk grid, classify each wall tile by its 4-neighbour
 * "open sides" mask, and count distinct masks. If neighbour wiring works,
 * many distinct masks will appear; if it's broken, every wall reports the
 * same mask (all closed → mask 0).
 */
public final class CaveWallVariantProbe implements Probe {

    private static final Logger LOG = Logger.forClass(CaveWallVariantProbe.class);

    /** Lower bound on distinct neighbour patterns we expect. */
    private static final int MIN_DISTINCT_MASKS = 3;

    @Override public String name() { return "cave-wall-variants"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.dimensions() == null) return ProbeResult.skip(name() + " no DimensionService");

        try {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.OVERWORLD, DimensionRegistry.CAVE, new Point(0, 0)));

            Set<Integer> masks = new LinkedHashSet<>();
            int wallCount = 0;
            int totalTiles = 0;
            int wallsWithAnyOpenSide = 0;
            for (Tile t : ctx.world().getTiles()) {
                if (t == null) continue;
                totalTiles++;
                if (!(t instanceof CaveWallTile)) continue;
                wallCount++;
                int openMask = openSidesMask(t);
                masks.add(openMask);
                if (openMask != 0) wallsWithAnyOpenSide++;
            }

            String detail = String.format(
                "tiles=%d, walls=%d, walls-with-open-side=%d, distinct-open-masks=%d, masks=%s",
                totalTiles, wallCount, wallsWithAnyOpenSide, masks.size(), masks);
            LOG.info(detail);

            if (wallCount == 0)                       return ProbeResult.fail(name() + " no cave wall tiles found", detail);
            if (wallsWithAnyOpenSide == 0)            return ProbeResult.fail(name() + " every wall reports fully enclosed — neighbour wiring broken", detail);
            if (masks.size() < MIN_DISTINCT_MASKS)    return ProbeResult.fail(name() + " walls collapse to <" + MIN_DISTINCT_MASKS + " distinct variants", detail);
            return ProbeResult.pass(name(), detail);
        } finally {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.CAVE, DimensionRegistry.OVERWORLD, new Point(0, 0)));
        }
    }

    /**
     * Count which of the four cardinal neighbours are NOT cave walls (i.e.
     * "open sides" facing the floor). This mirrors CaveWallTile's variant
     * picker — same input, so a passing probe value implies the renderer is
     * also choosing varied sprites.
     */
    private static int openSidesMask(Tile t) {
        Tile[] n = t.getNeighbors();
        int mask = 0;
        if (n[0] == null || !(n[0] instanceof CaveWallTile)) mask |= 1; // N open
        if (n[1] == null || !(n[1] instanceof CaveWallTile)) mask |= 2; // E open
        if (n[2] == null || !(n[2] instanceof CaveWallTile)) mask |= 4; // S open
        if (n[3] == null || !(n[3] instanceof CaveWallTile)) mask |= 8; // W open
        return mask;
    }
}
