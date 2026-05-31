package resources.domain.ship;

/**
 * Central, data-only hostility table. Kept as static logic (not per-ship
 * state) so every ship answers "is that thing my enemy?" identically. Extend
 * here when new factions or alliances are added.
 */
public final class FactionRelations {

    private FactionRelations() {}

    /** Does a ship of {@code self} attack the player on sight? */
    public static boolean isHostileToPlayer(Faction self) {
        return self == Faction.PIRATE;
    }

    /** Does a ship of {@code self} attack a ship of {@code other} on sight? */
    public static boolean isHostile(Faction self, Faction other) {
        if (self == other) return false;
        switch (self) {
            case PIRATE: return other == Faction.MERCHANT
                              || other == Faction.FISHER
                              || other == Faction.NAVY;
            case NAVY:   return other == Faction.PIRATE;
            default:     return false; // MERCHANT, FISHER, NEUTRAL don't initiate
        }
    }
}
