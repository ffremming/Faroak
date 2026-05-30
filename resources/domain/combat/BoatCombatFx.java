package resources.domain.combat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.presentation.image.BoatCombatSpriteSheet;
import resources.presentation.image.CombatSpriteSheet;
import resources.presentation.image.LazyImageCache;

/**
 * Short-lived boat combat VFX entities (muzzle, hit, splash, sink).
 */
public final class BoatCombatFx extends Entity implements TransientWorldEntity {

    private static final int FAST_FRAME_STEP_TICKS = 2;
    private static final int SLOW_FRAME_STEP_TICKS = 3;

    private static final LazyImageCache<ArrayList<BufferedImage>> MUZZLE_FRAMES =
        new LazyImageCache<>(() -> {
            ArrayList<BufferedImage> fromSheet = BoatCombatSpriteSheet.muzzleFrames(56);
            if (fromSheet.isEmpty()) fromSheet = CombatSpriteSheet.hitFrames(56);
            return fromSheet.isEmpty() ? buildMuzzleFrames() : fromSheet;
        });
    private static final LazyImageCache<ArrayList<BufferedImage>> HIT_FRAMES =
        new LazyImageCache<>(() -> {
            ArrayList<BufferedImage> combined = new ArrayList<>();
            combined.addAll(BoatCombatSpriteSheet.hitSparkFrames(70));
            combined.addAll(BoatCombatSpriteSheet.explosionFrames(70));
            if (combined.isEmpty()) combined.addAll(CombatSpriteSheet.hitFrames(70));
            return combined.isEmpty() ? buildHitFrames() : combined;
        });
    private static final LazyImageCache<ArrayList<BufferedImage>> SMOKE_FRAMES =
        new LazyImageCache<>(() -> {
            ArrayList<BufferedImage> fromSheet = BoatCombatSpriteSheet.smokeFrames(76);
            return fromSheet.isEmpty() ? buildSmokeFrames() : fromSheet;
        });
    private static final LazyImageCache<ArrayList<BufferedImage>> SPLASH_FRAMES =
        new LazyImageCache<>(() -> {
            ArrayList<BufferedImage> fromSheet = BoatCombatSpriteSheet.splashFrames(90);
            return fromSheet.isEmpty() ? buildSplashFrames() : fromSheet;
        });
    private static final LazyImageCache<ArrayList<BufferedImage>> RIPPLE_FRAMES =
        new LazyImageCache<>(() -> {
            ArrayList<BufferedImage> fromSheet = BoatCombatSpriteSheet.rippleFrames(96);
            return fromSheet.isEmpty() ? buildRippleFrames() : fromSheet;
        });
    private static final LazyImageCache<ArrayList<BufferedImage>> SINK_FRAMES =
        new LazyImageCache<>(() -> {
            ArrayList<BufferedImage> combined = new ArrayList<>();
            combined.addAll(BoatCombatSpriteSheet.sinkDebrisFrames(120));
            combined.addAll(BoatCombatSpriteSheet.splashFrames(120));
            return combined.isEmpty() ? buildSinkFrames() : combined;
        });

    private final ArrayList<BufferedImage> frames;
    private final int frameStepTicks;
    private int frameIndex;
    private int ticks;
    private boolean expired;

    private BoatCombatFx(GamePanel panel, String name, double centerX, double centerY,
                         ArrayList<BufferedImage> frames, int frameStepTicks) {
        super(panel, name,
            (int) Math.round(centerX - frames.get(0).getWidth() / 2.0),
            (int) Math.round(centerY - frames.get(0).getHeight() / 2.0),
            frames.get(0).getWidth(), frames.get(0).getHeight(),
            frames.get(0).getWidth(), frames.get(0).getHeight(),
            0, 0);
        this.frames = frames;
        this.frameStepTicks = Math.max(1, frameStepTicks);
        this.images = frames;
        this.solid = false;
    }

    public static void spawnMuzzleFlash(GamePanel panel, double centerX, double centerY) {
        spawn(panel, new BoatCombatFx(
            panel, "boat_fx_muzzle", centerX, centerY, MUZZLE_FRAMES.get(), FAST_FRAME_STEP_TICKS));
    }

    public static void spawnHitBurst(GamePanel panel, double centerX, double centerY) {
        spawn(panel, new BoatCombatFx(
            panel, "boat_fx_hit", centerX, centerY, HIT_FRAMES.get(), FAST_FRAME_STEP_TICKS));
        spawn(panel, new BoatCombatFx(
            panel, "boat_fx_smoke", centerX, centerY, SMOKE_FRAMES.get(), SLOW_FRAME_STEP_TICKS));
    }

    public static void spawnWaterImpact(GamePanel panel, double centerX, double centerY) {
        spawn(panel, new BoatCombatFx(
            panel, "boat_fx_splash", centerX, centerY, SPLASH_FRAMES.get(), FAST_FRAME_STEP_TICKS));
        spawn(panel, new BoatCombatFx(
            panel, "boat_fx_ripple", centerX, centerY, RIPPLE_FRAMES.get(), SLOW_FRAME_STEP_TICKS));
    }

