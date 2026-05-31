package resources.domain.ai;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;

/**
 * Sails a fixed list of far-apart waypoints. Loops back to the first when
 * {@code loop} is true, otherwise ping-pongs end to end — either way the ship
 * keeps crossing real distance rather than drifting. Used by merchants/traders.
 */
public final class SailRouteGoal implements ShipGoal {

    private final List<Point> route;
    private final boolean loop;
    private int index;
    private int dir = 1;

    public SailRouteGoal(List<Point> route, boolean loop) {
        this.route = new ArrayList<>(route);
        this.loop = loop;
    }

    @Override
    public Point currentWaypoint(BaseEntity host, GameContext ctx) {
        if (route.isEmpty()) return null;
        return route.get(index);
    }

    @Override
    public void onWaypointReached(BaseEntity host, GameContext ctx) {
        if (route.isEmpty()) return;
        if (loop) {
            index = (index + 1) % route.size();
            return;
        }
        if (index + dir < 0 || index + dir >= route.size()) dir = -dir;
        index += dir;
    }
}
