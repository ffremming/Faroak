package resources.domain.ship;

/**
 * Allegiance of a vessel. Drives who a ship treats as prey and how it reacts
 * to being attacked. The player is not a faction value — relations to the
 * player are expressed via {@link FactionRelations#isHostileToPlayer}.
 */
public enum Faction {
    /** Attacks the player and rival merchants/navy on sight. */
    PIRATE,
    /** Hauls cargo on trade routes; neutral, retaliates if struck. */
    MERCHANT,
    /** Works fishing grounds; neutral, flees if struck. */
    FISHER,
    /** Law enforcement; hostile to pirates, neutral to the player for now. */
    NAVY,
    /** Inert default; never initiates combat. */
    NEUTRAL
}