    public static void spawnSinkBurst(GamePanel panel, double centerX, double centerY) {
        spawn(panel, new BoatCombatFx(
            panel, "boat_fx_sink", centerX, centerY, SINK_FRAMES.get(), FAST_FRAME_STEP_TICKS));
        spawn(panel, new BoatCombatFx(
            panel, "boat_fx_sink_ripple", centerX, centerY, RIPPLE_FRAMES.get(), SLOW_FRAME_STEP_TICKS));
    }

    private static void spawn(GamePanel panel, BoatCombatFx fx) {
        if (panel == null || panel.world == null || fx == null) return;
        panel.world.placeEntityIgnoringTerrainCollision(fx);
    }

    @Override
    public void update() {
        if (expired) return;
        ticks++;
        if (ticks % frameStepTicks != 0) return;
        frameIndex++;
        if (frameIndex >= frames.size()) {
            expire();
        }
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> out = new ArrayList<>(1);
        int idx = Math.min(frameIndex, frames.size() - 1);
        out.add(frames.get(idx));
        return out;
    }

    private void expire() {
        if (expired) return;
        expired = true;
        panel.world.addToRemovalQueue(this);
    }

    private static ArrayList<BufferedImage> buildMuzzleFrames() {
        final int size = 56;
        ArrayList<BufferedImage> out = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            float t = i / 4.0f;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int core = 10 + (int) (t * 18);
            int ring = core + 10;
            g.setColor(new Color(255, 240, 180, 220 - i * 30));
            g.fillOval((size - ring) / 2, (size - ring) / 2, ring, ring);
            g.setColor(new Color(255, 180, 40, 220 - i * 35));
            g.fillOval((size - core) / 2, (size - core) / 2, core, core);
            g.setColor(new Color(255, 255, 255, 220 - i * 40));
            int spark = Math.max(2, 6 - i);
            g.fillOval(size / 2 - spark / 2, size / 2 - spark / 2, spark, spark);
            g.dispose();
            out.add(img);
        }
        return out;
    }

    private static ArrayList<BufferedImage> buildHitFrames() {
        final int size = 72;
        ArrayList<BufferedImage> out = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            float t = i / 5.0f;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int burst = 14 + (int) (t * 26);
            g.setColor(new Color(255, 170, 40, 220 - i * 25));
            g.fillOval((size - burst) / 2, (size - burst) / 2, burst, burst);
            int smoke = burst + 16;
            g.setColor(new Color(90, 90, 90, 140 - i * 18));
            g.fillOval((size - smoke) / 2, (size - smoke) / 2, smoke, smoke);
            g.setColor(new Color(255, 245, 215, 220 - i * 35));
            int core = Math.max(2, 10 - i);
            g.fillOval(size / 2 - core / 2, size / 2 - core / 2, core, core);
            g.dispose();
            out.add(img);
        }
        return out;
    }

    private static ArrayList<BufferedImage> buildSmokeFrames() {
        final int size = 76;
        ArrayList<BufferedImage> out = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r = 10 + i * 6;
            g.setColor(new Color(70, 70, 70, 180 - i * 20));
            g.fillOval(size / 2 - r / 2, size / 2 - r / 2, r, r);
            g.dispose();
            out.add(img);
        }
        return out;
    }

    private static ArrayList<BufferedImage> buildSplashFrames() {
        final int size = 96;
        ArrayList<BufferedImage> out = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            float t = i / 6.0f;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = 18 + (int) (t * 58);
            int h = 12 + (int) (t * 26);
            g.setColor(new Color(140, 210, 255, 200 - i * 18));
            g.fillOval((size - w) / 2, size / 2 - h / 2, w, h);
            int crest = Math.max(4, 20 - i * 2);
            g.setColor(new Color(230, 245, 255, 210 - i * 24));
            g.fillOval(size / 2 - crest / 2, size / 2 - 6 - crest, crest, crest);
            g.dispose();
            out.add(img);
        }
        return out;
    }

    private static ArrayList<BufferedImage> buildRippleFrames() {
        final int size = 96;
        ArrayList<BufferedImage> out = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r = 12 + i * 8;
            g.setColor(new Color(120, 200, 255, 180 - i * 20));
            g.drawOval(size / 2 - r / 2, size / 2 - r / 5, r, Math.max(6, r / 3));
            g.dispose();
            out.add(img);
        }
        return out;
    }

    private static ArrayList<BufferedImage> buildSinkFrames() {
        final int size = 140;
        ArrayList<BufferedImage> out = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            float t = i / 7.0f;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int splashW = 24 + (int) (t * 76);
            int splashH = 14 + (int) (t * 34);
            g.setColor(new Color(180, 220, 255, 170 - i * 18));
            g.fillOval((size - splashW) / 2, (size - splashH) / 2 + 8, splashW, splashH);
            int foam = 18 + (int) (t * 48);
            g.setColor(new Color(240, 250, 255, 180 - i * 20));
            g.fillOval((size - foam) / 2, (size - foam) / 2, foam, Math.max(8, foam / 3));
            g.dispose();
            out.add(img);
        }
        return out;
    }
}
