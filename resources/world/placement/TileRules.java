package resources.world.placement;

/**
 * Tile-name classification helpers shared by placement rules and farming.
 *
 * Centralised so that hoeing logic ({@link resources.domain.farming.FarmingService})
 * and surface-rule placement gates use the same "what counts as soil" semantics.
 */
public final class TileRules {

    public static boolean isTillable(String tileName) {
        if (tileName == null) return false;
        return tileName.startsWith("plains")
            || tileName.startsWith("forest")
            || tileName.startsWith("savanna");
    }

    public static boolean isWater(String tileName) {
        if (tileName == null) return false;
        return "ocean".equals(tileName)
            || "river".equals(tileName)
            || "shallowWater".equals(tileName);
    }

    private TileRules() {}
}
