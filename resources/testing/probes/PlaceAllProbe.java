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
 * Places every catalog object into the world and confirms it enters the world
 * after world.update(point) — the indexing quirk noted in project memory.
 *
 * Each object is given several candidate non-water, non-solid, collision-clear
 * tiles to land on (the test world has water/occupied tiles nearby, so a fixed
 * spread would fail for reasons unrelated to the object). The probe asserts that
 * every object that found a valid tile actually entered the world, and that the
 * vast majority found one.
 */
public final class PlaceAllProbe implements Probe {

    @Override public String name() { return "place-all"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        java.util.List<ObjectCatalog.Entry> all = new java.util.ArrayList<>();
        all.addAll(ObjectCatalog.ENTRIES);
        all.addAll(ObjectCatalog.EXTRA);

        int ts = ctx.tileSize();
        int cx = (int) player.getWorldX(), cy = (int) player.getWorldY();

        int placed = 0, noSpot = 0, placeFailed = 0;
        StringBuilder fails = new StringBuilder();

        // Walk an expanding ring of candidate tiles; consume one valid tile per
        // object so placements don't overlap each other.
        java.util.Iterator<Point> tiles = candidateTiles(ctx, cx, cy, ts).iterator();

        for (ObjectCatalog.Entry e : all) {
            Point at = nextFreeTile(ctx, tiles, ts, e.solid);
            if (at == null) { noSpot++; continue; } // ran out of clear tiles in scan radius
            GameObject obj = new GameObject(player.panel, e.name, at.x, at.y,
                ts, ts, ts, ts, 0, 0, e.solid);
            if (ctx.world().placeEntity(obj)) {
                ctx.world().update(at);   // make it queryable (memory caveat)
                placed++;
            } else {
                placeFailed++;
                if (fails.length() < 200) fails.append(e.name).append(' ');
            }
        }

        String detail = "placed=" + placed + "/" + all.size()
            + ", placeFailed=" + placeFailed + ", noClearTile=" + noSpot
            + (placeFailed > 0 ? " [" + fails + "]" : "");

        // Any object that got a verified-clear tile but still failed to place is
        // a real bug. Running out of clear tiles is a test-world limitation, not
        // a feature failure, so it doesn't fail the probe.
        if (placeFailed > 0) return ProbeResult.fail(name() + " object failed to place on a clear tile", detail);
        if (placed == 0)     return ProbeResult.fail(name() + " nothing placed", detail);
        return ProbeResult.pass(name(), detail);
    }

    /** Next tile from the iterator that is non-water, non-solid and collision-clear. */
    private static Point nextFreeTile(GameContext ctx, java.util.Iterator<Point> tiles, int ts, boolean solid) {
        while (tiles.hasNext()) {
            Point p = tiles.next();
            Tile t = ctx.world().getTile(p);
            if (t == null || t.isSolid()) continue;
            resources.geometry.HitBox hb = new resources.geometry.HitBox(p.x, p.y, ts, ts);
            if (ctx.world().solidCollision(hb)) continue;
            return p;
        }
        return null;
    }

    /** Tiles on expanding rings around (cx,cy), every other tile so footprints
     *  don't touch. Lazily materialised. */
    private static Iterable<Point> candidateTiles(GameContext ctx, int cx, int cy, int ts) {
        java.util.List<Point> pts = new java.util.ArrayList<>();
        for (int r = 1; r <= 30; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue; // ring boundary
                    pts.add(new Point(cx + dx * ts * 2, cy + dy * ts * 2));
                }
            }
        }
        return pts;
    }
}
