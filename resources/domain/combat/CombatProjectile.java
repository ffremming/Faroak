package resources.domain.combat;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.presentation.image.ImageContainer;

/**
 * Simple projectile entity: flies in a straight line, damages first health
 * target hit, and expires on impact or timeout.
 */
public final class CombatProjectile extends Entity implements TransientWorldEntity {

    private final CombatService service;
    private final BaseEntity attacker;
    private final double dx;
    private final double dy;
    private final int damage;
    private final double speedPxPerTick;
    private final int lifeTicks;

    private int ticks;

    public CombatProjectile(
            GamePanel panel,
            CombatService service,
            BaseEntity attacker,
            resources.geometry.Vector direction,
            int damage,
            double speedPxPerTick,
            int lifeTicks,
            String spriteName,
            int sizePx) {
        super(panel, "projectile", (int) attacker.getWorldX(), (int) attacker.getWorldY(),
            sizePx, sizePx, sizePx, sizePx, 0, 0);
        this.service = service;
        this.attacker = attacker;
        this.dx = direction.x;
        this.dy = direction.y;
        this.damage = damage;
        this.speedPxPerTick = speedPxPerTick;
        this.lifeTicks = lifeTicks;
        this.solid = false;

        BufferedImage raw = panel.images().getItemImage(spriteName);
        if (raw == null) {
            ArrayList<BufferedImage> fallback = panel.images().getObjectImages(spriteName);
            raw = fallback.isEmpty() ? null : fallback.get(0);
        }
        if (raw == null) raw = panel.images().getItemImage("block");

        BufferedImage scaled = ImageContainer.scaleImage(raw, sizePx, sizePx);
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        BufferedImage rotated = ImageContainer.rotateImage(scaled, angle);
        images.clear();
        images.add(rotated);
        width = rotated.getWidth();
        height = rotated.getHeight();

        centerOnAttacker();
    }

    @Override
    public void update() {
        ticks++;
        if (ticks > lifeTicks) {
            panel.world().addToRemovalQueue(this);
            return;
        }

        worldX += dx * speedPxPerTick;
        worldY += dy * speedPxPerTick;
        getHitBox().updateCoords();

        if (resolveImpact()) {
            panel.world().addToRemovalQueue(this);
        }
    }

    private boolean resolveImpact() {
        ArrayList<BaseEntity> collided = panel.world().getEntitiesCollidedWith(getHitBox());
        for (BaseEntity target : collided) {
            if (target == null || target == this || target == attacker) continue;
            if (target instanceof WeaponSwingEffect) continue;
            if (target instanceof CombatProjectile) continue;

            if (service.isDamageableTarget(attacker, target)) {
                if (service.applyDamage(panel, attacker, target, damage)) return true;
                continue;
            }

            if (target.isSolid()) return true;
        }
        return false;
    }

    private void centerOnAttacker() {
        double cx = attacker.getWorldX() + attacker.getWidth() / 2.0;
        double cy = attacker.getWorldY() + attacker.getHeight() / 2.0;
        worldX = cx - width / 2.0;
        worldY = cy - height / 2.0;
        getHitBox().updateCoords();
    }
}
