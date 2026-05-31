package resources.domain.ai;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;

/**
 * Visits a list of fishing spots: sail to a spot, "fish" (hold position) for a
 * fixed number of ticks, then advance to the next spot, looping. The idle is
 * counted down on each {@link #currentWaypoint} call while the goal is in the
 * fishing state, so the pilot's per-tick polling drives it without extra wiring.
 */
public final class FishingGoal implements ShipGoal {

    private final List<Point> spots;
    private final int fishTicks;
    private int index;
    private boolean fishing;
    private int remaining;

    public FishingGoal(List<Point> spots, int fishTicks) {
        this.spots = new ArrayList<>(spots);
        this.fishTicks = Math.max(1, fishTicks);
    }

    @Override
    public Point currentWaypoint(BaseEntity host, GameContext ctx) {
        if (spots.isEmpty()) return null;
        if (fishing) {
            if (--remaining <= 0) {
                fishing = false;
                index = (index + 1) % spots.size();
            }
            // While fishing, keep aiming at the current spot so the ship holds.
        }
        return spots.get(index);
    }

    @Override
    public void onWaypointReached(BaseEntity host, GameContext ctx) {
        if (spots.isEmpty() || fishing) return;
        fishing = true;
        remaining = fishTicks;
    }
}
