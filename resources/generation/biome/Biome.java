package resources.generation.biome;

import java.util.Collections;
import java.util.List;

/**
 * Immutable biome definition. Pairs a logical name with the tile sprite(s) used for the
 * ground and the list of vegetation rules that can populate it.
 *
 * {@code tileName} is the biome's primary tile — used for altitude bucketing and as a
 * sensible default. {@code tileVariants} is an optional list of additional sprite names
 * that the picker rotates through deterministically per (x, y). When non-empty, the
 * picker chooses uniformly from {@code tileVariants}; otherwise it uses {@code tileName}.
 * Always include {@code tileName} in {@code tileVariants} if you want it sampled too.
 */
public class Biome {

    public final String id;
    public final String tileName;
    public final List<String> tileVariants;
    public final boolean water;
    public final List<VegetationRule> vegetation;

    public Biome(String id, String tileName, boolean water, List<VegetationRule> vegetation) {
        this(id, tileName, Collections.emptyList(), water, vegetation);
    }

    public Biome(String id, String tileName, List<String> tileVariants, boolean water,
                 List<VegetationRule> vegetation) {
        this.id           = id;
        this.tileName     = tileName;
        this.tileVariants = tileVariants == null ? Collections.emptyList() : tileVariants;
        this.water        = water;
        this.vegetation   = vegetation == null ? Collections.emptyList() : vegetation;
    }
}
