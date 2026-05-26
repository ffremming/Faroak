package resources.testing.probes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import resources.app.GameContext;

/**
 * Renders a single frame to a BufferedImage and lets callers query pixels.
 *
 * Headless-only — same software pipeline as {@code RenderPerfProbe}. Used by
 * the visual-regression probes to assert that something actually painted at
 * the expected screen coordinates (not just that the entity list contained
 * the expected reference).
 */
final class RenderSampler {

    private RenderSampler() {}

    /** Render one frame into a fresh BufferedImage sized to the screen. */
    static BufferedImage render(GameContext ctx) {
        // Match the field used by the live game so the panel reports a
        // non-zero width/height — UserInterface.draw reads panel.width/height
        // when sizing the inventory + HUD, and renders nothing useful when
        // they're zero. Forwarding the constant screen size matches what the
        // EnvironmentManager's first updatePanelDimensions tick would do.
        if (ctx.player() != null) {
            ctx.player().panel.width  = ctx.screenWidth();
            ctx.player().panel.height = ctx.screenHeight();
        }
        BufferedImage canvas = new BufferedImage(
            ctx.screenWidth(), ctx.screenHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        try {
            ctx.camera().draw(g2);
        } finally {
            g2.dispose();
        }
        return canvas;
    }

    /** RGB at (x,y), or null if out of bounds. */
    static Color at(BufferedImage img, int x, int y) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return null;
        return new Color(img.getRGB(x, y), true);
    }

    /** Count how many distinct RGB values appear in the given rectangle. */
    static int distinctColorsIn(BufferedImage img, int x, int y, int w, int h) {
        java.util.HashSet<Integer> seen = new java.util.HashSet<>();
        int x2 = Math.min(x + w, img.getWidth());
        int y2 = Math.min(y + h, img.getHeight());
        for (int yy = Math.max(0, y); yy < y2; yy++) {
            for (int xx = Math.max(0, x); xx < x2; xx++) {
                seen.add(img.getRGB(xx, yy) & 0x00FFFFFF);
            }
        }
        return seen.size();
    }

    /** True if any non-transparent + non-pure-black pixel exists in the rect. */
    static boolean hasNonBackgroundPixel(BufferedImage img, int x, int y, int w, int h) {
        int x2 = Math.min(x + w, img.getWidth());
        int y2 = Math.min(y + h, img.getHeight());
        for (int yy = Math.max(0, y); yy < y2; yy++) {
            for (int xx = Math.max(0, x); xx < x2; xx++) {
                int argb = img.getRGB(xx, yy);
                int a = (argb >>> 24) & 0xFF;
                int rgb = argb & 0x00FFFFFF;
                if (a > 0 && rgb != 0x000000) return true;
            }
        }
        return false;
    }
}
