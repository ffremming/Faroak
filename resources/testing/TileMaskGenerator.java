package resources.testing;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import resources.generation.noise.OpenSimplex2S;

/**
 * Procedurally generates greyscale alpha masks used to build tile sprites:
 *   - 20 "texture" masks (grassy, gravel, rocky, mossy, ...): full-tile alpha
 *     patterns that, when laid over a base color, give the tile its surface look.
 *   - Per-texture B1 + C0 "border" masks: the crescent / corner shapes consumed by
 *     {@link resources.domain.tile.TileBorderResolver}, intersected with the
 *     texture pattern so the border reads as the same material as the body.
 *   - A small set of "micro" masks: scattered specks, cracks, pebbles, flowers —
 *     drawn ON TOP of a finished tile to break up tiling repetition.
 *
 * Masks are pure alpha: RGB is white, only the alpha channel varies. The
 * downstream compositor multiplies them against a base color sprite (see
 * {@link TransitionTileGenerator#compose}). This keeps the masks reusable across
 * any color palette.
 *
 * Output:
 *   resources/images/masks/texture/&lt;name&gt;.png        (20 files)
 *   resources/images/masks/border/&lt;name&gt;B1.png       (20 files)
 *   resources/images/masks/border/&lt;name&gt;C0.png       (20 files)
 *   resources/images/masks/micro/&lt;name&gt;.png          (8 files)
 *   resources/images/masks/examples/&lt;name&gt;_demo.png  (composed demos)
 *
 * Usage:
 *   javac -d /tmp/gen $(find resources -name "*.java")
 *   java  -cp /tmp/gen resources.testing.TileMaskGenerator
 */
public final class TileMaskGenerator {

    private static final int TILE = 32;
    private static final String ROOT = "resources/images/masks/";
    private static final String TEXTURE_DIR  = ROOT + "texture/";
    private static final String BORDER_DIR   = ROOT + "border/";
    private static final String MICRO_DIR    = ROOT + "micro/";
    private static final String EXAMPLES_DIR = ROOT + "examples/";
    private static final String TILE_DIR     = "resources/images/tile/";

    private TileMaskGenerator() {}

    public static void main(String[] args) throws IOException {
        mkdirs();

        TextureSpec[] textures = textureSpecs();
        for (TextureSpec spec : textures) {
            BufferedImage tex = renderTexture(spec);
            writePng(tex, TEXTURE_DIR + spec.name + ".png");

            BufferedImage edge = intersectWithShape(tex, edgeShape());
            writePng(edge, BORDER_DIR + spec.name + "B1.png");

            BufferedImage corner = intersectWithShape(tex, cornerShape());
            writePng(corner, BORDER_DIR + spec.name + "C0.png");
        }

        for (MicroSpec spec : microSpecs()) {
            BufferedImage micro = renderMicro(spec);
            writePng(micro, MICRO_DIR + spec.name + ".png");
        }

        writeExamples(textures);

        TileSpec[] tiles = tileSpecs();
        for (TileSpec t : tiles) composeTile(t);

        System.out.println("done: " + textures.length + " textures, "
                + microSpecs().length + " micro masks, " + tiles.length
                + " composed tiles in " + TILE_DIR);
    }

    // ---------------------------------------------------------------- specs

    /**
     * Parameters that drive a texture mask's procedural look.
     *   freq     – noise frequency (higher = finer detail)
     *   octaves  – fBm octaves (more = bushier)
     *   threshold – alpha cut-off in [0,1]; pixels below go transparent
     *   contrast – multiplier applied after thresholding (gives crisper edges)
     *   density  – maximum alpha for kept pixels (lower = sparser overlay)
     *   style    – which procedural primitive to use
     *   seed     – deterministic seed per texture
     */
    private record TextureSpec(String name, Style style, double freq, int octaves,
                               double threshold, double contrast, double density, long seed) {}

    private enum Style {
        WISPY,     // streak-like, grass blades
        BLOBS,     // soft round patches, moss / lichen
        SPECKS,    // scattered dots, gravel / sand
        CRACKS,    // angular fissures, rock / dried mud
        VEINS,     // long flowing strokes, marble / ice
        CLOUDY     // smooth large-scale variation, dust / snow
    }

