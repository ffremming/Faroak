package resources.testing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Generates color-shifted variants of tile sprites. Reads PNGs from
 * {@code resources/images/tile/}, multiplies brightness by a factor,
 * and writes a sibling file with the chosen suffix (e.g. {@code _light},
 * {@code _dark}). Alpha is preserved.
 *
 * Usage:
 *   javac -d /tmp/gen resources/testing/SpriteVariantGenerator.java
 *   java -cp /tmp/gen resources.testing.SpriteVariantGenerator
 *
 * Or invoke {@link #generate(List, String, double)} programmatically.
 */
public final class SpriteVariantGenerator {

    private static final String TILE_DIR = "resources/images/tile/";

    private SpriteVariantGenerator() {}

    public static void main(String[] args) throws IOException {
        List<String> water = Arrays.asList(
            "ocean0.png", "ocean1.png", "ocean2.png", "oceanT.png"
        );
        List<String> sand = Arrays.asList(
            "beach.png",
            "beach1B1.png", "beach1C0.png",
            "beach2B1.png", "beach2C0.png",
            "beachB1.png",  "beachC0.png",
            "wetBeach.png",
            "wetBeach1B1.png", "wetBeach1C0.png",
            "wetBeachB1.png",  "WetBeachC0.png",
            "sandBorderW.png"
        );

        int w = generate(water, "_light", 1.25);
        int s = generate(sand,  "_dark",  0.78);
        System.out.println("wrote " + w + " lighter water + " + s + " darker sand variants to " + TILE_DIR);
    }

    /**
     * For every {@code file} in {@code files}, write {@code <base><suffix>.png}
     * with each pixel's brightness multiplied by {@code factor} (clamped 0..255).
     * Returns the number of files written.
     */
    public static int generate(List<String> files, String suffix, double factor) throws IOException {
        int written = 0;
        for (String file : files) {
            File in = new File(TILE_DIR + file);
            if (!in.exists()) {
                System.out.println("skip (missing): " + file);
                continue;
            }
            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                System.out.println("skip (unreadable): " + file);
                continue;
            }
            BufferedImage out = shiftBrightness(src, factor);
            File outFile = new File(TILE_DIR + withSuffix(file, suffix));
            ImageIO.write(out, "png", outFile);
            System.out.println("  " + file + " -> " + outFile.getName());
            written++;
        }
        return written;
    }

    /**
     * Multiply RGB by {@code factor} ONLY for "sand-like" pixels — those whose
     * red and green channels both exceed blue by at least {@code warmth}. Leaves
     * foam (near-white or blue-ish pixels) untouched so the wave highlight on
     * border crescents stays crisp.
     *
     * Returns the number of pixels actually recolored (useful for sanity checks).
     */
    public static int darkenSandOnly(BufferedImage src, BufferedImage dst, double factor, int warmth) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = src.getRGB(0, 0, w, h, null, 0, w);
        int hit = 0;
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >>> 24) & 0xFF;
            if (a == 0) continue;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >>  8) & 0xFF;
            int b =  argb        & 0xFF;
            boolean isSand = (r - b) >= warmth && (g - b) >= warmth;
            if (!isSand) continue;
            int nr = clamp((int) Math.round(r * factor));
            int ng = clamp((int) Math.round(g * factor));
            int nb = clamp((int) Math.round(b * factor));
            pixels[i] = (a << 24) | (nr << 16) | (ng << 8) | nb;
            hit++;
        }
        dst.setRGB(0, 0, w, h, pixels, 0, w);
        return hit;
    }

    /**
     * Replace the RGB of every "sand-like" pixel in {@code src} with the RGB
     * sampled from {@code colorSrc} at the same coordinate. Foam (cool/neutral
     * pixels failing the warmth test) is left untouched, preserving wave
     * highlights.
     *
     * Writes the result to {@code TILE_DIR/outName}.
     */
    public static int swapSandColor(String srcName, String colorSrcName, String outName, int warmth) throws IOException {
        BufferedImage src      = readTile(srcName);
        BufferedImage colorSrc = readTile(colorSrcName);
        if (src == null || colorSrc == null) {
            System.out.println("skip " + outName + " (missing source)");
            return 0;
        }
        int w = src.getWidth(), h = src.getHeight();
        int[] srcPx = src.getRGB(0, 0, w, h, null, 0, w);
        // colorSrc is sampled per-pixel below; tiled if smaller than src.
        int cw = colorSrc.getWidth(), ch = colorSrc.getHeight();
        int hit = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int argb = srcPx[i];
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >>  8) & 0xFF;
                int b =  argb        & 0xFF;
                if ((r - b) < warmth || (g - b) < warmth) continue;
                int c = colorSrc.getRGB(x % cw, y % ch);
                srcPx[i] = (a << 24) | (c & 0x00FFFFFF);
                hit++;
            }
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, srcPx, 0, w);
        ImageIO.write(out, "png", new java.io.File(TILE_DIR + outName));
        System.out.println("  " + srcName + " sand-only <- " + colorSrcName + " -> " + outName + " (" + hit + " px)");
        return hit;
    }

    private static BufferedImage readTile(String fileName) throws IOException {
        java.io.File f = new java.io.File(TILE_DIR + fileName);
        return f.exists() ? ImageIO.read(f) : null;
    }

    /**
     * Build a foam-less variant of {@code srcName}: keeps sand pixels (warmth test),
     * recolors them to {@code colorSrcName}'s tone, and clears every other pixel
     * (foam/water highlights) to fully transparent.
     */
    public static int eraseNonSand(String srcName, String colorSrcName, String outName, int warmth) throws IOException {
        BufferedImage src      = readTile(srcName);
        BufferedImage colorSrc = readTile(colorSrcName);
        if (src == null || colorSrc == null) {
            System.out.println("skip " + outName + " (missing source)");
            return 0;
        }
        int w = src.getWidth(), h = src.getHeight();
        int[] srcPx = src.getRGB(0, 0, w, h, null, 0, w);
        int cw = colorSrc.getWidth(), ch = colorSrc.getHeight();
        int kept = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int argb = srcPx[i];
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >>  8) & 0xFF;
                int b =  argb        & 0xFF;
                boolean isSand = a > 0 && (r - b) >= warmth && (g - b) >= warmth;
                if (isSand) {
                    int c = colorSrc.getRGB(x % cw, y % ch);
                    srcPx[i] = (a << 24) | (c & 0x00FFFFFF);
                    kept++;
                } else {
                    srcPx[i] = 0; // fully transparent
                }
            }
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, srcPx, 0, w);
        ImageIO.write(out, "png", new java.io.File(TILE_DIR + outName));
        System.out.println("  " + srcName + " sand-keep <- " + colorSrcName + " -> " + outName + " (" + kept + " px)");
        return kept;
    }

    /** Multiply RGB channels by {@code factor}; clamp to [0,255]; keep alpha. */
    public static BufferedImage shiftBrightness(BufferedImage src, double factor) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = src.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >>> 24) & 0xFF;
            int r = clamp((int) Math.round(((argb >> 16) & 0xFF) * factor));
            int g = clamp((int) Math.round(((argb >>  8) & 0xFF) * factor));
            int b = clamp((int) Math.round(( argb        & 0xFF) * factor));
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    private static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    private static String withSuffix(String fileName, String suffix) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return fileName + suffix;
        return fileName.substring(0, dot) + suffix + fileName.substring(dot);
    }
}
