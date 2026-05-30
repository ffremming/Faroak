package resources.domain.combat;

/**
 * Immutable configuration for a single melee strike. Groups the tunable damage
 * and swing-VFX numbers that previously travelled as a long parameter list on
 * {@link CombatService#meleeAttack}. The attacker, game context, and aim vector
 * are NOT part of this spec — they are runtime inputs, not weapon config.
 *
 * @param damage           hit-point damage applied per target
 * @param rangePx          reach of the arc in pixels
 * @param arcDegrees       full sweep angle of the damage arc in degrees
 * @param maxTargets       cap on targets hit (<= 0 means unlimited)
 * @param swingSpriteName  sprite for the visual swing arc (null skips it)
 * @param swingTicks       duration of the swing VFX in ticks
 * @param swingArcDegrees  visual swing arc angle in degrees
 * @param swingRadiusPx    visual swing radius in pixels
 */
public record MeleeAttackSpec(
        int damage,
        int rangePx,
        double arcDegrees,
        int maxTargets,
        String swingSpriteName,
        int swingTicks,
        double swingArcDegrees,
        double swingRadiusPx) {
}
