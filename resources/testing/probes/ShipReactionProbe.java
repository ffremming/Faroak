package resources.testing.probes;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import resources.app.GameContext;
import resources.domain.ai.FishingGoal;
import resources.domain.ai.ShipPilotBehavior;
import resources.domain.ai.ShipReaction;
import resources.domain.entity.component.AIComponent;
import resources.domain.object.Boat;
import resources.domain.ship.Faction;
import resources.domain.ship.ShipKind;
import resources.domain.ship.WeaponLoadout;
import resources.domain.tile.Tile;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** A FLEE fisher moves away from its attacker after being struck. */
public final class ShipReactionProbe implements Probe {
    @Override public String name() { return "ship_reaction"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        List<Point> water = findWater(ctx);
        if (water.size() < 2) return ProbeResult.skip(name() + " not enough water");

        Point spot = water.get(0);
        ShipKind fisher = ShipKind.builder("test_fisher")
            .size(128, 128).hitbox(96, 96).speed(4.0).maxHealth(20)
            .loadout(WeaponLoadout.NONE).faction(Faction.FISHER)
            .reaction(ShipReaction.FLEE).build();
        Boat boat = new Boat(ctx.player().panel, fisher, spot.x, spot.y, false);
        boat.addComponent(new AIComponent(ctx,
            new ShipPilotBehavior(new FishingGoal(Arrays.asList(spot), 9999))));
        if (!ctx.world().placeShipOnWater(boat))
            return ProbeResult.skip(name() + " place fisher failed");
        ctx.world().update(ctx.player().getPoint());

        // Attack from the player position; the fisher should sail away from it.
        Point src = ctx.player().getPoint();
        double before = boat.getPoint().distance(src);
        boat.takeBoatDamage(1, src.x, src.y);
        try { harness.tick(60); }
        catch (Throwable t) { return ProbeResult.fail(name(), "tick threw: " + t); }
        double after = boat.getPoint().distance(src);
        ctx.world().removeEntity(boat);

        if (after <= before)
            return ProbeResult.fail(name(), "fisher did not flee: " + (int) before + " -> " + (int) after);
        return ProbeResult.pass(name(), "fisher fled: " + (int) before + " -> " + (int) after);
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
