package resources.domain.ship;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import resources.domain.ai.FishingGoal;
import resources.domain.ai.PirateHuntGoal;
import resources.domain.ai.SailRouteGoal;
import resources.domain.ai.ShipReaction;

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

    /** Hostile hunter: armed, fast, mid HP, sweeps for prey. */
    public static final ShipKind PIRATE_BRIG = ShipKind.builder("pirate_brig")
        .displayName("Pirate Brig")
        .spriteDir("resources/images/objects/vehicles/watercraft/ships/pirateBrig/")
        .size(192, 192).hitbox(144, 144)
        .speed(4.4).maxHealth(40)
        .loadout(WeaponLoadout.BROADSIDE)
        .faction(Faction.PIRATE)
        .goal(ctx -> new PirateHuntGoal(System.nanoTime()))
        .build();

    /** Neutral worker: unarmed, works fishing grounds, flees if struck. */
    public static final ShipKind FISHER = ShipKind.builder("fisher")
        .displayName("Fishing Boat")
        .spriteDir("resources/images/objects/vehicles/watercraft/ships/fisher/")
        .size(128, 128).hitbox(96, 96)
        .speed(3.2).maxHealth(20)
        .loadout(WeaponLoadout.NONE)
        .faction(Faction.FISHER)
        .reaction(ShipReaction.FLEE)
        .goal(ctx -> new FishingGoal(fishingSpotsAround(ctx), 240))
        .build();

    /** Huge boardable warship: heavy guns, patrols, interior wired in Task 10. */
    public static final ShipKind GALLEON = ShipKind.builder("galleon")
        .displayName("Galleon")
        .spriteDir("resources/images/objects/vehicles/watercraft/ships/galleon/")
        .size(320, 320).hitbox(240, 240)
        .speed(2.4).maxHealth(140)
        .loadout(WeaponLoadout.HEAVY)
        .faction(Faction.NAVY)
        .goal(ctx -> new SailRouteGoal(galleonRouteAround(ctx), true))
        .build();

    public static final java.util.List<ShipKind> ALL =
        java.util.Collections.unmodifiableList(java.util.Arrays.asList(
            PLAYER_SLOOP, PIRATE_BRIG, FISHER, GALLEON));

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

    /** A small ring of fishing spots near the player's current location. The
     *  pilot navigates around land between them, so spots needn't be reachable
     *  in a straight line. */
    private static java.util.List<Point> fishingSpotsAround(resources.app.GameContext ctx) {
        int ox = ctx.player() != null ? (int) ctx.player().getWorldX() : 0;
        int oy = ctx.player() != null ? (int) ctx.player().getWorldY() : 0;
        return Arrays.asList(
            new Point(ox + 600,  oy),
            new Point(ox,        oy + 600),
            new Point(ox - 600,  oy),
            new Point(ox,        oy - 600));
    }

    /** A long rectangular patrol route for galleons — crosses real distance. */
    private static java.util.List<Point> galleonRouteAround(resources.app.GameContext ctx) {
        int ox = ctx.player() != null ? (int) ctx.player().getWorldX() : 0;
        int oy = ctx.player() != null ? (int) ctx.player().getWorldY() : 0;
        return Arrays.asList(
            new Point(ox + 1600, oy),
            new Point(ox + 1600, oy + 1600),
            new Point(ox,        oy + 1600),
            new Point(ox,        oy));
    }
}
