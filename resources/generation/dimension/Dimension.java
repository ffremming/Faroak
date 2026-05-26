package resources.generation.dimension;

import resources.core.id.Identifier;
import resources.generation.WorldGenerator;

/**
 * A self-contained world space with its own generator, ambient settings, and
 * (eventually) chunk store. The overworld, caves, and interior buildings are
 * each a Dimension.
 *
 * Players move between dimensions through portals; each dimension keeps its
 * own chunk system so chunks don't interfere across spaces.
 *
 * For now we only ship {@link #overworld()}; cave/interior dimensions plug in
 * later by constructing additional instances and registering them in
 * {@link DimensionRegistry}.
 */
public final class Dimension {

    private final Identifier id;
    private final WorldGenerator generator;
    private final boolean hasNaturalLight;
    private final double ambientLightFloor;

    public Dimension(Identifier id, WorldGenerator generator,
                     boolean hasNaturalLight, double ambientLightFloor) {
        this.id = id;
        this.generator = generator;
        this.hasNaturalLight = hasNaturalLight;
        this.ambientLightFloor = ambientLightFloor;
    }

    public Identifier      id()                 { return id; }
    public WorldGenerator  generator()          { return generator; }

    /** True for overworld; false for caves/interiors (always dark unless lit). */
    public boolean         hasNaturalLight()    { return hasNaturalLight; }

    /** Minimum light level even at "night"; 1.0 = fully lit. */
    public double          ambientLightFloor()  { return ambientLightFloor; }
}
