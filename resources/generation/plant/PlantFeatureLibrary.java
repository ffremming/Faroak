package resources.generation.plant;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * The pool of pixel-art "features" that {@link PlantImageGenerator} stamps to
 * build a plant. Each feature is a rectangular cut-out of an existing
 * hand-drawn sprite (currently shrub_M.png and spruce_M.png) — a leaf clump,
 * a trunk segment, a root flare, etc.
 *
 * Crop rectangles are committed here as constants. On {@link #bootstrap()} the
 * library:
 *   1. Loads the two source sprites once.
 *   2. For every {@link Feature}, either reads the cached PNG under
 *      {@code resources/images/features/plant/<name>.png} (if present) or
 *      crops from the source and writes the PNG so future runs can reuse it
 *      and so a human can inspect / hand-edit any feature without rebuilding.
 *
 * Why a fixed feature set rather than random patches: the source sprites have
 * a small number of *meaningful* shapes — the spruce tip is sharp, the bottom
 * tier is wide, the trunk has its own texture. Random crops would mix those
 * up and produce mush. A curated set keeps every plant readable as a plant.
 */
public final class PlantFeatureLibrary {

    /** A named crop rectangle into one of the source sprites. */
    public enum Feature {
        // shrub_M.png crops — small leaf clumps with the characteristic
        // bright-green highlight and the dark berry-red dots.
        SHRUB_TIP      (Source.SHRUB,  18,   2,  46,  22),
        SHRUB_CLUMP_L  (Source.SHRUB,   4,  20,  30,  44),
        SHRUB_CLUMP_R  (Source.SHRUB,  32,  18,  60,  44),
        SHRUB_MID_BAND (Source.SHRUB,   6,  44,  58,  68),
        SHRUB_LOWER    (Source.SHRUB,   8,  62,  56,  86),
        SHRUB_STEM     (Source.SHRUB,  24,  86,  40, 100),

        // spruce_M.png crops — tier slices of the conifer canopy, plus the
        // textured trunk and the splayed root flare.
        SPRUCE_TIP     (Source.SPRUCE, 32,   2,  64,  32),
        SPRUCE_TIER_TOP(Source.SPRUCE, 16,  28,  80,  70),
        SPRUCE_TIER_MID(Source.SPRUCE,  8,  60,  88, 110),
        SPRUCE_TIER_BOT(Source.SPRUCE,  0,  95,  96, 125),
        SPRUCE_TRUNK   (Source.SPRUCE, 38, 130,  60, 178),
        SPRUCE_ROOTS   (Source.SPRUCE, 28, 168,  68, 195);

        final Source source;
        final int    x, y, w, h;

        Feature(Source source, int left, int top, int right, int bottom) {
            this.source = source;
            this.x = left;
            this.y = top;
            this.w = right  - left;
            this.h = bottom - top;
        }

        public String fileName() { return name().toLowerCase() + ".png"; }
    }

    private enum Source {
        SHRUB ("resources/images/objects/nature/plants/bushes/shrub_M/shrub_M.png"),
        SPRUCE("resources/images/objects/nature/plants/trees/spruce_M/spruce_M.png");
        final String path;
        Source(String path) { this.path = path; }
    }

    private static final String FEATURE_DIR = "resources/images/features/plant/";

    private static final Map<Feature, BufferedImage> CACHE = new LinkedHashMap<>();
    private static boolean bootstrapped = false;

    private PlantFeatureLibrary() {}

    public static synchronized void bootstrap() {
        // Disabled at runtime: this used to crop source sprites and write feature
        // PNGs to disk. The game now uses the hand-authored plants_green.png
        // spritesheet, so this must never run during play (no image generation).
        throw new UnsupportedOperationException(
            "PlantFeatureLibrary is disabled — procedural plant images are not "
          + "generated at runtime.");
    }

    @SuppressWarnings("unused")
    private static synchronized void bootstrapInternal() {
        if (bootstrapped) return;
        new File(FEATURE_DIR).mkdirs();

        // Source images are loaded lazily — only if we actually need to crop.
        Map<Source, BufferedImage> sources = new java.util.EnumMap<>(Source.class);

        for (Feature f : Feature.values()) {
            File cached = new File(FEATURE_DIR + f.fileName());
            BufferedImage img = readIfExists(cached);
            if (img == null) {
                BufferedImage src = sources.computeIfAbsent(f.source, s -> readOrNull(s.path));
                if (src == null) {
                    System.err.println("PlantFeatureLibrary: missing source " + f.source.path);
                    continue;
                }
                img = src.getSubimage(f.x, f.y, f.w, f.h);
                // getSubimage returns a view; copy so writing the PNG and any
                // future stamping operate on independent pixel data.
                BufferedImage copy = new BufferedImage(f.w, f.h, BufferedImage.TYPE_INT_ARGB);
                copy.getGraphics().drawImage(img, 0, 0, null);
                img = stripGroundShadow(copy);
                writeQuietly(img, cached);
            }
            CACHE.put(f, img);
        }
        bootstrapped = true;
    }

    public static BufferedImage get(Feature f) {
        if (!bootstrapped) bootstrapInternal();
        return CACHE.get(f);
    }

    public static Map<Feature, BufferedImage> all() {
        if (!bootstrapped) bootstrapInternal();
        return Collections.unmodifiableMap(CACHE);
    }

    /**
     * Replace the near-white "ground shadow" pixels in the source sprites
     * with transparency. The hand-painted spruce/shrub each sit on a small
     * light-grey shadow patch that bleeds into the lower crops; left in, it
     * shows up as a halo when the feature is stamped onto grass.
     */
    private static BufferedImage stripGroundShadow(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>>  8) & 0xFF;
                int b =  argb         & 0xFF;
                // Light, near-grey: high & similar RGB. Foliage greens and
                // brown trunks both fail this test.
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                if (max >= 170 && (max - min) <= 25) {
                    img.setRGB(x, y, 0);
                }
            }
        }
        return img;
    }

    private static BufferedImage readIfExists(File file) {
        if (!file.isFile()) return null;
        return readOrNull(file.getPath());
    }

    private static BufferedImage readOrNull(String path) {
        try { return ImageIO.read(new File(path)); }
        catch (IOException e) { return null; }
    }

    private static void writeQuietly(BufferedImage img, File out) {
        try { ImageIO.write(img, "PNG", out); }
        catch (IOException e) { System.err.println("PlantFeatureLibrary: cannot write " + out); }
    }
}
