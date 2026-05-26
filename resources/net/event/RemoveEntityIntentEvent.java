package resources.net.event;

import java.awt.Point;

import resources.core.event.GameEvent;

/**
 * Proposed entity removal. Used by authority checks before world mutation.
 */
public final class RemoveEntityIntentEvent implements GameEvent {

    private final String entityName;
    private final Point worldPoint;

    public RemoveEntityIntentEvent(String entityName, Point worldPoint) {
        this.entityName = entityName;
        this.worldPoint = (worldPoint == null) ? new Point() : new Point(worldPoint);
    }

    public String entityName() { return entityName; }
    public Point worldPoint()  { return new Point(worldPoint); }
}
