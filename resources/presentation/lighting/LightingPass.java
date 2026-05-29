package resources.presentation.lighting;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;

import resources.core.time.GameClock;
import resources.presentation.camera.Camera;

/**
 * Composites darkness + light onto the rendered scene. Renders a translucent
 * black overlay sized to the camera viewport, then "punches" out a soft radial
 * hole per {@link LightSource} using {@link AlphaComposite#DstOut}.
 *
 * Ambient opacity comes from {@link GameClock#phase()}; per-dimension floor /
 * ceiling can layer on later. Kept independent of the scene renderer so the
 * lighting strategy can change (cell-based diffusion, GPU shader) without
 * touching tile/entity drawing.
 */
public final class LightingPass {

    private static final float[] FALLOFF_STOPS  = {0.0f, 0.7f, 1.0f};
    private static final Color   HOLE_OPAQUE    = new Color(0, 0, 0, 255);
    private static final Color   HOLE_HALF      = new Color(0, 0, 0, 128);
    private static final Color   HOLE_CLEAR     = new Color(0, 0, 0, 0);

    private final LightField  field;
    private final GameClock   clock;
    private BufferedImage overlay;
    private int overlayW = -1;
    private int overlayH = -1;

    public LightingPass(LightField field, GameClock clock) {
        this.field = field;
        this.clock = clock;
    }

    /** Apply darkness + lights over the current frame buffer. */
    public void apply(Graphics2D dst, Camera camera, int viewportW, int viewportH) {
        int ambient = ambientAlpha();
        if (ambient <= 0 && field.size() == 0) return;

        ensureOverlay(viewportW, viewportH);
        Graphics2D og = overlay.createGraphics();
        og.setColor(new Color(0, 0, 0, ambient));
        og.fillRect(0, 0, viewportW, viewportH);
        punchLights(og, camera);
        og.dispose();

        dst.drawImage(overlay, 0, 0, null);
    }

    private void ensureOverlay(int viewportW, int viewportH) {
        if (overlay == null || overlayW != viewportW || overlayH != viewportH) {
            overlayW = viewportW;
            overlayH = viewportH;
            overlay = new BufferedImage(viewportW, viewportH, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private void punchLights(Graphics2D og, Camera camera) {
        og.setComposite(AlphaComposite.DstOut);
        int camX = (int) camera.getWorldX();
        int camY = (int) camera.getWorldY();
        for (LightSource s : field.sources()) {
            int cx = s.worldCenterX() - camX;
            int cy = s.worldCenterY() - camY;
            int r  = Math.max(1, s.radius());
            Color[] colors = new Color[]{ HOLE_OPAQUE, blend(HOLE_HALF, s.intensity()), HOLE_CLEAR };
            RadialGradientPaint paint = new RadialGradientPaint(cx, cy, r, FALLOFF_STOPS, colors);
            og.setPaint(paint);
            og.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
    }

    /**
     * Continuous ambient darkness alpha (0=full day, 220=deep night).
     *
     * Why interpolated: discrete per-phase values produced a visible step at
     * each phase boundary. A cosine ramp matches GameClock.dayPhase() smoothly
     * — minimum darkness at p=0.25 (mid-day), maximum at p=0.75 (mid-night).
     */
    private int ambientAlpha() {
        // Light changes disabled for now: keep time-of-day static (full daylight,
        // no ambient darkness overlay). Restore the cosine ramp below to re-enable.
        return 0;
        // double p = clock.dayPhase();
        // double cos = Math.cos((p - 0.25) * 2.0 * Math.PI);
        // double t = (1.0 - cos) * 0.5;
        // return (int) Math.round(t * MAX_NIGHT_ALPHA);
    }

    private static final int MAX_NIGHT_ALPHA = 220;

    private Color blend(Color base, float intensity) {
        int a = Math.round(base.getAlpha() * Math.max(0f, Math.min(1f, intensity)));
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
    }
}
