package resources.domain.combat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.object.Boat;
import resources.domain.tile.Tile;
import resources.presentation.image.BoatCombatSpriteSheet;
import resources.presentation.image.ImageContainer;

/**
 * Broadside cannonball fired from a boat. Flies in a straight line, damages
 * the first enemy boat hit, and expires after a fixed max range.
 */
public final class BoatProjectile extends Entity implements TransientWorldEntity {

    private static final int BODY_SIZE = 24;
    private static final int HITBOX_SIZE = 14;
    private static final int HITBOX_OFFSET = (BODY_SIZE - HITBOX_SIZE) / 2;
    private static final int FRAME_STEP_TICKS = 3;
    private static final int MAX_LIFETIME_TICKS = 220;

    /**
     * Row-0 cannonball sprites are authored with travel direction roughly to
     * the left, so we rotate from that baseline to the projectile heading.
     */
    private static final double SOURCE_FORWARD_ANGLE_DEG = 180.0;

    private static volatile ArrayList<BufferedImage> BASE_FRAMES;

    private final Boat shooter;
    private final double dirX;
    private final double dirY;
    private final double speedPxPerTick;
    private final double maxRangePx;
    private final int damage;

    private double traveledPx;
    private int frameIndex;
    private int ticks;
    private boolean expired;

    public BoatProjectile(GamePanel panel, Boat shooter,
                          double centerX, double centerY,
                          double dirX, double dirY,
                          double speedPxPerTick, double maxRangePx,
                          int damage) {
        super(panel, "boat_projectile",
            (int) Math.round(centerX - BODY_SIZE / 2.0),
            (int) Math.round(centerY - BODY_SIZE / 2.0),
            BODY_SIZE, BODY_SIZE,
            HITBOX_SIZE, HITBOX_SIZE,
            HITBOX_OFFSET, HITBOX_OFFSET);
        this.shooter = shooter;
        double len = Math.hypot(dirX, dirY);
        if (len <= 0.0001) {
            this.dirX = 1.0;
            this.dirY = 0.0;
        } else {
            this.dirX = dirX / len;
            this.dirY = dirY / len;
        }
        this.speedPxPerTick = Math.max(0.1, speedPxPerTick);
        this.maxRangePx = Math.max(this.speedPxPerTick, maxRangePx);
        this.damage = Math.max(1, damage);
        this.images = orientedFrames();
        this.solid = false;
    }

    @Override
    public void update() {
        if (expired) return;
        if (ticks >= MAX_LIFETIME_TICKS) {
            if (!isOffWater()) BoatCombatFx.spawnWaterImpact(panel, centerX(), centerY());
            expire();
            return;
        }

        step();
        if (expired) return;

        if (isOffWater()) {
            BoatCombatFx.spawnHitBurst(panel, centerX(), centerY());
            expire();
            return;
        }

        Boat target = hitBoat();
        if (target == null) return;

        target.takeBoatDamage(damage, centerX(), centerY());
        expire();
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> out = new ArrayList<>(1);
        out.add(images.get(frameIndex));
        return out;
    }

    private void step() {
        setWorldX(getWorldX() + dirX * speedPxPerTick);
        setWorldY(getWorldY() + dirY * speedPxPerTick);
        getHitBox().updateCoords();

        traveledPx += speedPxPerTick;
        ticks++;
        if (ticks % FRAME_STEP_TICKS == 0) {
            frameIndex = (frameIndex + 1) % images.size();
        }
        if (traveledPx >= maxRangePx) {
            if (!isOffWater()) BoatCombatFx.spawnWaterImpact(panel, centerX(), centerY());
            expire();
        }
    }

    private Boat hitBoat() {
        for (BaseEntity ent : panel.world.getEntities()) {
            if (!(ent instanceof Boat)) continue;
            Boat boat = (Boat) ent;
            if (boat == shooter) continue;
            if (boat.isDestroyed()) continue;
            if (getHitBox().intersects(boat.getHitBox())) return boat;
        }
        return null;
    }

    private boolean isOffWater() {
        Tile tile = panel.world.getTile(new Point((int) centerX(), (int) centerY()));
        if (tile == null) return true;
        String n = tile.getName();
        return !("ocean".equals(n) || "river".equals(n) || "shallowWater".equals(n));
    }

    private ArrayList<BufferedImage> orientedFrames() {
        ArrayList<BufferedImage> base = baseFrames();
        if (base.isEmpty()) return fallbackFrames();

        double heading = Math.toDegrees(Math.atan2(dirY, dirX));
        double rotateDeg = heading - SOURCE_FORWARD_ANGLE_DEG;

        ArrayList<BufferedImage> out = new ArrayList<>(base.size());
        for (BufferedImage frame : base) {
            BufferedImage rotated = ImageContainer.rotateImage(frame, rotateDeg);
            out.add(ImageContainer.scaleImage(rotated, BODY_SIZE, BODY_SIZE));
        }
        return out.isEmpty() ? fallbackFrames() : out;
    }

    private static ArrayList<BufferedImage> baseFrames() {
        ArrayList<BufferedImage> cached = BASE_FRAMES;
        if (cached != null) return cached;
        synchronized (BoatProjectile.class) {
            if (BASE_FRAMES != null) return BASE_FRAMES;
            ArrayList<BufferedImage> fromSheet = BoatCombatSpriteSheet.projectileFrames(BODY_SIZE);
            BASE_FRAMES = fromSheet.isEmpty() ? fallbackFrames() : fromSheet;
            return BASE_FRAMES;
        }
    }

    private static ArrayList<BufferedImage> fallbackFrames() {
        ArrayList<BufferedImage> out = new ArrayList<>(3);
        out.add(projectileFrame(new Color(255, 225, 170, 220), new Color(70, 70, 70, 220), 8));
        out.add(projectileFrame(new Color(255, 210, 140, 220), new Color(60, 60, 60, 220), 10));
        out.add(projectileFrame(new Color(255, 235, 190, 220), new Color(75, 75, 75, 220), 9));
        return out;
    }

    private static BufferedImage projectileFrame(Color glow, Color shell, int glowRadius) {
        BufferedImage img = new BufferedImage(BODY_SIZE, BODY_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = BODY_SIZE / 2;
        int cy = BODY_SIZE / 2;
        g.setColor(glow);
        g.fillOval(cx - glowRadius / 2, cy - glowRadius / 2, glowRadius, glowRadius);
        g.setColor(shell);
        g.fillOval(cx - 5, cy - 5, 10, 10);
        g.dispose();
        return img;
    }

    private double centerX() { return getWorldX() + getWidth() / 2.0; }
    private double centerY() { return getWorldY() + getHeight() / 2.0; }

    private void expire() {
        if (expired) return;
        expired = true;
        panel.world.addToRemovalQueue(this);
    }
}
