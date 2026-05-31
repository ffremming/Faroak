package resources.generation.interior;

import java.awt.Point;

import resources.app.GamePanel;
import resources.generation.dimension.Dimension;
import resources.generation.dimension.DimensionRegistry;

/**
 * Registers the ship-interior dimension (decks the player walks when boarding a
 * boardable ship). Currently a single galleon deck at slot 0; add more layouts
 * here as more boardable kinds arrive. Mirrors {@link InteriorBootstrap}.
 */
public final class ShipInteriorBootstrap {

    private static InteriorManager lastBuilt;

    private ShipInteriorBootstrap() {}

    /** Manager built by the most recent {@link #register} call, or null. */
    public static InteriorManager lastBuilt() { return lastBuilt; }

    public static Dimension register(GamePanel panel, Point overworldArrival) {
        InteriorTileTypes.bootstrap();
        InteriorManager manager = new InteriorManager(panel, overworldArrival);
        manager.place(0, 0, InteriorRegistry.GALLEON_DECK);
        lastBuilt = manager;
        return new Dimension(
            DimensionRegistry.SHIP_INTERIOR,
            new InteriorGenerator(manager),
            false,
            0.25);
    }

    /** Backwards-compatible overload — defaults overworld arrival to (0,0). */
    public static Dimension register(GamePanel panel) {
        return register(panel, new Point(0, 0));
    }
}
