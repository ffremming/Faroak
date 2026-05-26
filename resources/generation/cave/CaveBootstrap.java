package resources.generation.cave;

import resources.app.GamePanel;
import resources.generation.dimension.Dimension;
import resources.generation.dimension.DimensionRegistry;

/**
 * One-call entry point for wiring the cave dimension into the rest of the
 * generation system.
 *
 * Keeping the bootstrap separate from {@link CaveGenerator} lets the main
 * {@code GenerationManager} stay agnostic about which tile types or generator
 * configuration a dimension needs — it just receives a ready-to-register
 * {@link Dimension}.
 */
public final class CaveBootstrap {

    private static final long CAVE_SEED = 12345L;
    private static final double CAVE_AMBIENT = 0.05;

    private CaveBootstrap() {}

    /** Builds the cave {@link Dimension} (and ensures cave tile types are registered). */
    public static Dimension register(GamePanel panel) {
        CaveTileTypes.init();
        return new Dimension(
            DimensionRegistry.CAVE,
            new CaveGenerator(panel, CAVE_SEED),
            false,
            CAVE_AMBIENT);
    }
}
