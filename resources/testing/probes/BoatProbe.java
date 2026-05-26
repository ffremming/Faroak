package resources.testing.probes;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.object.Boat;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Smoke-tests the {@link Boat} entity end-to-end: construct one on a water
 * tile (or at origin if no water sits nearby), tick the world a few times and
 * confirm no exception escapes the AI/terrain-speed pipeline.
 *
 * The probe is environment-dependent — if no water cell is found within the
 * scan radius, we still tick the boat at origin but report a skip rather than
 * a pass so the result accurately reflects what was tested.
 */
public final class BoatProbe implements Probe {

    private static final Logger LOG = Logger.forClass(BoatProbe.class);

    private static final int SCAN_RADIUS_TILES = 16;
    private static final int TICKS_TO_RUN = 30;

    @Override public String name() { return "boat"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        List<Point> waterTiles = findWaterTiles(ctx);

        Boat boat = null;
        Point spawn = null;
        boolean onWater = false;
        for (Point p : waterTiles) {
            Boat candidate = new Boat(ctx.player().panel, p.x, p.y);
            if (ctx.world().placeEntity(candidate)) {
                boat = candidate;
                spawn = p;
                onWater = true;
                break;
            }
        }
        if (boat == null) {
            // Fall back to origin so we at least tick the AI pipeline.
            spawn = new Point(0, 0);
            boat = new Boat(ctx.player().panel, spawn.x, spawn.y);
            if (!ctx.world().placeEntity(boat)) {
                return ProbeResult.skip(name() + " could not place boat anywhere; candidates=" + waterTiles.size());
            }
        }
        ctx.world().update(ctx.player().getPoint());

        double startX = boat.getWorldX();
        double startY = boat.getWorldY();
        Throwable thrown = null;
        try {
            harness.tick(TICKS_TO_RUN);
        } catch (Throwable t) {
            thrown = t;
        }

        double drift = Math.hypot(boat.getWorldX() - startX, boat.getWorldY() - startY);
        ctx.world().removeEntity(boat);

        String detail = String.format("on-water=%s, spawn=%s, ticks=%d, drift=%.2f",
            onWater, spawn, TICKS_TO_RUN, drift);
        LOG.info(detail);

        if (thrown != null) return ProbeResult.fail(name() + " ticking threw", thrown.toString());
        if (!onWater)       return ProbeResult.skip(name() + " no water tile placeable — ticked off-water clean: " + detail);
        return ProbeResult.pass(name(), detail);
    }

    private static List<Point> findWaterTiles(GameContext ctx) {
        int ts = ctx.tileSize();
        int radius = SCAN_RADIUS_TILES * ts;
        Point center = ctx.player().getPoint();
        List<Point> out = new ArrayList<>();
        for (int dy = -radius; dy <= radius; dy += ts) {
            for (int dx = -radius; dx <= radius; dx += ts) {
                Point p = new Point(center.x + dx, center.y + dy);
                Tile t = ctx.world().getTile(p);
                if (t == null) continue;
                String name = t.getName();
                if ("ocean".equals(name) || "river".equals(name)) out.add(p);
            }
        }
        return out;
    }
}
