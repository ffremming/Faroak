package resources.testing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Generates transition (border + corner) tiles from a base sprite and a mask
 * sprite. The mask sprite supplies the alpha shape of the edge/corner; the
 * base sprite supplies the RGB color. Output pixel = base.RGB combined with
 * mask.alpha (multiplied with base.alpha if base has transparency).
 *
 * Convention (see {@link resources.domain.tile.TileBorderResolver}):
 *   - "<tile>B1.png" is the canonical edge sprite (rotated 90°/180°/270° at runtime
 *     to produce B2/B3/B4).
 *   - "<tile>C0.png" is the canonical corner sprite (rotated for C1..C4).
 *
 * So generating beach→wetBeach transitions = produce beachB1.png and beachC0.png
 * with the beach color filling the same crescent shape used by wetBeachB1.png /
 * wetBeachC0.png.
 *
 * Usage:
 *   javac -d /tmp/gen resources/testing/TransitionTileGenerator.java
 *   java  -cp /tmp/gen resources.testing.TransitionTileGenerator
 *
 * Output is written with a "_gen" suffix so artist-authored sprites are not
 * clobbered. Rename to drop "_gen" once you've eyeballed the result.
 */
public final class TransitionTileGenerator {

    private static final String TILE_DIR = "resources/images/tile/";

    private TransitionTileGenerator() {}

    public static void main(String[] args) throws IOException {
        // --- New intermediate water tier: midWater (between shallow and medium) ---
        // Real tile, generated as a depth band. No new artist art: derive its three
        // animation frames by recolouring the shallow frames toward the shallow↔medium
        // midpoint teal, at an alpha between the two tiers so the seabed still shows
        // through. Must run before the crescents below (they read midWater0..2).
        generateMidWater();

        // --- Procedural ocean depth transitions (ANIMATED + TRANSLUCENT) ---
        // A shallower tier's water creeps onto the deeper neighbour. The resolver
        // draws "<neighbour>[frame]B<side>" on the lower tile, so:
        //   medium     draws shallowWater[f]B* / mid draws shallowWater… etc.
        //   ocean      draws mediumWater[f]B*
        // The crescent's COLOUR SOURCE is the neighbour's actual animation FRAME, not
        // an opaque swatch — so the crescent (a) carries the tier's baked translucency
        // (the seabed shows through it, like the water body) and (b) ANIMATES, cycling
        // shallowWaterB1 → shallowWater1B1 → shallowWater2B1 in lock-step with the
        // water it edges. The wavy wetBeach silhouette + softened inner edge are kept.
        generateAnimatedSoftPair("shallowWater", "wetBeachB1.png", "WetBeachC0.png");
        generateAnimatedSoftPair("midWater",     "wetBeachB1.png", "WetBeachC0.png");
        generateAnimatedSoftPair("mediumWater",  "wetBeachB1.png", "WetBeachC0.png");

        // Darkened sand source for the wetBeach BODY (×0.85 of beach reads as "wet sand").
        // The 4 border/corner sprites (wetBeachB1, WetBeachC0, wetBeach1B1, wetBeach1C0)
        // are artist-painted and include wave foam — do NOT regenerate them, they'd lose
        // the foam outline.
        SpriteVariantGenerator.generate(java.util.Collections.singletonList("beach.png"), "_wet", 0.85);
        copyAs("beach_wet.png", "wetBeach.png");
    }

    /** Verbatim copy under a new name (used for animation-frame aliases). */
    private static void copyAs(String srcName, String dstName) throws IOException {
        BufferedImage src = read(srcName);
        if (src == null) { System.out.println("skip " + dstName + " (source missing)"); return; }
        ImageIO.write(src, "png", new File(TILE_DIR + dstName));
        System.out.println("  " + srcName + " -> " + dstName);
    }

    // ----------------------------------------------------- midWater (new tier)

