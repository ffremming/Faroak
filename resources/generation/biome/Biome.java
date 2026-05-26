package resources.generation.biome;

import java.util.Collections;
import java.util.List;

/**
 * Immutable biome definition. Pairs a logical name with the tile sprite used for the
 * ground and the list of vegetation rules that can populate it.
 */
public class Biome {

    public final String id;
    public final String tileName;
    public final boolean water;
    public final List<VegetationRule> vegetation;

    public Biome(String id, String tileName, boolean water, List<VegetationRule> vegetation) {
        this.id         = id;
        this.tileName   = tileName;
        this.water      = water;
        this.vegetation = vegetation == null ? Collections.emptyList() : vegetation;
    }
}
