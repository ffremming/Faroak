package resources.domain.ship;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog of every ship kind. Mirrors the static-final-constants + ALL-list
 * pattern used by InteriorRegistry / BiomeRegistry. Add a kind by declaring a
 * constant and listing it in {@link #ALL}.
 */
public final class ShipKindRegistry {

    /** The migrated player boat — identical footprint/stats to the legacy Boat. */
    public static final ShipKind PLAYER_SLOOP = ShipKind.builder("player_sloop")
        .displayName("Sloop")
        .spriteDir("resources/images/objects/vehicles/watercraft/ships/starterShip/")
        .size(192, 192)
        .hitbox(144, 144)
        .speed(4.0)
        .maxHealth(30)
        .loadout(WeaponLoadout.BROADSIDE)
        .faction(Faction.NEUTRAL)
        .build();

    public static final java.util.List<ShipKind> ALL =
        java.util.Collections.unmodifiableList(java.util.Arrays.asList(PLAYER_SLOOP));

    private static final Map<String, ShipKind> BY_ID;
    static {
        Map<String, ShipKind> m = new LinkedHashMap<>();
        for (ShipKind k : ALL) m.put(k.id(), k);
        BY_ID = java.util.Collections.unmodifiableMap(m);
    }

    private ShipKindRegistry() {}

    public static ShipKind byId(String id) { return BY_ID.get(id); }

    /** Default water-traversal speed multipliers, shared by most kinds. Mirrors
     *  the table the legacy Boat built inline. */
    public static Map<String, Double> defaultWaterTerrain() {
        Map<String, Double> m = new HashMap<>();
        m.put("ocean",        1.0);
        m.put("shallowWater", 1.0);
        m.put("mediumWater",  1.0);
        m.put("river",        1.0);
        m.put("beach",        0.3);
        m.put("wetBeach",     0.3);
        m.put("tidalSand",    0.3);
        m.put("riverbank",    0.3);
        return m;
    }
}
