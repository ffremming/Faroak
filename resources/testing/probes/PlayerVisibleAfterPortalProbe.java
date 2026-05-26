package resources.testing.probes;

import java.awt.Point;
import java.awt.image.BufferedImage;

import resources.app.GameContext;
import resources.core.event.DimensionChangeEvent;
import resources.generation.dimension.DimensionRegistry;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Renders the scene before and after a portal-driven dimension change and
 * asserts that the player sprite is actually visible on screen (pixels at
 * the screen-centre region are non-empty) on the FIRST frame after the swap.
 *
 * This is a regression for the "player disappears when entering a cave" bug
 * that the entity-index probes kept missing — the index can contain the
 * player while the visibility filter culls them, leaving the rendered frame
 * blank. We catch that here by sampling the actual rendered output.
 */
public final class PlayerVisibleAfterPortalProbe implements Probe {

    private static final Logger LOG = Logger.forClass(PlayerVisibleAfterPortalProbe.class);

    /** Half-side of the sample box around the player's screen-centre, in pixels. */
    private static final int SAMPLE_HALF = 48;

    @Override public String name() { return "player-visible-after-portal"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.player() == null) return ProbeResult.skip(name() + " no player");
        if (ctx.dimensions() == null) return ProbeResult.skip(name() + " no DimensionService");

        // Baseline: player must already be visible in the overworld so the
        // probe doesn't false-positive when nothing renders at all.
        BufferedImage before = RenderSampler.render(ctx);
        boolean visibleBefore = sampleContainsPlayer(before, ctx);
        if (!visibleBefore) {
            return ProbeResult.fail(name() + " baseline: player not visible in overworld",
                pixelDetail(before, ctx));
        }

        try {
            // Trigger the portal exactly as the game's interact() flow would.
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.OVERWORLD, DimensionRegistry.CAVE, new Point(0, 0)));

            // No harness.tick() here on purpose: the bug we're catching is
            // that the FIRST frame after the swap should already show the
            // player. If we let the world tick we'd give EnvironmentManager
            // a chance to repair the index and the test would hide the bug.
            BufferedImage after = RenderSampler.render(ctx);
            boolean visibleAfter = sampleContainsPlayer(after, ctx);

            String detail = String.format(
                "before={visible=%s}, after={visible=%s, %s}",
                visibleBefore, visibleAfter, pixelDetail(after, ctx));
            LOG.info(detail);
            if (!visibleAfter) {
                return ProbeResult.fail(name() + " player sprite missing on first cave frame",
                    detail);
            }
            return ProbeResult.pass(name(), detail);
        } finally {
            ctx.events().publish(new DimensionChangeEvent(
                DimensionRegistry.CAVE, DimensionRegistry.OVERWORLD, new Point(0, 0)));
        }
    }

    /**
     * The camera centres the followed entity. So in screen-space the player
     * sprite straddles screenWidth/2, screenHeight/2. We sample a small box
     * around that point and accept ANY non-background pixel as evidence the
     * sprite drew.
     */
    private static boolean sampleContainsPlayer(BufferedImage img, GameContext ctx) {
        int cx = ctx.screenWidth()  / 2;
        int cy = ctx.screenHeight() / 2;
        return RenderSampler.hasNonBackgroundPixel(
            img, cx - SAMPLE_HALF, cy - SAMPLE_HALF, SAMPLE_HALF * 2, SAMPLE_HALF * 2);
    }

    private static String pixelDetail(BufferedImage img, GameContext ctx) {
        int cx = ctx.screenWidth()  / 2;
        int cy = ctx.screenHeight() / 2;
        int distinct = RenderSampler.distinctColorsIn(
            img, cx - SAMPLE_HALF, cy - SAMPLE_HALF, SAMPLE_HALF * 2, SAMPLE_HALF * 2);
        int centreArgb = img.getRGB(cx, cy);
        java.awt.Color cc = new java.awt.Color(centreArgb, true);
        // Count non-transparent pixels across the WHOLE frame, not just the
        // centre box. Tells us if anything painted at all.
        int nonTransparentPixels = 0;
        int totalPixels = img.getWidth() * img.getHeight();
        for (int y = 0; y < img.getHeight(); y += 4) {
            for (int x = 0; x < img.getWidth(); x += 4) {
                if (((img.getRGB(x, y) >>> 24) & 0xFF) != 0) nonTransparentPixels++;
            }
        }
        return String.format(
            "centre=(%d,%d) argb=#%08X (a=%d r=%d g=%d b=%d), distinct-in-box=%d, "
          + "img=%dx%d, non-transparent-samples=%d/%d, player-pos=(%d,%d), "
          + "camera-pos=(%d,%d), entities-visible=%d, sorted-visible=%d, tiles=%d",
            cx, cy, centreArgb, cc.getAlpha(), cc.getRed(), cc.getGreen(), cc.getBlue(),
            distinct, img.getWidth(), img.getHeight(),
            nonTransparentPixels, totalPixels / 16,
            (int) ctx.player().getWorldX(), (int) ctx.player().getWorldY(),
            (int) ctx.camera().getWorldX(), (int) ctx.camera().getWorldY(),
            ctx.world().getVisibleEntities(ctx.camera()).size(),
            ctx.world().getEntities().size(),
            ctx.world().getTiles().size());
    }
}
