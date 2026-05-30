package resources.domain.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.component.HealthComponent;
import resources.domain.entity.component.LootComponent;
import resources.domain.inventory.DropTable;
import resources.domain.object.GroundItem;
import resources.domain.player.Moveable;
import resources.domain.player.Playable;
import resources.geometry.Vector;

/**
 * Shared combat helpers: melee arcs, projectile spawning, and health damage.
 */
public final class CombatService {

    private static final int DEFAULT_PROJECTILE_SIZE = 28;

    private final Random rng;

    public CombatService() {
        this(new Random());
    }

    public CombatService(Random rng) {
        this.rng = rng;
    }

    /**
     * Execute one sweeping melee strike.
     *
     * @return number of health targets hit.
     */
    public int meleeAttack(
            BaseEntity attacker,
            GameContext ctx,
            Vector aim,
            MeleeAttackSpec spec) {
        if (attacker == null || ctx == null || spec == null || spec.damage() <= 0) return 0;
        if (!(ctx instanceof GamePanel)) return 0;

        Vector direction = normalizedAim(aim, attacker);
        spawnSwingEffect(attacker, ctx, direction, spec.swingSpriteName(),
            spec.swingTicks(), spec.swingArcDegrees(), spec.swingRadiusPx());

        List<TargetCandidate> targets =
            targetsInArc(attacker, ctx, direction, spec.rangePx(), spec.arcDegrees());
        int hits = 0;
        for (TargetCandidate candidate : targets) {
            if (spec.maxTargets() > 0 && hits >= spec.maxTargets()) break;
            if (applyDamage(ctx, attacker, candidate.target, spec.damage())) {
                hits++;
            }
        }
        return hits;
    }

    public boolean fireProjectile(
            BaseEntity attacker,
            GameContext ctx,
            Vector aim,
            int damage,
            double speedPxPerTick,
            int lifeTicks,
            String spriteName) {
        if (attacker == null || ctx == null || damage <= 0) return false;
        if (!(ctx instanceof GamePanel)) return false;
        Vector direction = normalizedAim(aim, attacker);
        if (direction.hasNoVelocity()) return false;

        CombatProjectile projectile = new CombatProjectile(
            (GamePanel) ctx,
            this,
            attacker,
            direction,
            damage,
            speedPxPerTick,
            Math.max(1, lifeTicks),
            spriteName,
            DEFAULT_PROJECTILE_SIZE);
        return placeTransient(ctx, projectile);
    }

    /** Apply damage to one target if target carries health. */
    public boolean applyDamage(GameContext ctx, BaseEntity attacker, BaseEntity target, int damage) {
        if (ctx == null || attacker == null || target == null || damage <= 0) return false;
        if (!isDamageableTarget(attacker, target)) return false;

        // Players and HealthComponent-bearing entities take damage through
        // different paths (player lifecycle vs. component + loot-on-death), so
        // dispatch stays an instanceof check with each branch in its own helper.
        if (target instanceof Playable) {
            return damagePlayable(ctx, (Playable) target, damage);
        }
        return damageHealthComponent(ctx, attacker, target, damage);
    }

    /** Damage a player via its lifecycle; players drop no loot here. */
    private boolean damagePlayable(GameContext ctx, Playable playable, int damage) {
        if (playable.lifecycle() == null || playable.lifecycle().isDead()) return false;
        playable.lifecycle().damage(damage);
        spawnHitEffect(ctx, playable);
        return true;
    }

    /** Damage a HealthComponent-bearing entity; awards loot if it dies. */
    private boolean damageHealthComponent(
            GameContext ctx, BaseEntity attacker, BaseEntity target, int damage) {
        HealthComponent hp = target.getComponent(HealthComponent.class);
        if (hp == null || hp.isDead()) return false;

        hp.takeDamage(damage);
        spawnHitEffect(ctx, target);
        if (hp.isDead()) {
            onDeath(ctx, attacker, target);
        }
        return true;
    }

    /** Predicate used by melee/projectile hit-tests. */
    public boolean isDamageableTarget(BaseEntity attacker, BaseEntity target) {
        if (target == null || attacker == null) return false;
        if (target == attacker) return false;
        if (target instanceof WeaponSwingEffect) return false;
        if (target instanceof CombatProjectile) return false;

        if (target instanceof Playable) {
            Playable p = (Playable) target;
            return p.lifecycle() != null && !p.lifecycle().isDead();
        }

        HealthComponent hp = target.getComponent(HealthComponent.class);
        return hp != null && !hp.isDead();
    }

    private void onDeath(GameContext ctx, BaseEntity attacker, BaseEntity target) {
        awardLoot(ctx, attacker, target);
        ctx.world().addToRemovalQueue(target);
    }

    private void awardLoot(GameContext ctx, BaseEntity attacker, BaseEntity target) {
        if (!(attacker instanceof Playable)) return;
        LootComponent loot = target.getComponent(LootComponent.class);
        if (loot == null || loot.dropTable() == null) return;

        Playable player = (Playable) attacker;
        DropTable table = loot.dropTable();
        // Spill loot onto the ground at the slain entity's centre; the player
        // walks over it to collect (mirrors the harvest/farming drop flow).
        int cx = (int) (target.getWorldX() + target.getWidth()  / 2.0);
        int cy = (int) (target.getWorldY() + target.getHeight() / 2.0);
        for (DropTable.Drop drop : table.roll(rng)) {
            int dx = rng.nextInt(LOOT_SCATTER_PX * 2 + 1) - LOOT_SCATTER_PX;
            int dy = rng.nextInt(LOOT_SCATTER_PX * 2 + 1) - LOOT_SCATTER_PX;
            GroundItem ground = new GroundItem(
                player.panel, drop.itemName, drop.quantity, cx + dx, cy + dy);
            ctx.world().placeEntity(ground);
        }
    }

