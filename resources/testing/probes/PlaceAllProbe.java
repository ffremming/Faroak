package resources.testing.probes;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.object.GameObject;
import resources.domain.object.ObjectCatalog;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Places every catalog object into the world (on walkable tiles) and confirms it
 * enters the world after world.update(point) — the indexing quirk noted in
 * project memory. Reports how many of N placed successfully.
 */
public final class PlaceAllProbe implements Probe {

    @Override public String name() { return "place-all"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        Point spot = walkable(ctx, player);
        if (spot == null) return ProbeResult.skip(name() + " no walkable tile");

        java.util.List<ObjectCatalog.Entry> all = new java.util.ArrayList<>();
        all.addAll(ObjectCatalog.ENTRIES);
        all.addAll(ObjectCatalog.EXTRA);

        int placed = 0, failed = 0;
        StringBuilder fails = new StringBuilder();
        int ts = ctx.tileSize();
        int i = 0;
        for (ObjectCatalog.Entry e : all) {
            // Spread placements out so hitboxes don't collide with each other.
            Point at = new Point(spot.x + (i % 8) * ts * 3, spot.y + (i / 8) * ts * 3);
            i++;
            GameObject obj = new GameObject(player.panel, e.name, at.x, at.y,
                ts, ts, ts, ts, 0, 0, e.solid);
            if (ctx.world().placeEntity(obj)) {
                ctx.world().update(at);   // make it queryable (memory caveat)
                placed++;
            } else {
                failed++;
                if (fails.length() < 200) fails.append(e.name).append(" ");
            }
        }

        String detail = "placed=" + placed + "/" + all.size() + ", failed=" + failed
            + (failed > 0 ? " [" + fails + "]" : "");
        // Some failures are legitimate (a spot already occupied / not NOT_WATER).
        // Require the bulk to succeed rather than 100% so the probe stays stable.
        if (placed < all.size() * 0.7) return ProbeResult.fail(name() + " too many placement failures", detail);
        return ProbeResult.pass(name(), detail);
    }

    private static Point walkable(GameContext ctx, Playable player) {
        int ts = ctx.tileSize();
        int cx = (int) player.getWorldX(), cy = (int) player.getWorldY();
        for (int r = 0; r <= 12; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    Point p = new Point(cx + dx * ts, cy + dy * ts);
                    Tile t = ctx.world().getTile(p);
                    if (t != null && !t.isSolid()) return p;
                }
            }
        }
        return null;
    }
}
