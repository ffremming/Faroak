package resources.domain.object;

import java.awt.Point;

import resources.app.GamePanel;
import resources.core.event.DimensionChangeEvent;
import resources.core.id.Identifier;
import resources.domain.player.Playable;

/**
 * Interactive object that, on use, fires a {@link DimensionChangeEvent} for
 * the configured destination dimension. The dimension service does the actual
 * world swap + player reposition.
 *
 * Constructed with the from / to identifiers + the arrival point in the
 * destination dimension, so two portals can pair up by configuration without
 * runtime coupling.
 */
public final class Portal extends GameObject {

    private final Identifier from;
    private final Identifier to;
    private final Point      arrival;

    public Portal(GamePanel panel, String name, int worldX, int worldY,
                  Identifier from, Identifier to, Point arrival) {
        super(panel, name, worldX, worldY, 64, 64, 64, 64, 0, 0, false);
        this.from    = from;
        this.to      = to;
        this.arrival = new Point(arrival);
    }

    @Override
    public void interact(Playable playable) {
        playable.panel.events().publish(new DimensionChangeEvent(from, to, arrival));
    }
}
