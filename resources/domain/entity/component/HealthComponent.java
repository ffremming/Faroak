package resources.domain.entity.component;

import resources.domain.entity.BaseEntity;

/**
 * Tracks an entity's hit-point pool. Pure data carrier — when health drains
 * to zero the {@code dead} flag flips, but the component itself never removes
 * its host. The owning system (combat, mob despawn, harvest) decides what
 * "death" means for that entity.
 *
 * Kept deliberately small so it can be attached to anything that bleeds:
 * mobs, the player, breakable props.
 */
public final class HealthComponent implements EntityComponent {

    private final int maxHealth;
    private int current;
    private boolean dead;

    public HealthComponent(int maxHealth) {
        if (maxHealth <= 0) throw new IllegalArgumentException("maxHealth must be > 0");
        this.maxHealth = maxHealth;
        this.current   = maxHealth;
    }

    @Override public void onAttach(BaseEntity owner) { /* no registration needed */ }
    @Override public void onDetach(BaseEntity owner) { /* no registration needed */ }

    /** Subtract {@code amount} hit points (clamped at 0). Flips {@code dead} on depletion. */
    public void takeDamage(int amount) {
        if (amount <= 0 || dead) return;
        current -= amount;
        if (current <= 0) {
            current = 0;
            dead = true;
        }
    }

    /** Restore up to {@link #max()} hit points. No-op if already dead. */
    public void heal(int amount) {
        if (amount <= 0 || dead) return;
        current = Math.min(maxHealth, current + amount);
    }

    public boolean isDead() { return dead; }
    public int     current(){ return current; }
    public int     max()    { return maxHealth; }
}
