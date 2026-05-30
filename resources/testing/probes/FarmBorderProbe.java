package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.farming.FarmTile;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;
import resources.world.placement.TileRules;

/**
 * Verifies that a freshly tilled {@link FarmTile} surrounded by non-farm
 * terrain renders a multi-layer image stack: the soil base plus procedural
 * "mud" border overlays on the sides that face non-farm neighbours. Without the
 * soil-plot border rule the FarmTile draws a single bare soil sprite (stack
 * size 1) and reads as having no edge.
 */
public final class FarmBorderProbe implements Probe {

    private static final Logger LOG = Logger.forClass(FarmBorderProbe.class);

    @Override public String name() { return "farm-borders"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        if (!relocateToTillable(ctx, player)) {
            return ProbeResult.skip(name() + " no tillable tile within scan radius");
        }
        Point at = player.getPoint();
        FarmTile tile = ctx.world().tillTileAt(at);
        if (tile == null) return ProbeResult.skip(name() + " could not till tile under player");
        ctx.world().update(at);
        tile.setNeighBors();

        int stack = tile.getImages().size();
        // A lone tilled tile with all-grass neighbours should carry the soil
        // base plus a border on every non-farm side (up to 4) + corners.
        String detail = String.format("image-stack=%d", stack);
        LOG.info(detail);

        if (stack < 2) {
            return ProbeResult.fail(name() + " FarmTile has no border overlay (bare soil only)", detail);
        }
        return ProbeResult.pass(name(), detail);
    }

    private static boolean relocateToTillable(GameContext ctx, Playable player) {
        int ts = ctx.tileSize();
        int cx = (int) player.getWorldX();
        int cy = (int) player.getWorldY();
        for (int r = 0; r <= 10; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    Point p = new Point(cx + dx * ts, cy + dy * ts);
                    Tile t = ctx.world().getTile(p);
                    if (t == null) continue;
                    if (TileRules.isTillable(t.getName())) {
                        player.setWorldX(p.x);
                        player.setWorldY(p.y);
                        player.resetInteractionHitBox();
                        ctx.world().update(player.getPoint());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
