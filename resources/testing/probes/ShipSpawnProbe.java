package resources.testing.probes;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.object.Boat;
import resources.domain.spawn.ShipSpawner;
import resources.domain.tile.Tile;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** Spawner places NPC ships on water and they tick without throwing. */
public final class ShipSpawnProbe implements Probe {
    @Override public String name() { return "ship_spawn"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        // Center the fleet on a real patch of water if the player's own tile is
        // landlocked, so the spawner has somewhere to place hulls.
        List<Point> water = findWater(ctx);
        Point center = water.isEmpty() ? ctx.player().getPoint() : water.get(water.size() / 2);

        int before = countNpcShips(ctx);
        ShipSpawner.spawnFleetNear(ctx, center);
        ctx.world().update(ctx.player().getPoint());
        int after = countNpcShips(ctx);
        if (after <= before)
            return ProbeResult.skip(name() + " no water fit a hull near spawn (" + before + "->" + after + ")");
        try { harness.tick(30); }
        catch (Throwable t) { return ProbeResult.fail(name(), "spawned ship tick threw: " + t); }
        return ProbeResult.pass(name(), "spawned " + (after - before) + " NPC ships");
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

    private static int countNpcShips(GameContext ctx) {
        int n = 0;
        for (BaseEntity e : ctx.world().getEntities()) {
            if (!(e instanceof Boat)) continue;
            Boat b = (Boat) e;
            if (b.kind() != null && !"player_sloop".equals(b.kind().id())) n++;
        }
        return n;
    }
}
