package resources.testing.probes;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.ai.WaterNavigator;
import resources.domain.object.Boat;
import resources.domain.tile.Tile;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** Navigator should reduce distance to a reachable water waypoint and never
 *  leave water. */
public final class WaterNavigatorProbe implements Probe {
    @Override public String name() { return "water_navigator"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        List<Point> water = findWater(ctx);
        if (water.size() < 2) return ProbeResult.skip(name() + " not enough water near spawn");

        Point start = water.get(0);
        Point goal  = farthest(start, water);

        Boat boat = new Boat(ctx.player().panel, start.x, start.y);
        if (!ctx.world().placeEntity(boat)) return ProbeResult.skip(name() + " could not place boat");
        ctx.world().update(ctx.player().getPoint());

        WaterNavigator nav = new WaterNavigator(boat.kind().speed());
        double startDist = dist(boat, goal);
        boolean leftWater = false;
        for (int i = 0; i < 120; i++) {
            nav.stepToward(boat, goal, ctx);
            if (!isWater(ctx, (int) boat.getWorldX(), (int) boat.getWorldY())) { leftWater = true; break; }
        }
        double endDist = dist(boat, goal);
        ctx.world().removeEntity(boat);

        if (leftWater) return ProbeResult.fail(name(), "boat left water during navigation");
        if (endDist >= startDist) return ProbeResult.fail(name(),
            "did not approach waypoint: " + startDist + " -> " + endDist);
        return ProbeResult.pass(name(), "approached: " + (int) startDist + " -> " + (int) endDist);
    }

    private static double dist(Boat b, Point p) {
        return Math.hypot(b.getWorldX() - p.x, b.getWorldY() - p.y);
    }
    private static Point farthest(Point from, List<Point> pts) {
        Point best = from; double bd = -1;
        for (Point p : pts) { double d = from.distance(p); if (d > bd) { bd = d; best = p; } }
        return best;
    }
    private static boolean isWater(GameContext ctx, int x, int y) {
        Tile t = ctx.world().getTile(new Point(x, y));
        return t != null && resources.world.placement.TileRules.isWater(t.getName());
    }
    private static List<Point> findWater(GameContext ctx) {
        int ts = ctx.tileSize(); int r = 16 * ts;
        Point c = ctx.player().getPoint();
        List<Point> out = new ArrayList<>();
        for (int dy = -r; dy <= r; dy += ts)
            for (int dx = -r; dx <= r; dx += ts) {
                Point p = new Point(c.x + dx, c.y + dy);
                if (isWater(ctx, p.x, p.y)) out.add(p);
            }
        return out;
    }
}
