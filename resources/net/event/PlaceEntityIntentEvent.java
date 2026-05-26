package resources.net.event;

import java.awt.Point;

import resources.core.event.GameEvent;

/**
 * Proposed entity placement. Carries only validation-relevant data.
 */
public final class PlaceEntityIntentEvent implements GameEvent {

    private final String entityName;
    private final Point worldPoint;

    public PlaceEntityIntentEvent(String entityName, Point worldPoint) {
        this.entityName = entityName;
        this.worldPoint = (worldPoint == null) ? new Point() : new Point(worldPoint);
    }

    public String entityName() { return entityName; }
    public Point worldPoint()  { return new Point(worldPoint); }
}
