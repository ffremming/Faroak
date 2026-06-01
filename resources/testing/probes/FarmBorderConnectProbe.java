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
 * Verifies that tilling a second tile next to an existing {@link FarmTile}
 * connects the two beds <em>mutually</em>: the first tile must drop its border
 * on the side now facing soil, not keep drawing an edge against a grass tile
 * that is no longer there. This is the regression guard for the bug where a
 * placed FarmTile only wired its own borders and never refreshed its
 * neighbours, so an existing tile kept a stale edge toward a freshly tilled
 * neighbour.
 */
public final class FarmBorderConnectProbe implements Probe {

    private static final Logger LOG = Logger.forClass(FarmBorderConnectProbe.class);

    @Override public String name() { return "farm-border-connect"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        Point base = findTillablePairOrigin(ctx, player);
        if (base == null) return ProbeResult.skip(name() + " no two adjacent tillable tiles found");
        int ts = ctx.tileSize();
        Point east = new Point(base.x + ts, base.y);

        // Till the first tile in isolation and measure its border stack.
        FarmTile first = ctx.world().tillTileAt(base);
        if (first == null) return ProbeResult.skip(name() + " could not till first tile");
        ctx.world().update(base);
        int beforeStack = first.getImages().size();

        // Till the eastern neighbour. The fix must refresh `first` so it drops
        // its east edge; without it the stack stays the same.
        FarmTile second = ctx.world().tillTileAt(east);
        if (second == null) return ProbeResult.skip(name() + " could not till neighbour tile");
        ctx.world().update(east);
        int afterStack = first.getImages().size();

        // The first tile's east neighbour must now resolve to the new FarmTile.
        Tile eastNeighbor = first.getNeighbors()[1]; // index 1 == EAST
        boolean wired = eastNeighbor instanceof FarmTile;

        String detail = String.format("before=%d after=%d eastIsFarm=%b", beforeStack, afterStack, wired);
        LOG.info(detail);

        if (!wired) {
            return ProbeResult.fail(name() + " first tile's east neighbour not re-wired to FarmTile", detail);
        }
        if (afterStack >= beforeStack) {
            return ProbeResult.fail(name() + " first tile kept stale border after neighbour tilled", detail);
        }
        return ProbeResult.pass(name(), detail);
    }

    /**
     * Find a tile that is tillable and whose eastern neighbour is also tillable,
     * relocating the player onto it. Returns the origin tile's world point.
     */
    private static Point findTillablePairOrigin(GameContext ctx, Playable player) {
        int ts = ctx.tileSize();
        int cx = (int) player.getWorldX();
        int cy = (int) player.getWorldY();
        for (int r = 0; r <= 10; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    Point p = new Point(cx + dx * ts, cy + dy * ts);
                    Point e = new Point(p.x + ts, p.y);
                    Tile t = ctx.world().getTile(p);
                    Tile te = ctx.world().getTile(e);
                    if (t == null || te == null) continue;
                    if (TileRules.isTillable(t.getName()) && TileRules.isTillable(te.getName())) {
                        player.setWorldX(p.x);
                        player.setWorldY(p.y);
                        player.resetInteractionHitBox();
                        ctx.world().update(player.getPoint());
                        return p;
                    }
                }
            }
        }
        return null;
    }
}
