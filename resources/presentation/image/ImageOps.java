package resources.presentation.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import resources.domain.entity.Entity;

/**
 * Pure image-processing helpers — scaling, outlining, transparency reduction,
 * pixel-perfect intersection test. Stateless; safe to call from anywhere.
 */
public final class ImageOps {

    private ImageOps() {}

    /** Nearest-neighbour scale to (w,h). Preserves pixel-art crispness. */
    public static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        AffineTransform tx = AffineTransform.getScaleInstance(
            (double) w / src.getWidth(),
            (double) h / src.getHeight());
        new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(src, out);
        g.dispose();
        return out;
    }

    /** Trace a white outline around the opaque pixels of {@code src}. */
    public static BufferedImage outline(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int w = src.getWidth(), h = src.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (!isOpaque(src, x, y)) continue;
                markBorderPixels(src, out, x, y, w, h);
            }
        }
        return out;
    }

    private static void markBorderPixels(BufferedImage src, BufferedImage out, int x, int y, int w, int h) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) {
                    out.setRGB(x, y, Color.WHITE.getRGB());
                } else if (!isOpaque(src, nx, ny)) {
                    out.setRGB(nx, ny, Color.WHITE.getRGB());
                }
            }
        }
    }

    private static boolean isOpaque(BufferedImage img, int x, int y) {
        return ((img.getRGB(x, y) >> 24) & 0xFF) > 76; // ~30% alpha threshold
    }

    /** Knock alpha down to 30% — used for translucent placement previews. */
    public static BufferedImage reduceTransparency(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        int[] pixels = src.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int alpha = (argb >>> 24) & 0xFF;
            int reducedAlpha = (alpha * 30) / 100;
            pixels[i] = (reducedAlpha << 24) | (argb & 0x00FFFFFF);
        }
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, width, height, pixels, 0, width);
        return out;
    }

    /** Rotate by {@code degrees}. Used for tile sprite variants. */
    public static BufferedImage rotate(BufferedImage src, double degrees) {
        double radians = Math.toRadians(degrees);
        int w = src.getWidth(), h = src.getHeight();
        int newW = (int) Math.round(w * Math.abs(Math.cos(radians)) + h * Math.abs(Math.sin(radians)));
        int newH = (int) Math.round(h * Math.abs(Math.cos(radians)) + w * Math.abs(Math.sin(radians)));
        BufferedImage out = new BufferedImage(newW, newH, src.getType());
        Graphics2D g = out.createGraphics();
        AffineTransform tx = new AffineTransform();
        tx.translate(newW / 2.0, newH / 2.0);
        tx.rotate(radians);
        tx.translate(-w / 2.0, -h / 2.0);
        g.setTransform(tx);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Pixel-perfect intersection test between two entity sprites in world space. */
    public static boolean pixelIntersection(Entity a, Entity b) {
        if (a.images.isEmpty() || b.images.isEmpty()) return false;
        BufferedImage aImg = a.images.get(0), bImg = b.images.get(0);
        double aSx = (double) aImg.getWidth()  / a.getWidth();
        double aSy = (double) aImg.getHeight() / a.getHeight();
        double bSx = (double) bImg.getWidth()  / b.getWidth();
        double bSy = (double) bImg.getHeight() / b.getHeight();
        int needed = (a.getWidth() * a.getHeight()) / (aImg.getWidth() * aImg.getHeight()) * 150;
        int hits = countOverlap(a, b, aImg, bImg, aSx, aSy, bSx, bSy);
        return hits > needed;
    }

    private static int countOverlap(Entity a, Entity b, BufferedImage aImg, BufferedImage bImg,
                                    double aSx, double aSy, double bSx, double bSy) {
        int hits = 0;
        for (int x = 0; x < aImg.getWidth(); x++) {
            for (int y = 0; y < aImg.getHeight(); y++) {
                if (!isOpaque(aImg, x, y)) continue;
                int wx = (int) (x / aSx + a.getWorldX());
                int wy = (int) (y / aSy + a.getWorldY());
                int px = (int) ((wx - b.getWorldX()) * bSx);
                int py = (int) ((wy - b.getWorldY()) * bSy);
                if (px >= 0 && px < bImg.getWidth() && py >= 0 && py < bImg.getHeight()
                    && isOpaque(bImg, px, py)) hits++;
            }
        }
        return hits;
    }
}