    /**
     * Target body colour for midWater: the midpoint between the shallow
     * (≈98,192,186) and medium (≈21,129,150) tier colours — the teal the old
     * shallow↔medium border read as.
     */
    private static final int MID_WATER_RGB = (60 << 16) | (160 << 8) | 168;
    /** Alpha between shallow (140) and medium (199) so the seabed still shows through. */
    private static final int MID_WATER_ALPHA = 170;

    /**
     * Derive midWater's three animation frames from shallowWater's by recolouring
     * each visible pixel toward {@link #MID_WATER_RGB} (preserving the per-pixel
     * texture excursion around shallow's mean) and setting the baked alpha to
     * {@link #MID_WATER_ALPHA}. Also writes {@code midWater_opaque.png}, the full
     * alpha colour swatch used as the crescent colour source.
     */
    public static void generateMidWater() throws IOException {
        int targetR = (MID_WATER_RGB >> 16) & 0xFF;
        int targetG = (MID_WATER_RGB >>  8) & 0xFF;
        int targetB =  MID_WATER_RGB        & 0xFF;
        // shallow mean (from the frames) — recolour shifts this mean to the target
        // while keeping each pixel's deviation, so wave texture survives.
        final int meanR = 98, meanG = 192, meanB = 186;
        int dr = targetR - meanR, dg = targetG - meanG, db = targetB - meanB;

        for (int f = 0; f < 3; f++) {
            BufferedImage src = read("shallowWater" + f + ".png");
            if (src == null) { System.out.println("skip midWater" + f + " (shallow frame missing)"); continue; }
            int w = src.getWidth(), h = src.getHeight();
            int[] in = src.getRGB(0, 0, w, h, null, 0, w);
            int[] out = new int[w * h];
            for (int i = 0; i < in.length; i++) {
                int a = (in[i] >>> 24) & 0xFF;
                if (a == 0) { out[i] = 0; continue; }
                int r = clampByte(((in[i] >> 16) & 0xFF) + dr);
                int g = clampByte(((in[i] >>  8) & 0xFF) + dg);
                int b = clampByte(( in[i]        & 0xFF) + db);
                out[i] = (MID_WATER_ALPHA << 24) | (r << 16) | (g << 8) | b;
            }
            writeArgb(out, w, h, "midWater" + f + ".png");
        }
        // opaque swatch for the crescent colour source
        int[] swatch = new int[32 * 32];
        for (int i = 0; i < swatch.length; i++) swatch[i] = 0xFF000000 | (MID_WATER_RGB & 0x00FFFFFF);
        writeArgb(swatch, 32, 32, "midWater_opaque.png");
        System.out.println("  midWater frames + opaque swatch (rgb="
                + targetR + "," + targetG + "," + targetB + " a=" + MID_WATER_ALPHA + ")");
    }

