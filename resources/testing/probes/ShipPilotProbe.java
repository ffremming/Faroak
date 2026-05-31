package resources.testing.probes;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.ai.PirateHuntGoal;
import resources.domain.ai.ShipPilotBehavior;
import resources.domain.combat.BoatProjectile;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.AIComponent;
import resources.domain.object.Boat;
import resources.domain.ship.Faction;
import resources.domain.ship.ShipKind;
import resources.domain.ship.WeaponLoadout;
import resources.domain.tile.Tile;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** A hostile armed ship should close on the player's boat and eventually fire. */
public final class ShipPilotProbe implements Probe {
    @Override public String name() { return "ship_pilot"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        List<Point> water = findWater(ctx);
        if (water.size() < 3) return ProbeResult.skip(name() + " not enough water");

        // Put the player's boat on one water tile and ride it (so it's a target).
        Point pSpot = water.get(0);
        Boat playerBoat = new Boat(ctx.player().panel, pSpot.x, pSpot.y, false);
        if (!ctx.world().placeShipOnWater(playerBoat))
            return ProbeResult.skip(name() + " place player boat failed");
        // Move the player onto the boat so boarding succeeds and the boat counts
        // as the player's (ridden) target for hostile ships.
        ctx.player().setWorldX(playerBoat.getWorldX() + playerBoat.getWidth() / 2.0);
        ctx.player().setWorldY(playerBoat.getWorldY() + playerBoat.getHeight() / 2.0);
        ctx.player().getHitBox().updateCoords();
        playerBoat.tryBoardFromClick(ctx.player());
        if (!playerBoat.isRidden())
            return ProbeResult.skip(name() + " player failed to board target boat");

        // Pirate a few tiles away, armed, hostile.
        Point ePot = farFrom(pSpot, water, ctx.tileSize() * 4);
        if (ePot == null) return ProbeResult.skip(name() + " no nearby water for pirate");
        ShipKind pirate = ShipKind.builder("test_pirate")
            .size(192, 192).hitbox(144, 144).speed(4.0).maxHealth(40)
            .loadout(WeaponLoadout.BROADSIDE).faction(Faction.PIRATE).build();
        Boat enemy = new Boat(ctx.player().panel, pirate, ePot.x, ePot.y, false);
        enemy.addComponent(new AIComponent(ctx,
            new ShipPilotBehavior(new PirateHuntGoal(1L))));
        if (!ctx.world().placeShipOnWater(enemy))
            return ProbeResult.skip(name() + " place pirate failed");
        ctx.world().update(ctx.player().getPoint());

        double startDist = Math.hypot(enemy.getWorldX() - playerBoat.getWorldX(),
                                      enemy.getWorldY() - playerBoat.getWorldY());
        boolean fired = false;
        try {
            for (int i = 0; i < 240; i++) {
                harness.tick(1);
                if (countProjectiles(ctx) > 0) { fired = true; break; }
            }
        } catch (Throwable t) {
            return ProbeResult.fail(name(), "pilot tick threw: " + t);
        }
        double endDist = Math.hypot(enemy.getWorldX() - playerBoat.getWorldX(),
                                    enemy.getWorldY() - playerBoat.getWorldY());

        ctx.world().removeEntity(enemy);
        ctx.world().removeEntity(playerBoat);

        if (!fired && endDist >= startDist)
            return ProbeResult.fail(name(), "pirate neither approached nor fired: "
                + (int) startDist + " -> " + (int) endDist);
        return ProbeResult.pass(name(), "engaged: dist " + (int) startDist + "->" + (int) endDist
            + ", fired=" + fired);
    }

    private static int countProjectiles(GameContext ctx) {
        int n = 0;
        for (BaseEntity e : ctx.world().getEntities()) if (e instanceof BoatProjectile) n++;
        return n;
    }
    private static Point farFrom(Point from, List<Point> pts, int minPx) {
        for (Point p : pts) if (from.distance(p) >= minPx) return p;
        return null;
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