    private static TextureSpec[] textureSpecs() {
        return new TextureSpec[] {
            new TextureSpec("grassy",     Style.WISPY,  0.55, 3, 0.42, 1.4,  0.85, 1001L),
            new TextureSpec("tallGrass",  Style.WISPY,  0.38, 3, 0.48, 1.6,  0.95, 1002L),
            new TextureSpec("dryGrass",   Style.WISPY,  0.60, 2, 0.50, 1.5,  0.75, 1003L),
            new TextureSpec("mossy",      Style.BLOBS,  0.30, 4, 0.40, 1.2,  0.90, 1004L),
            new TextureSpec("lichen",     Style.BLOBS,  0.55, 2, 0.55, 1.4,  0.70, 1005L),
            new TextureSpec("gravel",     Style.SPECKS, 1.40, 1, 0.45, 1.8,  0.85, 1006L),
            new TextureSpec("coarseGravel", Style.SPECKS, 0.90, 1, 0.50, 2.0, 0.95, 1007L),
            new TextureSpec("sandy",      Style.SPECKS, 1.80, 1, 0.30, 1.2,  0.55, 1008L),
            new TextureSpec("rocky",      Style.CRACKS, 0.35, 3, 0.35, 1.6,  0.95, 1009L),
            new TextureSpec("crackedRock", Style.CRACKS, 0.55, 2, 0.45, 2.0, 1.00, 1010L),
            new TextureSpec("pebbled",    Style.BLOBS,  0.85, 1, 0.45, 1.5,  0.80, 1011L),
            new TextureSpec("muddy",      Style.CLOUDY, 0.25, 3, 0.30, 1.1,  0.85, 1012L),
            new TextureSpec("dirt",       Style.CLOUDY, 0.40, 3, 0.30, 1.1,  0.90, 1013L),
            new TextureSpec("ashen",      Style.CLOUDY, 0.20, 4, 0.25, 1.0,  0.80, 1014L),
            new TextureSpec("snowy",      Style.CLOUDY, 0.18, 3, 0.20, 0.9,  0.95, 1015L),
            new TextureSpec("icy",        Style.VEINS,  0.30, 3, 0.50, 1.5,  0.75, 1016L),
            new TextureSpec("marble",     Style.VEINS,  0.25, 3, 0.55, 1.6,  0.85, 1017L),
            new TextureSpec("leafy",      Style.BLOBS,  0.45, 3, 0.50, 1.3,  0.80, 1018L),
            new TextureSpec("rootMat",    Style.VEINS,  0.40, 2, 0.45, 1.4,  0.80, 1019L),
            new TextureSpec("bouldery",   Style.BLOBS,  0.20, 2, 0.45, 1.5,  0.95, 1020L),
        };
    }

    /**
     * Micro masks: sparse overlays drawn on top of a finished tile to add
     * highlight detail (a few flowers, a couple of pebbles, a hairline crack).
     */
    private record MicroSpec(String name, Style style, double freq, double threshold,
                             double density, long seed) {}

    private static MicroSpec[] microSpecs() {
        return new MicroSpec[] {
            new MicroSpec("flowers",   Style.SPECKS, 1.20, 0.85, 0.95, 2001L),
            new MicroSpec("pebbles",   Style.SPECKS, 0.90, 0.78, 0.85, 2002L),
            new MicroSpec("twigs",     Style.WISPY,  0.80, 0.75, 0.80, 2003L),
            new MicroSpec("mushrooms", Style.BLOBS,  1.10, 0.82, 0.90, 2004L),
            new MicroSpec("cracks",    Style.CRACKS, 0.60, 0.85, 0.85, 2005L),
            new MicroSpec("scratches", Style.VEINS,  0.55, 0.80, 0.70, 2006L),
            new MicroSpec("dustMotes", Style.SPECKS, 2.00, 0.88, 0.60, 2007L),
            new MicroSpec("mossPatch", Style.BLOBS,  0.55, 0.80, 0.85, 2008L),
        };
    }

    // ---------------------------------------------------------------- render

