package resources.domain.ship;

/**
 * Immutable weapon profile for a ship kind. Tunes the broadside the ship fires
 * through {@code BoatCombatComponent}. The existing player boat values
 * (8 damage, 24-tick cooldown, 15-tile range, 13 px/tick) live here as the
 * BROADSIDE preset so the player ship is unchanged after migration.
 */
public final class WeaponLoadout {

    /** Unarmed — a fisher or transport. fireBroadside() is never invoked. */
    public static final WeaponLoadout NONE =
        new WeaponLoadout(false, 0, 0, 0, 0.0);

    /** The classic player broadside. */
    public static final WeaponLoadout BROADSIDE =
        new WeaponLoadout(true, 8, 24, 15, 13.0);

    /** Heavier galleon/warship guns: more damage, slower, longer reach. */
    public static final WeaponLoadout HEAVY =
        new WeaponLoadout(true, 14, 40, 18, 12.0);

    private final boolean armed;
    private final int damage;
    private final int cooldownTicks;
    private final int rangeTiles;
    private final double projectileSpeed;

    private WeaponLoadout(boolean armed, int damage, int cooldownTicks,
                          int rangeTiles, double projectileSpeed) {
        this.armed = armed;
        this.damage = damage;
        this.cooldownTicks = cooldownTicks;
        this.rangeTiles = rangeTiles;
        this.projectileSpeed = projectileSpeed;
    }

    public boolean armed()          { return armed; }
    public int     damage()         { return damage; }
    public int     cooldownTicks()  { return cooldownTicks; }
    public int     rangeTiles()     { return rangeTiles; }
    public double  projectileSpeed(){ return projectileSpeed; }
}
