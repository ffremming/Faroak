package resources.domain.ai;

import java.awt.Point;
import java.util.Random;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;

/**
 * A pirate's search pattern: sweep long legs across open water hunting for
 * prey. Actual chase/fire is the pilot's hostile override; this goal only
 * keeps an idle pirate roaming wide so it covers ground and stumbles onto
 * targets, rather than circling one spot.
 */
public final class PirateHuntGoal implements ShipGoal {

    private static final int LEG_PIXELS = 2000;

    private final Random rng;
    private Point waypoint;

    public PirateHuntGoal(long seed) { this.rng = new Random(seed); }

    @Override
    public Point currentWaypoint(BaseEntity host, GameContext ctx) {
        if (waypoint == null) waypoint = roll(host);
        return waypoint;
    }

    @Override
    public void onWaypointReached(BaseEntity host, GameContext ctx) {
        waypoint = roll(host);
    }

    private Point roll(BaseEntity host) {
        double a = rng.nextDouble() * Math.PI * 2;
        int x = (int) (host.getWorldX() + Math.cos(a) * LEG_PIXELS);
        int y = (int) (host.getWorldY() + Math.sin(a) * LEG_PIXELS);
        return new Point(x, y);
    }
}
