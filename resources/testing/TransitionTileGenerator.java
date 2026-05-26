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
        // shallowWater borders (drawn on ocean tiles).
        // Color = lighter ocean variant; mask = the crescent already used for wet-beach edges.
        generatePair("shallowWater", "oceanT_light.png", "wetBeachB1.png", "WetBeachC0.png", false);

        // Animation frames for shallowWater. ocean0_light is a textured wave frame;
        // oceanT_light is a flat color swatch — using it would give a blank middle frame.
        copyAs("ocean0_light.png", "shallowWater0.png");
        copyAs("ocean1_light.png", "shallowWater1.png");
        copyAs("ocean2_light.png", "shallowWater2.png");

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

    /** Generate both the edge ({@code outB1.png}) and the corner ({@code outC0.png}). */
    public static void generatePair(String outBaseName, String colorSrc,
                                    String edgeMask, String cornerMask,
                                    boolean genSuffix) throws IOException {
        generateEdge(outBaseName, colorSrc, edgeMask, genSuffix);
        generateCorner(outBaseName, colorSrc, cornerMask, genSuffix);
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
