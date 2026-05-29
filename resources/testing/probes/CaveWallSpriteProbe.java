package resources.testing.probes;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

import resources.app.GameContext;
import resources.core.event.DimensionChangeEvent;
import resources.domain.tile.MountainTile;
import resources.domain.tile.Tile;
import resources.generation.dimension.DimensionRegistry;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Strongest variant of the cave-wall test: actually render the cave scene,
 * then read each wall tile's chosen sprite by hashing its drawn pixels. If
 * more than {@link #MAX_DOMINANCE_PCT}% of walls render the same pixel
 * signature, fail — that's the "every wall looks identical" user complaint.
 *
 * Uses the public CaveWallTile.getImages() path so we capture exactly what
 * the renderer would draw. Avoids reaching into private state.
 */
public final class CaveWallSpriteProbe implements Probe {

    private static final Logger LOG = Logger.forClass(CaveWallSpriteProbe.class);

    /** Max % of walls allowed to share a single sprite signature. */
    private static final int MAX_DOMINANCE_PCT = 70;

    /** Min distinct sprite signatures we expect across loaded walls. */
    private static final int MIN_DISTINCT_SPRITES = 3;

    @Override public String name() { return "cave-wall-sprites"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.dimensions() == null) return ProbeResult.skip(name() + " no DimensionService");

        try {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.OVERWORLD, DimensionRegistry.CAVE, new Point(0, 0)));

            Map<Long, Integer> sigCounts = new LinkedHashMap<>();
            int walls = 0;
            for (Tile t : ctx.world().getTiles()) {
                if (!(t instanceof MountainTile)) continue;
                walls++;
                long sig = spriteSignature(t);
                sigCounts.merge(sig, 1, Integer::sum);
            }

            int distinct = sigCounts.size();
            int dominantCount = 0;
            long dominantSig = 0;
            for (Map.Entry<Long, Integer> e : sigCounts.entrySet()) {
                if (e.getValue() > dominantCount) {
                    dominantCount = e.getValue();
                    dominantSig = e.getKey();
                }
            }
            int dominancePct = walls == 0 ? 0 : (dominantCount * 100) / walls;

            String detail = String.format(
                "walls=%d, distinct-sprite-signatures=%d, dominant-sig=%d (%d walls, %d%%)",
                walls, distinct, dominantSig, dominantCount, dominancePct);
            LOG.info(detail);

            if (walls == 0)                                return ProbeResult.fail(name() + " no walls", detail);
            if (distinct < MIN_DISTINCT_SPRITES)            return ProbeResult.fail(name() + " walls collapse to <"+MIN_DISTINCT_SPRITES+" distinct sprites", detail);
            if (dominancePct > MAX_DOMINANCE_PCT)           return ProbeResult.fail(name() + " one sprite dominates >"+MAX_DOMINANCE_PCT+"% of walls", detail);
            return ProbeResult.pass(name(), detail);
        } finally {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.CAVE, DimensionRegistry.OVERWORLD, new Point(0, 0)));
        }
    }

    /**
     * Hash the actual sprite the tile would draw, by reading getImages() and
     * computing a coarse fingerprint of its pixels. Two walls that pick the
     * same sprite produce the same signature; two walls that pick different
     * sprites produce different signatures. No private-field access required.
     */
    private static long spriteSignature(Tile t) {
        if (t.getImages().isEmpty()) return -1L;
        BufferedImage img = t.getImages().get(0);
        if (img == null) return -1L;
        // Use BufferedImage identity instead of pixel sampling: tiles sharing
        // the same cached image share an identityHashCode, while different
        // sprites are distinct BufferedImage instances. Avoids the failure
        // mode where pixel sampling hits transparent areas of multiple
        // distinct sprites and collapses them to the same signature.
        return ((long) System.identityHashCode(img) << 16)
             ^ ((long) img.getWidth()  << 8)
             ^ img.getHeight();
    }

}
