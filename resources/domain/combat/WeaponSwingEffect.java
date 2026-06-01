package resources.domain.combat;

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

        // spriteName (the equipped weapon) is intentionally ignored: the swing
        // shows only the slash arc, not the weapon icon orbiting the player.
        this.slashFrames = CombatSpriteSheet.slashFrames(56);

        double baseAngle = Math.atan2(direction.y, direction.x);
        double halfArc = Math.toRadians(Math.max(10.0, arcDegrees) / 2.0);
        this.startAngleRad = baseAngle - halfArc;
        this.endAngleRad = baseAngle + halfArc;

        this.solid = false;
        refreshPose(0.0);
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        // The scene renderer draws getImages(); the inherited BaseEntity version
        // resolves our name ("weapon_swing") through the tile loader and returns
        // a flat green placeholder. Expose the rotated slash arc that
        // refreshPose() builds into the image list instead.
        return images;
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

        BufferedImage slash = slashFrame(progress);
        BufferedImage visual = slash != null
            ? ImageContainer.rotateImage(slash, Math.toDegrees(angle) + 90.0)
            : null;
        images.clear();
        if (visual != null) images.add(visual);

        width = visual != null ? visual.getWidth() : 40;
        height = visual != null ? visual.getHeight() : 40;

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

    private static double smooth(double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }
}
