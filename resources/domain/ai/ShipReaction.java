package resources.domain.ai;

/** How a non-hostile ship answers an unprovoked attack. */
public enum ShipReaction {
    /** Sail on; ignore the hit. */
    IGNORE,
    /** Break off and run from the attacker. */
    FLEE,
    /** Turn and fight the attacker (only meaningful if armed). */
    RETALIATE
}
