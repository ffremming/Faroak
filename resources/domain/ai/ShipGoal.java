package resources.domain.ai;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;

/**
 * A ship's standing purpose: emit the next destination the ship should sail
 * toward. The pilot behaviour drives the ship there via WaterNavigator. A goal
 * is stateful (remembers its current leg) and resumes cleanly after a combat
 * interruption. Returns null when the ship should simply hold position.
 */
public interface ShipGoal {
    /** World-pixel point the ship currently wants to reach, or null to idle. */
    Point currentWaypoint(BaseEntity host, GameContext ctx);

    /** Called when the host reaches (is close to) the current waypoint so the
     *  goal can advance to its next leg. */
    void onWaypointReached(BaseEntity host, GameContext ctx);
}
