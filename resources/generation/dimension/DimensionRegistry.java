package resources.generation.dimension;

import resources.core.id.Identifier;
import resources.core.registry.Registry;

/**
 * Where dimensions live once their generators are built. The bootstrap layer
 * (GenerationManager) populates this; gameplay code resolves dimensions by
 * Identifier when teleporting or initialising chunk systems.
 */
public final class DimensionRegistry {

    public static final Identifier OVERWORLD = Identifier.of("overworld");
    public static final Identifier CAVE      = Identifier.of("cave");
    public static final Identifier INTERIOR  = Identifier.of("interior");

    private static final Registry<Dimension> REG = new Registry<>("dimension");

    private DimensionRegistry() {}

    public static Registry<Dimension> instance() { return REG; }
}