    /** Pixel scatter applied to each loot stack around a slain entity. */
    private static final int LOOT_SCATTER_PX = 16;

    private List<TargetCandidate> targetsInArc(
            BaseEntity attacker,
            GameContext ctx,
            Vector direction,
            int rangePx,
            double arcDegrees) {
        ArrayList<TargetCandidate> candidates = new ArrayList<>();
        double ox = attacker.getWorldX() + attacker.getWidth() / 2.0;
        double oy = attacker.getWorldY() + attacker.getHeight() / 2.0;
        double halfArc = Math.max(1.0, arcDegrees) / 2.0;

        ArrayList<BaseEntity> snapshot = new ArrayList<>(ctx.world().getEntities());
        for (BaseEntity target : snapshot) {
            if (!isDamageableTarget(attacker, target)) continue;

            double tx = target.getWorldX() + target.getWidth() / 2.0;
            double ty = target.getWorldY() + target.getHeight() / 2.0;
            double dx = tx - ox;
            double dy = ty - oy;
            double distSq = dx * dx + dy * dy;
            double targetRadius = Math.max(target.getWidth(), target.getHeight()) * 0.5;
            double maxRange = rangePx + targetRadius;
            if (distSq > maxRange * maxRange) continue;

            double dist = Math.sqrt(Math.max(0.0001, distSq));
            double dot = ((dx * direction.x) + (dy * direction.y)) / dist;
            dot = Math.max(-1.0, Math.min(1.0, dot));
            double angle = Math.toDegrees(Math.acos(dot));
            if (angle > halfArc) continue;

            candidates.add(new TargetCandidate(target, distSq));
        }

        candidates.sort((a, b) -> Double.compare(a.distSq, b.distSq));
        return candidates;
    }

    /**
     * Spawn the visual swing + impact effects for a non-combat action (e.g. a
     * harvest swing with an axe) without resolving any damage. Reuses the same
     * {@link WeaponSwingEffect} / {@link CombatHitEffect} art as melee combat so
     * chopping a tree looks consistent with striking an enemy.
     *
     * @param swingSpriteName equipped tool/weapon sprite (e.g. "axe"); null skips the swing arc.
     * @param target          entity being struck; null skips the impact burst.
     */
    public void spawnActionEffects(
            BaseEntity attacker,
            GameContext ctx,
            Vector aim,
            String swingSpriteName,
            int swingTicks,
            double swingArcDegrees,
            double swingRadiusPx,
            BaseEntity target) {
        if (attacker == null || !(ctx instanceof GamePanel)) return;
        Vector direction = normalizedAim(aim, attacker);
        spawnSwingEffect(attacker, ctx, direction, swingSpriteName,
            Math.max(1, swingTicks), swingArcDegrees, swingRadiusPx);
        if (target != null) spawnHitEffect(ctx, target);
    }

    private void spawnSwingEffect(
            BaseEntity attacker,
            GameContext ctx,
            Vector direction,
            String spriteName,
            int swingTicks,
            double swingArcDegrees,
            double swingRadiusPx) {
        if (spriteName == null || spriteName.isBlank()) return;
        WeaponSwingEffect effect = new WeaponSwingEffect(
            (GamePanel) ctx,
            attacker,
            direction,
            spriteName,
            Math.max(1, swingTicks),
            swingArcDegrees,
            swingRadiusPx);
        placeTransient(ctx, effect);
    }

    private void spawnHitEffect(GameContext ctx, BaseEntity target) {
        if (!(ctx instanceof GamePanel) || target == null) return;
        double cx = target.getWorldX() + target.getWidth() / 2.0;
        double cy = target.getWorldY() + target.getHeight() / 2.0;
        CombatHitEffect fx = new CombatHitEffect((GamePanel) ctx, cx, cy, 44);
        placeTransient(ctx, fx);
    }

    private static boolean placeTransient(GameContext ctx, BaseEntity entity) {
        if (ctx == null || entity == null) return false;
        if (ctx.world().placeEntityIgnoringTerrainCollision(entity)) return true;
        return ctx.world().placeEntity(entity);
    }

    private static Vector normalizedAim(Vector raw, BaseEntity attacker) {
        if (raw != null && !raw.hasNoVelocity()) {
            return raw.normalize(1.0);
        }
        if (attacker instanceof Moveable) {
            Vector facing = ((Moveable) attacker).getFacingVector();
            if (facing != null && !facing.hasNoVelocity()) {
                return facing.normalize(1.0);
            }
        }
        return new Vector(1, 0);
    }

    private static final class TargetCandidate {
        final BaseEntity target;
        final double distSq;

        TargetCandidate(BaseEntity target, double distSq) {
            this.target = target;
            this.distSq = distSq;
        }
    }
}
