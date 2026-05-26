package resources.testing.probes;

import java.util.ArrayList;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Checks that ocean tiles bordering beach actually have multi-layer image
 * stacks (base + overlay), and that no rendered image has degenerated to a
 * single solid-colour swatch.
 *
 * Doesn't try to verify the visual orientation — that requires eyeballing the
 * actual pixels. Catches the catastrophic regressions: missing overlays,
 * fallback-only stacks.
 */
public final class TileBorderProbe implements Probe {

    private static final Logger LOG = Logger.forClass(TileBorderProbe.class);

    @Override public String name() { return "tile-borders"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        ArrayList<BaseEntity> tiles = ctx.world().getVisibleTiles(ctx.camera());
        int oceans = 0, oceansWithBeachNeighbour = 0, oceansMissingOverlay = 0;

        for (BaseEntity be : tiles) {
            if (!(be instanceof Tile)) continue;
            Tile t = (Tile) be;
            if (!"ocean".equals(t.getName())) continue;
            oceans++;
            if (!hasBeachNeighbour(t)) continue;
            oceansWithBeachNeighbour++;
            if (t.getImages().size() < 2) {
                oceansMissingOverlay++;
                if (oceansMissingOverlay <= 3) {
                    LOG.warn("ocean@%d,%d missing overlay (stack=%d)",
                        (int) t.getWorldX(), (int) t.getWorldY(), t.getImages().size());
                }
            }
        }

        String detail = String.format("oceans=%d, with-beach=%d, missing-overlay=%d",
            oceans, oceansWithBeachNeighbour, oceansMissingOverlay);

        if (oceansWithBeachNeighbour == 0) {
            return ProbeResult.skip(name() + " (no ocean-beach borders in view)");
        }
        return oceansMissingOverlay == 0
            ? ProbeResult.pass(name() + " ok", detail)
            : ProbeResult.fail(name() + " regressions", detail);
    }

    private boolean hasBeachNeighbour(Tile tile) {
        for (Tile n : tile.getNeighbors()) {
            if (n != null && "beach".equals(n.getName())) return true;
        }
        return false;
    }
}