    private static BufferedImage renderTexture(TextureSpec s) {
        BufferedImage out = blank();
        int[] px = new int[TILE * TILE];
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                double v = sample(s.style, s.seed, x, y, s.freq, s.octaves);
                double a = shape(v, s.threshold, s.contrast) * s.density;
                px[y * TILE + x] = alphaToWhite(a);
            }
        }
        out.setRGB(0, 0, TILE, TILE, px, 0, TILE);
        return out;
    }

    private static BufferedImage renderMicro(MicroSpec s) {
        BufferedImage out = blank();
        int[] px = new int[TILE * TILE];
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                double v = sample(s.style, s.seed, x, y, s.freq, 2);
                double a = shape(v, s.threshold, 2.0) * s.density;
                px[y * TILE + x] = alphaToWhite(a);
            }
        }
        out.setRGB(0, 0, TILE, TILE, px, 0, TILE);
        return out;
    }

    private static BufferedImage blank() {
        return new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Style-specific noise primitive. All produce a value in roughly [0, 1].
     *
     * fBm is plain octave sum. Cracks/veins use the "ridged" variant
     * (1 - |noise|) which concentrates high values along zero-crossings — those
     * read as fissures or strokes. Wispy multiplies a horizontal stretched
     * noise by a directional gradient so the result reads as vertical blades.
     */
    private static double sample(Style style, long seed, int x, int y, double freq, int octaves) {
        switch (style) {
            case WISPY:   return wispy(seed, x, y, freq, octaves);
            case BLOBS:   return fbm(seed, x * freq, y * freq, octaves);
            case SPECKS:  return specks(seed, x, y, freq);
            case CRACKS:  return ridged(seed, x * freq, y * freq, octaves);
            case VEINS:   return veins(seed, x, y, freq, octaves);
            case CLOUDY:  return fbm(seed, x * freq * 0.6, y * freq * 0.6, octaves);
            default:      return 0.0;
        }
    }

    private static double fbm(long seed, double x, double y, int octaves) {
        double sum = 0, amp = 1, norm = 0;
        for (int o = 0; o < octaves; o++) {
            sum  += amp * (OpenSimplex2S.noise2(seed + o, x, y) * 0.5 + 0.5);
            norm += amp;
            x *= 2.0; y *= 2.0; amp *= 0.5;
        }
        return sum / norm;
    }

    private static double ridged(long seed, double x, double y, int octaves) {
        double sum = 0, amp = 1, norm = 0;
        for (int o = 0; o < octaves; o++) {
            double n = OpenSimplex2S.noise2(seed + o, x, y);
            sum  += amp * (1.0 - Math.abs(n));
            norm += amp;
            x *= 2.0; y *= 2.0; amp *= 0.5;
        }
        return Math.pow(sum / norm, 4.0); // sharpen ridges
    }

    private static double wispy(long seed, int x, int y, double freq, int octaves) {
        // Stretch horizontally so noise lobes become vertical strokes, then
        // multiply by a row-modulated mask so blades clump in tufts.
        double n = fbm(seed, x * freq * 3.0, y * freq * 0.6, octaves);
        double tuft = 0.5 + 0.5 * OpenSimplex2S.noise2(seed + 99L, x * freq * 0.4, y * freq * 0.4);
        return n * tuft;
    }

    private static double specks(long seed, int x, int y, double freq) {
        double n = OpenSimplex2S.noise2(seed, x * freq, y * freq) * 0.5 + 0.5;
        double j = OpenSimplex2S.noise2(seed + 7L, x * freq * 2.3, y * freq * 2.3) * 0.5 + 0.5;
        return Math.pow(Math.max(n, j), 2.0);
    }

    private static double veins(long seed, int x, int y, double freq, int octaves) {
        double warpX = OpenSimplex2S.noise2(seed + 11L, x * freq * 0.5, y * freq * 0.5) * 4.0;
        double warpY = OpenSimplex2S.noise2(seed + 13L, x * freq * 0.5, y * freq * 0.5) * 4.0;
        return ridged(seed, (x + warpX) * freq, (y + warpY) * freq, octaves);
    }

    /**
     * Map a raw noise value in [0,1] to an alpha in [0,1]: pixels below
     * {@code threshold} go fully transparent; the rest are remapped to [0,1]
     * and a contrast curve is applied so edges read crisp.
     */
    private static double shape(double v, double threshold, double contrast) {
        if (v <= threshold) return 0.0;
        double t = (v - threshold) / (1.0 - threshold);
        t = Math.pow(t, 1.0 / Math.max(contrast, 0.01));
        return Math.min(1.0, t);
    }

    // -------------------------------------------------------- border shapes

    /**
     * Canonical "B1" crescent: a soft alpha gradient that fades from full
     * coverage at the top edge to zero a few pixels in. This matches what
     * existing B1 sprites look like (wetBeachB1 etc. follow the same idea).
     */
    private static BufferedImage edgeShape() {
        BufferedImage out = blank();
        int[] px = new int[TILE * TILE];
        int reach = TILE / 2;
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                double t = 1.0 - Math.min(1.0, (double) y / reach);
                double a = Math.pow(t, 1.3);
                px[y * TILE + x] = alphaToWhite(a);
            }
        }
        out.setRGB(0, 0, TILE, TILE, px, 0, TILE);
        return out;
    }

    /**
     * Canonical "C0" outer-corner: full alpha in the corner, fading toward
     * the opposite diagonal. Distance-to-corner with a smooth falloff.
     */
    private static BufferedImage cornerShape() {
        BufferedImage out = blank();
        int[] px = new int[TILE * TILE];
        double reach = TILE * 0.7;
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                double d = Math.sqrt(x * x + y * y); // distance from top-left corner
                double t = 1.0 - Math.min(1.0, d / reach);
                double a = Math.pow(t, 1.3);
                px[y * TILE + x] = alphaToWhite(a);
            }
        }
        out.setRGB(0, 0, TILE, TILE, px, 0, TILE);
        return out;
    }

    /** Pixel-wise alpha multiply: out.alpha = texture.alpha * gate.alpha. */
    private static BufferedImage intersectWithShape(BufferedImage texture, BufferedImage gate) {
        int w = TILE, h = TILE;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] a = texture.getRGB(0, 0, w, h, null, 0, w);
        int[] b = gate   .getRGB(0, 0, w, h, null, 0, w);
        int[] o = new int[w * h];
        for (int i = 0; i < o.length; i++) {
            int aa = (a[i] >>> 24) & 0xFF;
            int bb = (b[i] >>> 24) & 0xFF;
            int alpha = (aa * bb) / 255;
            o[i] = (alpha << 24) | 0x00FFFFFF;
        }
        out.setRGB(0, 0, w, h, o, 0, w);
        return out;
    }

    // ---------------------------------------------------------- demo output

    /**
     * For each texture, write a 3-up demo image showing:
     *   [base color] [texture mask over base] [B1 + C0 over base]
     * so you can eyeball whether the procedural parameters read right.
     */
    private static void writeExamples(TextureSpec[] textures) throws IOException {
        List<int[]> palette = Arrays.asList(
            new int[] {0x4F8A2F}, // green-ish
            new int[] {0x7A5A38}, // earth
            new int[] {0x8E8E8E}, // grey
            new int[] {0xC2B280}, // sand
            new int[] {0xE6E6F0}, // snow/ice
            new int[] {0x3B5A2A}  // deep moss
        );
        for (int i = 0; i < textures.length; i++) {
            TextureSpec spec = textures[i];
            int rgb = palette.get(i % palette.size())[0];
            BufferedImage demo = renderDemo(spec, rgb);
            writePng(demo, EXAMPLES_DIR + spec.name + "_demo.png");
        }
    }

    private static final int DEMO_PANELS = 3;
    private static final int DEMO_GUTTER = 4;

    private static BufferedImage renderDemo(TextureSpec spec, int rgb) {
        int stride = TILE + DEMO_GUTTER;
        int w = stride * DEMO_PANELS - DEMO_GUTTER;
        int h = TILE;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] px = new int[w * h];

        int[] base = new int[TILE * TILE];
        Arrays.fill(base, 0xFF000000 | rgb);

        BufferedImage tex   = renderTexture(spec);
        BufferedImage edge  = intersectWithShape(tex, edgeShape());
        BufferedImage corner= intersectWithShape(tex, cornerShape());

        int[] tA = tex.getRGB(0, 0, TILE, TILE, null, 0, TILE);
        int[] eA = edge.getRGB(0, 0, TILE, TILE, null, 0, TILE);
        int[] cA = corner.getRGB(0, 0, TILE, TILE, null, 0, TILE);

        blit(px, w, 0, base);

        int[] panel2 = base.clone();
        for (int i = 0; i < panel2.length; i++) {
            int a = (tA[i] >>> 24) & 0xFF;
            panel2[i] = blendLighten(panel2[i], a);
        }
        blit(px, w, stride, panel2);

        int[] panel3 = base.clone();
        for (int i = 0; i < panel3.length; i++) {
            int a = Math.max((eA[i] >>> 24) & 0xFF, (cA[i] >>> 24) & 0xFF);
            panel3[i] = blendLighten(panel3[i], a);
        }
        blit(px, w, stride * 2, panel3);

        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    private static void blit(int[] dst, int dstW, int xOff, int[] src) {
        for (int y = 0; y < TILE; y++) {
            System.arraycopy(src, y * TILE, dst, y * dstW + xOff, TILE);
        }
    }

    /** Lighten base color toward white by {@code alpha/255}, preserving full opacity. */
    private static int blendLighten(int base, int alpha) {
        int r = (base >> 16) & 0xFF;
        int g = (base >>  8) & 0xFF;
        int b =  base        & 0xFF;
        double t = alpha / 255.0;
        r = (int) (r + (255 - r) * t * 0.45);
        g = (int) (g + (255 - g) * t * 0.45);
        b = (int) (b + (255 - b) * t * 0.45);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // -------------------------------------------------- composed-tile output

    /**
     * Recipe for a finished tile.
     *
     *   baseRgb           – dominant body color
     *   darkRgb, lightRgb – per-pixel jitter targets; each pixel picks somewhere
     *                       on the line between dark↔base↔light driven by smooth
     *                       low-frequency noise (gives uniform texture, no clumps)
     *   featureStyle      – overlay style applied on top of jitter (or NONE)
     *   featureRgb        – color of feature overlay pixels
     *   featureFreq       – noise frequency for feature placement
     *   featureThreshold  – cutoff in [0,1]; higher = sparser features
     *   borderFamily      – which existing border shape to borrow ("grass" / "mud"
     *                       / "plains" / "desert"); loaded from existing PNGs as a
     *                       binary alpha mask (hard edges, not gradient)
     *   seed              – per-tile noise seed (kept distinct across the set)
     */
    private record TileSpec(String name, int baseRgb, int darkRgb, int lightRgb,
                            FeatureStyle featureStyle, int featureRgb,
                            double featureFreq, double featureThreshold,
                            String borderFamily, long seed) {}

    private enum FeatureStyle { NONE, BLADES, SPOTS, PEBBLES, DUST, CRACKS }

    private static TileSpec[] tileSpecs() {
        return new TileSpec[] {
            // grasses — green family, blades or speckles for variety
            new TileSpec("lushGrass",     0x4FA02C, 0x3C7820, 0x6BC83A, FeatureStyle.BLADES,  0x2D5618, 1.6, 0.55, "grass",  3001L),
            new TileSpec("meadowGrass",   0x65A53D, 0x4C7E2D, 0x8AC25A, FeatureStyle.BLADES,  0x37671E, 1.4, 0.60, "grass",  3002L),
            new TileSpec("tallGrassTile", 0x3D8222, 0x2D6018, 0x5AAE30, FeatureStyle.BLADES,  0x214712, 1.8, 0.50, "grass",  3003L),
            new TileSpec("dryGrassTile",  0xA5A24E, 0x80803A, 0xC4C075, FeatureStyle.BLADES,  0x6A6826, 1.4, 0.55, "grass",  3004L),
            new TileSpec("burnedGrass",   0x4C402C, 0x322A1C, 0x6A5A3B, FeatureStyle.SPOTS,   0x251D11, 1.6, 0.70, "grass",  3005L),
            new TileSpec("mossyGrass",    0x3A6B2A, 0x265018, 0x568B3C, FeatureStyle.SPOTS,   0x18380F, 1.5, 0.60, "grass",  3006L),

            // muds — brown family, fine spots
            new TileSpec("wetMud",        0x5A3F26, 0x402B1A, 0x755337, FeatureStyle.SPOTS,   0x2C1C0F, 1.8, 0.65, "mud",    3007L),
            new TileSpec("crackedMud",    0x856238, 0x614728, 0xA47C4D, FeatureStyle.CRACKS,  0x3E2B17, 0.9, 0.55, "mud",    3008L),
            new TileSpec("peatMud",       0x3A2B1D, 0x261A10, 0x523D27, FeatureStyle.SPOTS,   0x180F08, 1.7, 0.70, "mud",    3009L),
            new TileSpec("swampMud",      0x46512A, 0x32391C, 0x60703A, FeatureStyle.SPOTS,   0x232812, 1.6, 0.62, "mud",    3010L),
            new TileSpec("riverMud",      0x68522F, 0x4B391E, 0x866940, FeatureStyle.PEBBLES, 0xA08560, 1.2, 0.75, "mud",    3011L),

            // rocky — grey family, lighter pebbles
            new TileSpec("cobbleRock",    0x747474, 0x575757, 0x9B9B9B, FeatureStyle.PEBBLES, 0xB8B8B8, 1.0, 0.65, "plains", 3012L),
            new TileSpec("crackedStone",  0x807F7C, 0x615F5C, 0xA4A2A0, FeatureStyle.CRACKS,  0x444240, 0.9, 0.55, "plains", 3013L),
            new TileSpec("gravelPath",    0x8E867B, 0x6E6759, 0xB0A89C, FeatureStyle.PEBBLES, 0xC5BCA8, 1.4, 0.55, "plains", 3014L),
            new TileSpec("boulderField",  0x676767, 0x4A4A4A, 0x8E8E8E, FeatureStyle.PEBBLES, 0xACACAC, 0.7, 0.60, "plains", 3015L),
            new TileSpec("slateRock",     0x525A60, 0x3A4147, 0x747F88, FeatureStyle.SPOTS,   0x2B3036, 1.4, 0.60, "plains", 3016L),

            // desert — warm pale sand, dust specks
            new TileSpec("paleSand",      0xDDCB91, 0xC4B47B, 0xEFE0AE, FeatureStyle.DUST,    0xB29F6A, 1.8, 0.75, "desert", 3017L),
            new TileSpec("redSand",       0xC07242, 0xA45C32, 0xD89060, FeatureStyle.DUST,    0x8E4926, 1.6, 0.72, "desert", 3018L),
            new TileSpec("dustyDesert",   0xC9AC78, 0xAE9560, 0xDFC692, FeatureStyle.DUST,    0x988350, 1.6, 0.75, "desert", 3019L),
            new TileSpec("desertGravel",  0xBA9F6E, 0x9C8552, 0xD3BB8B, FeatureStyle.PEBBLES, 0xE2D0A0, 1.2, 0.65, "desert", 3020L),
        };
    }

    /**
     * Compose three PNGs per spec into {@link #TILE_DIR}:
     *   - {@code <name>.png}   – fully-opaque body
     *   - {@code <name>B1.png} – body color, gated by the right-edge shape borrowed from the border family
     *   - {@code <name>C0.png} – body color, gated by the top-right-corner shape from the border family
     *
     * Border shapes are loaded from the existing artist-painted PNGs and
     * thresholded to a binary alpha (>0 → opaque, ==0 → transparent). This
     * preserves the irregular hand-drawn boundary and keeps borders looking
     * like the existing tiles instead of smooth gradients.
     */
    private static void composeTile(TileSpec spec) throws IOException {
        boolean[] edgeShape   = loadBinaryShape(spec.borderFamily + "B1.png");
        boolean[] cornerShape = loadBinaryShape(spec.borderFamily + "C0.png");

        // Body uses the full feature pass; borders use feature-free fill so the
        // silhouette reads as clean material rather than a speckled patch (the
        // small opaque area of a B1/C0 amplifies feature density, and dark
        // feature colors otherwise dominate the silhouette).
        int[] body       = renderBody(spec, true);
        int[] borderBody = renderBody(spec, false);
        writePng(toImage(body,       null),        TILE_DIR + spec.name + ".png");
        writePng(toImage(borderBody, edgeShape),   TILE_DIR + spec.name + "B1.png");
        writePng(toImage(borderBody, cornerShape), TILE_DIR + spec.name + "C0.png");
    }

    /**
     * Render the body color array. Per-pixel: start at baseRgb, shift along
     * the dark↔light axis by smooth noise, then if {@code withFeatures} is
     * true and a feature mask hits that pixel, override with the feature color.
     */
    /** Multiplier on the dark↔light jitter excursion. 1.0 = full spec range, 0.25 = quartered. */
    private static final double BODY_JITTER_SCALE = 0.25;
    /** Bump added to every spec's feature threshold. Higher cutoff = sparser features. */
    private static final double FEATURE_THRESHOLD_BUMP = 0.20;

    private static int[] renderBody(TileSpec spec, boolean withFeatures) {
        int[] out = new int[TILE * TILE];
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                double jitter = OpenSimplex2S.noise2(spec.seed, x * 0.9, y * 0.9); // [-1,1]
                double t = Math.abs(jitter) * BODY_JITTER_SCALE;
                int rgb = jitter < 0
                        ? lerpRgb(spec.baseRgb, spec.darkRgb,  t)
                        : lerpRgb(spec.baseRgb, spec.lightRgb, t);
                if (withFeatures && spec.featureStyle != FeatureStyle.NONE
                        && featureHits(spec.featureStyle, spec.seed, x, y,
                                       spec.featureFreq, spec.featureThreshold + FEATURE_THRESHOLD_BUMP)) {
                    rgb = spec.featureRgb & 0x00FFFFFF;
                }
                out[y * TILE + x] = 0xFF000000 | (rgb & 0x00FFFFFF);
            }
        }
        return out;
    }

    /**
     * Does the feature noise field for this pixel rise above the cutoff?
     * Different styles sample noise differently (blades stretched vertically,
     * cracks use ridged noise, etc.).
     */
    private static boolean featureHits(FeatureStyle style, long seed, int x, int y,
                                       double freq, double threshold) {
        double v;
        switch (style) {
            case BLADES:
                // stretched vertically — readable as short vertical strokes
                v = fbm(seed + 31L, x * freq * 2.4, y * freq * 0.7, 2);
                break;
            case CRACKS:
                v = ridged(seed + 41L, x * freq, y * freq, 2);
                break;
            case PEBBLES: {
                double a = OpenSimplex2S.noise2(seed + 51L, x * freq,       y * freq      ) * 0.5 + 0.5;
                double b = OpenSimplex2S.noise2(seed + 53L, x * freq * 2.1, y * freq * 2.1) * 0.5 + 0.5;
                v = Math.max(a, b);
                break;
            }
            case DUST:
                v = OpenSimplex2S.noise2(seed + 61L, x * freq * 2.5, y * freq * 2.5) * 0.5 + 0.5;
                break;
            case SPOTS:
            default:
                v = OpenSimplex2S.noise2(seed + 71L, x * freq * 1.8, y * freq * 1.8) * 0.5 + 0.5;
                break;
        }
        return v > threshold;
    }

    /**
     * Read an existing border PNG and reduce its alpha to a binary mask.
     * Cached per filename so we don't re-read disk for every tile.
     */
    private static final java.util.Map<String, boolean[]> SHAPE_CACHE = new java.util.HashMap<>();

    private static boolean[] loadBinaryShape(String fileName) throws IOException {
        boolean[] cached = SHAPE_CACHE.get(fileName);
        if (cached != null) return cached;
        File f = new File(TILE_DIR + fileName);
        if (!f.exists()) throw new IOException("border shape missing: " + fileName);
        BufferedImage img = ImageIO.read(f);
        BufferedImage scaled = img.getWidth() == TILE && img.getHeight() == TILE
                ? img : nearestScale(img, TILE, TILE);
        int[] px = scaled.getRGB(0, 0, TILE, TILE, null, 0, TILE);
        boolean[] out = new boolean[TILE * TILE];
        for (int i = 0; i < out.length; i++) {
            int a = (px[i] >>> 24) & 0xFF;
            out[i] = a >= 128; // hard threshold — partial-alpha outline rounds outward
        }
        SHAPE_CACHE.put(fileName, out);
        return out;
    }

    private static BufferedImage nearestScale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        double sx = (double) src.getWidth()  / w;
        double sy = (double) src.getHeight() / h;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, src.getRGB((int) (x * sx), (int) (y * sy)));
            }
        }
        return out;
    }

    /**
     * Pack a body color array into a {@link BufferedImage}. If {@code gate} is
     * non-null, pixels where the gate is false go fully transparent.
     */
    private static BufferedImage toImage(int[] body, boolean[] gate) {
        BufferedImage img = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
        int[] out = new int[TILE * TILE];
        for (int i = 0; i < out.length; i++) {
            if (gate != null && !gate[i]) out[i] = 0;
            else out[i] = body[i];
        }
        img.setRGB(0, 0, TILE, TILE, out, 0, TILE);
        return img;
    }

    /** Linear interpolation between two packed 0xRRGGBB colors. */
    private static int lerpRgb(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl= (int) Math.round(ab + (bb - ab) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ----------------------------------------------------------- file utils

    private static int alphaToWhite(double a) {
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, a)) * 255);
        return (alpha << 24) | 0x00FFFFFF;
    }

    private static void writePng(BufferedImage img, String path) throws IOException {
        File f = new File(path);
        ImageIO.write(img, "png", f);
    }

    private static void mkdirs() {
        new File(TEXTURE_DIR).mkdirs();
        new File(BORDER_DIR).mkdirs();
        new File(MICRO_DIR).mkdirs();
        new File(EXAMPLES_DIR).mkdirs();
    }
}