    private static int clampByte(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    private static void writeArgb(int[] px, int w, int h, String name) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, px, 0, w);
        ImageIO.write(img, "png", new File(TILE_DIR + name));
    }

    /** Generate both the edge ({@code outB1.png}) and the corner ({@code outC0.png}). */
    public static void generatePair(String outBaseName, String colorSrc,
                                    String edgeMask, String cornerMask,
                                    boolean genSuffix) throws IOException {
        generateEdge(outBaseName, colorSrc, edgeMask, genSuffix);
        generateCorner(outBaseName, colorSrc, cornerMask, genSuffix);
    }

    /** Pixels over which the crescent's inner edge feathers out. Small = stays a crescent. */
    private static final int INNER_FEATHER_PX = 2;

    /**
     * Like {@link #generatePair} but feathers the crescent's INNER edge so the
     * transition into the deeper water isn't a hard cut. The wavy outer silhouette
     * and the solid tier colour are unchanged — only the alpha within {@link
     * #INNER_FEATHER_PX} of the crescent's inner boundary is ramped down.
     */
    public static void generateSoftenedPair(String outBaseName, String colorSrc,
                                            String edgeMask, String cornerMask) throws IOException {
        composeSoftened(outBaseName + "B1.png", colorSrc, edgeMask);
        composeSoftened(outBaseName + "C0.png", colorSrc, cornerMask);
    }

    /** Animation frames the resolver cycles for water borders (matches base water). */
    private static final int WATER_FRAMES = 3;

    /**
     * Generate the ANIMATED, TRANSLUCENT crescent set for a water tier. For each of
     * the {@link #WATER_FRAMES} animation frames the colour source is that tier's
     * own water frame ({@code <tier>0/1/2.png}) — which carries the baked per-tier
     * alpha — so the crescent is as translucent as the water body (seabed shows
     * through) and changes every frame in lock-step with it.
     *
     * Output names match the resolver's frame convention (see {@code borderKey}):
     *   frame 0 → {@code <tier>B1} / {@code <tier>C0}
     *   frame f → {@code <tier>fB1} / {@code <tier>fC0}
     */
    public static void generateAnimatedSoftPair(String tier, String edgeMask, String cornerMask) throws IOException {
        for (int f = 0; f < WATER_FRAMES; f++) {
            String frameSrc = tier + f + ".png";        // translucent animated water frame
            String suffix   = (f == 0) ? "" : String.valueOf(f);
            composeSoftened(tier + suffix + "B1.png", frameSrc, edgeMask);
            composeSoftened(tier + suffix + "C0.png", frameSrc, cornerMask);
        }
    }

    /**
     * Stamp the tier colour (from the animated water frame, so the crescent is as
     * translucent as the water body and animates with it) through the wavy mask,
     * feathering the inner edge a couple of pixels so the silhouette isn't jagged.
     *
     * NOTE: no rim/line and no extra opacity — the crescent is a plain translucent
     * patch of the shallower tier's colour. The resolver FLATTENS it into the base
     * water with replace-semantics ({@code WaterLayerFlattener}) so the crescent
     * region shifts colour without stacking alpha (no darker band, no outline).
     */
    public static void composeSoftened(String outName, String colorSrc, String maskFile) throws IOException {
        BufferedImage color = read(colorSrc);
        BufferedImage mask  = read(maskFile);
        if (color == null || mask == null) {
            System.out.println("skip " + outName + " (color or mask missing)");
            return;
        }
        int w = mask.getWidth(), h = mask.getHeight();
        BufferedImage scaledColor = nearestScale(color, w, h);
        BufferedImage composed = applyMaskAlpha(scaledColor, mask);

        int[] px = composed.getRGB(0, 0, w, h, null, 0, w);
        int[] dist = innerDistance(px, w, h);
        int[] out = new int[w * h];
        for (int i = 0; i < out.length; i++) {
            int a = (px[i] >>> 24) & 0xFF;
            if (a == 0) { out[i] = 0; continue; }
            double f = Math.min(1.0, dist[i] / (double) INNER_FEATHER_PX);
            int na = (int) Math.round(a * f);
            out[i] = (na << 24) | (px[i] & 0x00FFFFFF);
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, out, 0, w);
        ImageIO.write(img, "png", new File(TILE_DIR + outName));
        System.out.println("  softened " + outName + " (feather=" + INNER_FEATHER_PX + "px)");
    }

    /**
     * For each opaque pixel, the Chebyshev distance (in px) to the nearest
     * transparent pixel — capped at {@link #INNER_FEATHER_PX}+1 since we only need
     * the rim. Transparent pixels get 0. Two-pass chamfer over the small tile.
     */
    private static int[] innerDistance(int[] px, int w, int h) {
        int cap = INNER_FEATHER_PX + 1;
        int[] d = new int[w * h];
        for (int i = 0; i < d.length; i++) d[i] = ((px[i] >>> 24) & 0xFF) == 0 ? 0 : cap;
        // forward pass
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (d[i] == 0) continue;
                int best = d[i];
                if (x > 0)            best = Math.min(best, d[i - 1] + 1);
                if (y > 0)            best = Math.min(best, d[i - w] + 1);
                if (x > 0 && y > 0)   best = Math.min(best, d[i - w - 1] + 1);
                if (x < w - 1 && y > 0) best = Math.min(best, d[i - w + 1] + 1);
                d[i] = best;
            }
        }
        // backward pass
        for (int y = h - 1; y >= 0; y--) {
            for (int x = w - 1; x >= 0; x--) {
                int i = y * w + x;
                if (d[i] == 0) continue;
                int best = d[i];
                if (x < w - 1)          best = Math.min(best, d[i + 1] + 1);
                if (y < h - 1)          best = Math.min(best, d[i + w] + 1);
                if (x < w - 1 && y < h - 1) best = Math.min(best, d[i + w + 1] + 1);
                if (x > 0 && y < h - 1) best = Math.min(best, d[i + w - 1] + 1);
                d[i] = best;
            }
        }
        return d;
    }

    public static void generateEdge(String outBaseName, String colorSrc, String maskFile, boolean genSuffix) throws IOException {
        compose(outBaseName + "B1" + (genSuffix ? "_gen" : "") + ".png", colorSrc, maskFile);
    }

    public static void generateCorner(String outBaseName, String colorSrc, String maskFile, boolean genSuffix) throws IOException {
        compose(outBaseName + "C0" + (genSuffix ? "_gen" : "") + ".png", colorSrc, maskFile);
    }

    /**
     * Write {@code TILE_DIR/outName} where each pixel is the RGB of {@code colorSrc}
     * (scaled to the mask's dimensions, nearest-neighbour) carrying the alpha of
     * {@code maskFile}.
     */
    public static void compose(String outName, String colorSrc, String maskFile) throws IOException {
        BufferedImage color = read(colorSrc);
        BufferedImage mask  = read(maskFile);
        if (color == null || mask == null) {
            System.out.println("skip " + outName + " (color or mask missing)");
            return;
        }
        BufferedImage scaledColor = nearestScale(color, mask.getWidth(), mask.getHeight());
        BufferedImage out = applyMaskAlpha(scaledColor, mask);
        File outFile = new File(TILE_DIR + outName);
        ImageIO.write(out, "png", outFile);
        System.out.println("  " + colorSrc + " + " + maskFile + " -> " + outFile.getName());
    }

    private static BufferedImage read(String fileName) throws IOException {
        File f = new File(TILE_DIR + fileName);
        if (!f.exists()) return null;
        return ImageIO.read(f);
    }

    private static BufferedImage applyMaskAlpha(BufferedImage color, BufferedImage mask) {
        int w = mask.getWidth(), h = mask.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] colorPx = color.getRGB(0, 0, w, h, null, 0, w);
        int[] maskPx  = mask .getRGB(0, 0, w, h, null, 0, w);
        int[] outPx   = new int[w * h];
        for (int i = 0; i < outPx.length; i++) {
            int colorA = (colorPx[i] >>> 24) & 0xFF;
            int maskA  = (maskPx[i]  >>> 24) & 0xFF;
            int finalA = (colorA * maskA) / 255;
            outPx[i] = (finalA << 24) | (colorPx[i] & 0x00FFFFFF);
        }
        out.setRGB(0, 0, w, h, outPx, 0, w);
        return out;
    }

    private static BufferedImage nearestScale(BufferedImage src, int w, int h) {
        if (src.getWidth() == w && src.getHeight() == h) return src;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        double sx = (double) src.getWidth()  / w;
        double sy = (double) src.getHeight() / h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int srcX = (int) (x * sx);
                int srcY = (int) (y * sy);
                out.setRGB(x, y, src.getRGB(srcX, srcY));
            }
        }
        return out;
    }
}
