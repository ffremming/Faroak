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

    // Shared art root for the new sliced ship sets (frames 0..7, 0=N clockwise).
    private static final String SHIP_ART = "resources/images/objects/ships/";

    /** Tiny civilian rowboat-with-sail: unarmed, pootles between spots, flees.
     *  Footprint follows the dinghy art aspect (186x219 ≈ 0.85). */
    public static final ShipKind DINGHY = ShipKind.builder("dinghy")
        .displayName("Dinghy")
        .spriteDir(SHIP_ART + "dinghy/")
        .size(96, 112).hitbox(60, 76)
        .speed(3.0).maxHealth(14)
        .loadout(WeaponLoadout.NONE)
        .faction(Faction.NEUTRAL)
        .reaction(ShipReaction.FLEE)
        .goal(ctx -> new FishingGoal(fishingSpotsAround(ctx), 180))
        .build();

    /** Small civilian rowboat: unarmed, wanders, flees. (299x319 ≈ 0.94) */
    public static final ShipKind ROWBOAT = ShipKind.builder("rowboat")
        .displayName("Rowboat")
        .spriteDir(SHIP_ART + "rowboat/")
        .size(120, 128).hitbox(84, 96)
        .speed(2.8).maxHealth(16)
        .loadout(WeaponLoadout.NONE)
        .faction(Faction.NEUTRAL)
        .reaction(ShipReaction.FLEE)
        .goal(ctx -> new FishingGoal(fishingSpotsAround(ctx), 200))
        .build();

    /** Neutral worker: unarmed, works fishing grounds, flees if struck.
     *  Sailboat art (237x254 ≈ 0.93). */
    public static final ShipKind FISHER = ShipKind.builder("fisher")
        .displayName("Fishing Boat")
        .spriteDir(SHIP_ART + "sailboat/")
        .size(136, 144).hitbox(100, 108)
        .speed(3.2).maxHealth(20)
        .loadout(WeaponLoadout.NONE)
        .faction(Faction.FISHER)
        .reaction(ShipReaction.FLEE)
        .goal(ctx -> new FishingGoal(fishingSpotsAround(ctx), 240))
        .build();

    /** Merchant trader: unarmed cargo hauler, crosses real distance on trade
     *  routes, retaliates if struck. Crewboat art (332x469 ≈ 0.71, tall). */
    public static final ShipKind CREWBOAT = ShipKind.builder("crewboat")
        .displayName("Merchant Crewboat")
        .spriteDir(SHIP_ART + "crewboat/")
        .size(160, 224).hitbox(112, 168)
        .speed(3.0).maxHealth(45)
        .loadout(WeaponLoadout.NONE)
        .faction(Faction.MERCHANT)
        .reaction(ShipReaction.RETALIATE)
        .goal(ctx -> new SailRouteGoal(galleonRouteAround(ctx), true))
        .build();

    /** Hostile hunter: armed, fast, mid HP, sweeps for prey. Galleon art
     *  (926x742 ≈ 1.25, wide). */
    public static final ShipKind PIRATE_BRIG = ShipKind.builder("pirate_brig")
        .displayName("Pirate Galleon")
        .spriteDir(SHIP_ART + "galleon/")
        .size(288, 232).hitbox(216, 176)
        .speed(4.4).maxHealth(40)
        .loadout(WeaponLoadout.BROADSIDE)
        .faction(Faction.PIRATE)
        .goal(ctx -> new PirateHuntGoal(System.nanoTime()))
        .build();

    /** Huge boardable flagship: heavy guns, patrols, has an interior deck.
     *  galleon_large art (1166x1089 ≈ 1.07). */
    public static final ShipKind GALLEON = ShipKind.builder("galleon")
        .displayName("Galleon Flagship")
        .spriteDir(SHIP_ART + "galleon_large/")
        .size(384, 360).hitbox(288, 272)
        .speed(2.4).maxHealth(140)
        .loadout(WeaponLoadout.HEAVY)
        .faction(Faction.NAVY)
        .interior(resources.generation.dimension.DimensionRegistry.SHIP_INTERIOR)
        .goal(ctx -> new SailRouteGoal(galleonRouteAround(ctx), true))
        .build();

    public static final java.util.List<ShipKind> ALL =
        java.util.Collections.unmodifiableList(java.util.Arrays.asList(
            PLAYER_SLOOP, DINGHY, ROWBOAT, FISHER, CREWBOAT, PIRATE_BRIG, GALLEON));

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
        m.put("midWater",     1.0);
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
