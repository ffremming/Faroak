package resources.generation.interior;

import resources.app.GamePanel;
import resources.generation.dimension.Dimension;
import resources.generation.dimension.DimensionRegistry;

/**
 * Boot-time hook that registers the interior dimension. Call once from
 * {@code GenerationManager.generateMap()} (or similar bootstrap site) so the
 * dimension is available before any portal asks the registry to resolve
 * {@link DimensionRegistry#INTERIOR}.
 *
 * Kept as a standalone helper rather than inlining into GenerationManager so
 * future dimensions can follow the same pattern (one bootstrap class per
 * dimension).
 */
public final class InteriorBootstrap {

    private static final long DEFAULT_SEED = 7777L;

    private InteriorBootstrap() {}

    /** Build and return the interior {@link Dimension}; caller registers it. */
    public static Dimension register(GamePanel panel) {
        InteriorTileTypes.bootstrap();
        return new Dimension(
            DimensionRegistry.INTERIOR,
            new InteriorGenerator(panel, DEFAULT_SEED),
            false,
            0.3);
    }
}
