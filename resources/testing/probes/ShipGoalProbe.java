package resources.testing.probes;

import java.awt.Point;
import java.util.Arrays;

import resources.app.GameContext;
import resources.domain.ai.FishingGoal;
import resources.domain.ai.SailRouteGoal;
import resources.domain.ai.ShipGoal;
import resources.domain.object.Boat;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** Goal contract: SailRoute cycles its waypoints; Fishing yields a spot then
 *  advances after its idle. Pure logic — no world placement required. */
public final class ShipGoalProbe implements Probe {
    @Override public String name() { return "ship_goal"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        Point spawn = ctx.player().getPoint();
        Boat host = new Boat(ctx.player().panel, spawn.x, spawn.y, false);

        Point a = new Point(spawn.x, spawn.y);
        Point b = new Point(spawn.x + 1000, spawn.y);
        ShipGoal sail = new SailRouteGoal(Arrays.asList(a, b), true);
        Point wp1 = sail.currentWaypoint(host, ctx);
        if (wp1 == null) return ProbeResult.fail(name(), "sail goal gave null first waypoint");
        sail.onWaypointReached(host, ctx);
        Point wp2 = sail.currentWaypoint(host, ctx);
        if (wp2 == null || wp2.equals(wp1))
            return ProbeResult.fail(name(), "sail goal did not advance waypoint");

        FishingGoal fish = new FishingGoal(Arrays.asList(a, b), 5);
        Point fwp = fish.currentWaypoint(host, ctx);
        if (fwp == null) return ProbeResult.fail(name(), "fishing goal gave null spot");
        fish.onWaypointReached(host, ctx);      // arrive — begin idling
        Point during = fish.currentWaypoint(host, ctx);
        if (during == null || !during.equals(fwp))
            return ProbeResult.fail(name(), "fishing goal should hold spot while fishing");
        for (int i = 0; i < 6; i++) fish.currentWaypoint(host, ctx); // tick out the idle
        Point next = fish.currentWaypoint(host, ctx);
        if (next == null || next.equals(fwp))
            return ProbeResult.fail(name(), "fishing goal did not move to next spot after idle");

        return ProbeResult.pass(name(), "sail + fishing goals advance correctly");
    }
}
