package resources.testing.probes;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.object.Boat;
import resources.domain.ship.ShipKindRegistry;
import resources.domain.tile.Tile;
import resources.generation.dimension.DimensionRegistry;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** Clicking a boardable galleon in range moves the player into the ship interior. */
public final class ShipBoardingProbe implements Probe {
    @Override public String name() { return "ship_boarding"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!ShipKindRegistry.GALLEON.boardable())
            return ProbeResult.fail(name(), "GALLEON must be boardable");

        List<Point> water = findWater(ctx);
        Point spot = water.isEmpty() ? ctx.player().getPoint() : water.get(0);

        // Park the galleon on the player so it's trivially in boarding range.
        ctx.player().setWorldX(spot.x);
        ctx.player().setWorldY(spot.y);
        ctx.player().getHitBox().updateCoords();
        Boat galleon = new Boat(ctx.player().panel, ShipKindRegistry.GALLEON, spot.x, spot.y, false);
        // Galleon hulls span many tiles; the harness spawn may lack that much
        // open water. Use the transient-free no-solid path via placeShipOnWater,
        // falling back to a skip if the hull doesn't fit on water.
        if (!ctx.world().placeShipOnWater(galleon))
            return ProbeResult.skip(name() + " galleon hull did not fit on water here");
        ctx.world().update(ctx.player().getPoint());

        boolean boarded = galleon.tryBoardInteriorFromClick(ctx.player());
        try { harness.tick(2); } catch (Throwable t) { return ProbeResult.fail(name(), "tick threw: " + t); }

        boolean inside = DimensionRegistry.SHIP_INTERIOR.equals(ctx.dimensions().currentDimension());
        if (!boarded) return ProbeResult.fail(name(), "tryBoardInteriorFromClick returned false");
        if (!inside)  return ProbeResult.fail(name(), "did not enter ship interior dimension");
        return ProbeResult.pass(name(), "entered ship interior");
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
