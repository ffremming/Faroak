package resources.generation.interior;

import java.awt.Point;

import resources.app.GamePanel;
import resources.generation.dimension.Dimension;
import resources.generation.dimension.DimensionRegistry;

/**
 * Boot-time hook that registers the interior dimension. Builds an
 * {@link InteriorManager}, anchors the predefined interiors from
 * {@link InteriorRegistry} at known slots, and wraps the whole thing in a
 * {@link Dimension}.
 *
 * Each new interior layout should be added to {@link InteriorRegistry}; each
 * new placement should be added inside {@link #buildManager}.
 */
public final class InteriorBootstrap {

    private static InteriorManager lastBuilt;

    private InteriorBootstrap() {}

    /** The manager built by the most recent {@link #register} call, or null
     *  if the dimension hasn't been registered yet. Exposed so callers that
     *  set up portal arrival points after dimension registration (e.g.
     *  GenerationManager once the player exists) can update its state. */
    public static InteriorManager lastBuilt() { return lastBuilt; }

    /** Build the interior dimension. {@code overworldArrival} is where the
     *  player lands when leaving any interior through a door. */
    public static Dimension register(GamePanel panel, Point overworldArrival) {
        InteriorTileTypes.bootstrap();
        InteriorManager manager = buildManager(panel, overworldArrival);
        lastBuilt = manager;
        return new Dimension(
            DimensionRegistry.INTERIOR,
            new InteriorGenerator(manager),
            false,
            0.3);
    }

    /** Backwards-compatible overload — defaults overworld arrival to (0,0). */
    public static Dimension register(GamePanel panel) {
        return register(panel, new Point(0, 0));
    }

    private static InteriorManager buildManager(GamePanel panel, Point overworldArrival) {
        InteriorManager manager = new InteriorManager(panel, overworldArrival);
        // Three predefined interiors, each in its own slot. Slot 0 holds the
        // starter house so the default overworld door (which arrives at (0,0)
        // in the interior dimension) lands the player inside it.
        manager.place(0, 0, InteriorRegistry.STARTER_HOUSE);
        manager.place(1, 0, InteriorRegistry.STONE_HALL);
        manager.place(0, 1, InteriorRegistry.LONG_CABIN);
        return manager;
    }
}
