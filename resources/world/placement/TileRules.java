package resources.world.placement;

/**
 * Tile-name classification helpers shared by placement rules and farming.
 *
 * Centralised so that hoeing logic ({@link resources.domain.farming.FarmingService})
 * and surface-rule placement gates use the same "what counts as soil" semantics.
 */
public final class TileRules {

    /**
     * Grass/soil tiles that a hoe can turn into farmland. Includes the primary
     * biome tile names (plains/forest/savanna) AND their grass variants, since
     * generation paints most plains/forest/savanna ground with variant names
     * like "meadowGrass" or "lushGrass" rather than the biome's base name.
     * Without these, ~2/3 of visibly-grassy tiles silently refuse to be hoed.
     */
    public static boolean isTillable(String tileName) {
        if (tileName == null) return false;
        return tileName.startsWith("plains")
            || tileName.startsWith("forest")
            || tileName.startsWith("savanna")
            || tileName.equals("seasonal forest")
            || tileName.equals("meadowGrass")
            || tileName.equals("lushGrass")
            || tileName.equals("mossyGrass")
            || tileName.equals("dryGrassTile")
            || tileName.equals("burnedGrass");
    }

    public static boolean isWater(String tileName) {
        if (tileName == null) return false;
        return "ocean".equals(tileName)
            || "river".equals(tileName)
            || "mediumWater".equals(tileName)
            || "midWater".equals(tileName)
            || "shallowWater".equals(tileName);
    }

    private TileRules() {}
}
