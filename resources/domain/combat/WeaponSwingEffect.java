package resources.domain.combat;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.presentation.image.CombatSpriteSheet;
import resources.presentation.image.ImageContainer;

/**
 * Short-lived visual slash that orbits its owner for one swing arc.
 */
public final class WeaponSwingEffect extends Entity implements TransientWorldEntity {

    private final BaseEntity owner;
    private final BufferedImage source;
    private final ArrayList<BufferedImage> slashFrames;
    private final int lifeTicks;
    private final double startAngleRad;
    private final double endAngleRad;
    private final double radiusPx;

    private int ticks;

    public WeaponSwingEffect(
            GamePanel panel,
            BaseEntity owner,
            resources.geometry.Vector direction,
            String spriteName,
            int lifeTicks,
            double arcDegrees,
            double radiusPx) {
        super(panel, "weapon_swing", (int) owner.getWorldX(), (int) owner.getWorldY(),
            40, 40, 40, 40, 0, 0);
        this.owner = owner;
        this.lifeTicks = Math.max(1, lifeTicks);
        this.radiusPx = Math.max(10.0, radiusPx);

        BufferedImage raw = panel.images().getItemImage(spriteName);
        if (raw == null) {
            ArrayList<BufferedImage> fallback = panel.images().getObjectImages(spriteName);
            raw = fallback.isEmpty() ? null : fallback.get(0);
        }
        if (raw == null) {
            raw = panel.images().getItemImage("axe");
        }
        this.source = ImageContainer.scaleImage(raw, 40, 40);
        this.slashFrames = CombatSpriteSheet.slashFrames(56);

        double baseAngle = Math.atan2(direction.y, direction.x);
        double halfArc = Math.toRadians(Math.max(10.0, arcDegrees) / 2.0);
        this.startAngleRad = baseAngle - halfArc;
        this.endAngleRad = baseAngle + halfArc;

        this.solid = false;
        refreshPose(0.0);
    }

    @Override
    public void update() {
        ticks++;
        if (ticks > lifeTicks) {
            panel.world().addToRemovalQueue(this);
            return;
        }
        double progress = (double) ticks / (double) lifeTicks;
        refreshPose(progress);
    }

    private void refreshPose(double progress) {
        double eased = smooth(progress);
        double angle = startAngleRad + (endAngleRad - startAngleRad) * eased;

        BufferedImage rotated = ImageContainer.rotateImage(source, Math.toDegrees(angle) + 90.0);
        BufferedImage slash = slashFrame(progress);
        if (slash != null) slash = ImageContainer.rotateImage(slash, Math.toDegrees(angle) + 90.0);
        BufferedImage visual = composeVisual(rotated, slash);
        images.clear();
        images.add(visual);

        width = visual.getWidth();
        height = visual.getHeight();

        double cx = owner.getWorldX() + owner.getWidth() / 2.0;
        double cy = owner.getWorldY() + owner.getHeight() / 2.0;
        worldX = cx + Math.cos(angle) * radiusPx - width / 2.0;
        worldY = cy + Math.sin(angle) * radiusPx - height / 2.0;
        getHitBox().updateCoords();
    }

    private BufferedImage slashFrame(double progress) {
        if (slashFrames == null || slashFrames.isEmpty()) return null;
        int idx = (int) Math.floor(Math.max(0.0, Math.min(0.999, progress)) * slashFrames.size());
        idx = Math.max(0, Math.min(slashFrames.size() - 1, idx));
        return slashFrames.get(idx);
    }

    private static BufferedImage composeVisual(BufferedImage weapon, BufferedImage slash) {
        if (slash == null) return weapon;
        int w = Math.max(weapon.getWidth(), slash.getWidth());
        int h = Math.max(weapon.getHeight(), slash.getHeight());
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(slash, (w - slash.getWidth()) / 2, (h - slash.getHeight()) / 2, null);
        g.drawImage(weapon, (w - weapon.getWidth()) / 2, (h - weapon.getHeight()) / 2, null);
        g.dispose();
        return out;
    }

    private static double smooth(double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }
}
