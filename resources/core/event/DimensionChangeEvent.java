package resources.core.event;

import java.awt.Point;

import resources.core.id.Identifier;

/**
 * Fired when the player crosses a portal. Subscribers (DimensionService,
 * lighting, music, save system) react synchronously on the main loop.
 *
 * Immutable; carry only what the subscriber needs.
 */
public final class DimensionChangeEvent implements GameEvent {

    private final Identifier from;
    private final Identifier to;
    private final Point      arrival;

    public DimensionChangeEvent(Identifier from, Identifier to, Point arrival) {
        this.from    = from;
        this.to      = to;
        this.arrival = new Point(arrival);
    }

    public Identifier from()    { return from; }
    public Identifier to()      { return to; }
    public Point      arrival() { return new Point(arrival); }
}
