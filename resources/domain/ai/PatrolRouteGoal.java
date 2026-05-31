package resources.domain.ai;

import java.awt.Point;
import java.util.List;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;

/**
 * A closed patrol circuit — semantically a looping {@link SailRouteGoal}, named
 * separately so navy/galleon intent reads clearly and so future patrol-specific
 * tuning (alertness, speed changes) has a home. Delegates to a looping sail
 * route rather than subclassing it.
 */
public final class PatrolRouteGoal implements ShipGoal {
    private final SailRouteGoal inner;
    public PatrolRouteGoal(List<Point> circuit) { this.inner = new SailRouteGoal(circuit, true); }
    @Override public Point currentWaypoint(BaseEntity h, GameContext c) { return inner.currentWaypoint(h, c); }
    @Override public void onWaypointReached(BaseEntity h, GameContext c) { inner.onWaypointReached(h, c); }
}
