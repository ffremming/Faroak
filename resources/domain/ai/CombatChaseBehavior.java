package resources.domain.ai;

import resources.app.GameContext;
import resources.domain.combat.CombatService;
import resources.domain.combat.MeleeAttackSpec;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.HealthComponent;
import resources.domain.player.Moveable;
import resources.domain.player.Playable;
import resources.geometry.Vector;

/**
 * Chase + combat behavior with optional ranged pressure.
 */
public final class CombatChaseBehavior implements AIBehavior {

    private final BaseEntity target;
    private final double chaseStep;

    private final int meleeRange;
    private final int meleeDamage;
    private final int meleeCooldownTicks;
    private final String meleeSprite;

    private final int rangedRange;
    private final int rangedDamage;
    private final int rangedCooldownTicks;
    private final double projectileSpeed;
    private final int projectileLifeTicks;
    private final String projectileSprite;

    private final CombatService combat = new CombatService();

    private int meleeCooldown;
    private int rangedCooldown;

    public CombatChaseBehavior(
            BaseEntity target,
            double chaseStep,
            int meleeRange,
            int meleeDamage,
            int meleeCooldownTicks,
            String meleeSprite,
            int rangedRange,
            int rangedDamage,
            int rangedCooldownTicks,
            double projectileSpeed,
            int projectileLifeTicks,
            String projectileSprite) {
        this.target = target;
        this.chaseStep = chaseStep;
        this.meleeRange = meleeRange;
        this.meleeDamage = meleeDamage;
        this.meleeCooldownTicks = meleeCooldownTicks;
        this.meleeSprite = meleeSprite;
        this.rangedRange = rangedRange;
        this.rangedDamage = rangedDamage;
        this.rangedCooldownTicks = rangedCooldownTicks;
        this.projectileSpeed = projectileSpeed;
        this.projectileLifeTicks = projectileLifeTicks;
        this.projectileSprite = projectileSprite;
    }

    @Override
    public void tick(BaseEntity host, GameContext ctx) {
        if (host == null || ctx == null || target == null) return;
        if (!targetAlive()) return;

        if (meleeCooldown > 0) meleeCooldown--;
        if (rangedCooldown > 0) rangedCooldown--;

        double ox = host.getWorldX() + host.getWidth() / 2.0;
        double oy = host.getWorldY() + host.getHeight() / 2.0;
        double tx = target.getWorldX() + target.getWidth() / 2.0;
        double ty = target.getWorldY() + target.getHeight() / 2.0;

        double dx = tx - ox;
        double dy = ty - oy;
        double dist = Math.hypot(dx, dy);
        if (dist < 0.0001) return;

        Vector aim = new Vector(dx, dy).normalize(1.0);

        if (dist <= meleeRange && meleeCooldown == 0) {
            int hits = combat.meleeAttack(host, ctx, aim, new MeleeAttackSpec(
                meleeDamage,
                meleeRange + 16,
                100.0,
                1,
                meleeSprite,
                8,
                138.0,
                44.0));
            if (hits > 0) {
                meleeCooldown = Math.max(1, meleeCooldownTicks);
            }
        }

        if (rangedEnabled() && dist <= rangedRange && rangedCooldown == 0) {
            if (combat.fireProjectile(host, ctx, aim,
                    rangedDamage,
                    projectileSpeed,
                    projectileLifeTicks,
                    projectileSprite)) {
                rangedCooldown = Math.max(1, rangedCooldownTicks);
            }
        }

        moveTowards(host, dist, aim);
    }

    private void moveTowards(BaseEntity host, double dist, Vector aim) {
        // Keep a short stand-off distance so attacks can land.
        double preferred = rangedEnabled() ? Math.max(48.0, rangedRange * 0.6) : Math.max(24.0, meleeRange * 0.7);
        if (dist <= preferred) return;

        if (host instanceof Moveable) {
            Moveable moveable = (Moveable) host;
            moveable.addVelocity(new Vector(aim.x * chaseStep, aim.y * chaseStep));
            return;
        }

        host.setWorldX(host.getWorldX() + aim.x * chaseStep);
        host.setWorldY(host.getWorldY() + aim.y * chaseStep);
    }

    private boolean targetAlive() {
        if (target instanceof Playable) {
            Playable p = (Playable) target;
            return p.lifecycle() != null && !p.lifecycle().isDead();
        }
        HealthComponent hp = target.getComponent(HealthComponent.class);
        return hp == null || !hp.isDead();
    }

    private boolean rangedEnabled() {
        return rangedRange > 0 && rangedDamage > 0
            && projectileSpeed > 0.0 && projectileLifeTicks > 0;
    }
}
