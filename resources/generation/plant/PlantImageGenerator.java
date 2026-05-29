package resources.generation.plant;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;

import resources.generation.plant.PlantFeatureLibrary.Feature;

/**
 * Renders a {@link PlantSpec} by stamping pre-cut pixel-art
 * {@link PlantFeatureLibrary.Feature features} onto a transparent canvas.
 *
 * Every paint call uses nearest-neighbor interpolation so the source pixels
 * stay sharp — no antialiasing, no bilinear blur. Foliage features can be
 * tinted via {@link PlantSpec.Tint}; trunk features are never tinted.
 *
 * Layout convention: the canvas matches the spec's tile-sized footprint, and
 * features are anchored so the base of the trunk (or the lowest foliage clump
 * for shrubs) sits on the bottom row of pixels.
 */
public final class PlantImageGenerator {

    /** One world tile in pixels — matches GamePanel.tileSize. */
    public static final int TILE_PX = 64;

    private PlantImageGenerator() {}

    public static BufferedImage generate(PlantSpec spec) {
        // The procedural plant pipeline is retired: plants are loaded from the
        // hand-authored spritesheet (plants_green.png). This path must never run
        // during the game — it would render/write images at runtime. Guard it so
        // an accidental re-wire fails loudly instead of silently producing PNGs.
        throw new UnsupportedOperationException(
            "Procedural plant generation is disabled. Use the plants_green.png "
          + "spritesheet via PlantCatalog/ObjectCatalog instead.");
    }

    /** Retained-for-reference renderer; intentionally unreachable (see generate). */
    @SuppressWarnings("unused")
    private static BufferedImage generateInternal(PlantSpec spec) {
        PlantFeatureLibrary.get(PlantFeatureLibrary.Feature.values()[0]); // warm cache (reference only)

        int w = (int) Math.round(spec.size.widthTiles  * TILE_PX);
        int h = (int) Math.round(spec.size.heightTiles * TILE_PX);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Hard pixels: no antialias, no bilinear scaling.
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setComposite(AlphaComposite.SrcOver);

        Random rng = new Random(spec.seed);

        int trunkTopY = drawTrunk(g, spec, w, h);
        drawFoliage(g, spec, w, h, trunkTopY, rng);

        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------ trunk

    /** Returns the y coordinate at which foliage should start (= top of trunk). */
    private static int drawTrunk(Graphics2D g, PlantSpec spec, int w, int h) {
        if (spec.trunk == PlantSpec.Trunk.NONE) {
            // Foliage sits on the ground.
            return (int) (h * 0.92);
        }

        int trunkBottom = h - 2;
        int trunkH, trunkW;
        switch (spec.trunk) {
            case THICK: trunkW = Math.max(14, w / 5);  trunkH = (int) (h * 0.42); break;
            case THIN:  trunkW = Math.max(6,  w / 12); trunkH = (int) (h * 0.55); break;
            case MULTI: trunkW = Math.max(6,  w / 14); trunkH = (int) (h * 0.38); break;
            default:    trunkW = Math.max(8,  w / 10); trunkH = (int) (h * 0.45);
        }
        int trunkTop = trunkBottom - trunkH;
        int centerX  = w / 2;

        BufferedImage trunkImg = spec.trunk == PlantSpec.Trunk.THICK
                ? PlantFeatureLibrary.get(Feature.SPRUCE_TRUNK)
                : PlantFeatureLibrary.get(Feature.SHRUB_STEM);

        if (spec.trunk == PlantSpec.Trunk.MULTI) {
            int stems = 2;
            for (int i = 0; i < stems; i++) {
                int dx = (i - (stems - 1) / 2) * (trunkW + 4) - trunkW / 4;
                stamp(g, trunkImg, centerX + dx - trunkW / 2, trunkTop, trunkW, trunkH);
            }
        } else {
            stamp(g, trunkImg, centerX - trunkW / 2, trunkTop, trunkW, trunkH);
        }

        // Spruce-style root flare for thick trunks so the base reads as a tree.
        if (spec.trunk == PlantSpec.Trunk.THICK) {
            BufferedImage roots = PlantFeatureLibrary.get(Feature.SPRUCE_ROOTS);
            int rw = (int) (trunkW * 2.2);
            int rh = (int) (trunkH * 0.30);
            stamp(g, roots, centerX - rw / 2, trunkBottom - rh + 2, rw, rh);
        }

        return trunkTop;
    }

    // ---------------------------------------------------------------- foliage

    private static void drawFoliage(Graphics2D g, PlantSpec spec, int w, int h,
                                    int trunkTopY, Random rng) {
        switch (spec.shape) {
            case CONIFER: drawConifer(g, spec, w, h, trunkTopY, rng); break;
            case ROUND:   drawRound  (g, spec, w, h, trunkTopY, rng); break;
            case CLUSTER: drawCluster(g, spec, w, h, trunkTopY, rng); break;
            case FROND:   drawFrond  (g, spec, w, h, trunkTopY, rng); break;
        }
    }

    /**
     * Conifer: stamp the spruce tiers in order — widest at the bottom,
     * narrowest at the top, capped with the sharp tip. Each tier is the
     * actual pre-drawn pixel art, so we get real needle detail.
     */
    private static void drawConifer(Graphics2D g, PlantSpec spec, int w, int h,
                                    int trunkTopY, Random rng) {
        BufferedImage tip = tinted(Feature.SPRUCE_TIP,      spec.tint);
        BufferedImage top = tinted(Feature.SPRUCE_TIER_TOP, spec.tint);
        BufferedImage mid = tinted(Feature.SPRUCE_TIER_MID, spec.tint);
        BufferedImage bot = tinted(Feature.SPRUCE_TIER_BOT, spec.tint);

        int canopyTop    = (int) (h * 0.02);
        int canopyBottom = trunkTopY + 4;     // slight overlap onto trunk
        int centerX = w / 2;

        // Layout three big stacked tiers + a tip; widths scaled to spec width.
        // Each tier height is proportional to its source so the texture stays
        // recognisable.
        int botW = (int) (w * 1.00);
        int midW = (int) (w * 0.85);
        int topW = (int) (w * 0.65);
        int tipW = (int) (w * 0.40);

        int botH = (int) ((canopyBottom - canopyTop) * 0.30);
        int midH = (int) ((canopyBottom - canopyTop) * 0.30);
        int topH = (int) ((canopyBottom - canopyTop) * 0.25);
        int tipH = (canopyBottom - canopyTop) - botH - midH - topH;

        int yCursor = canopyBottom - botH;
        stamp(g, bot, centerX - botW / 2, yCursor, botW, botH);
        yCursor -= midH - 2;
        stamp(g, mid, centerX - midW / 2, yCursor, midW, midH);
        yCursor -= topH - 2;
        stamp(g, top, centerX - topW / 2, yCursor, topW, topH);
        yCursor -= tipH - 2;
        stamp(g, tip, centerX - tipW / 2, yCursor, tipW, tipH);
    }

    /**
     * Round canopy: pick the shrub clump features and tile them in an
     * elliptical region above the trunk. Each clump is rotated/flipped
     * randomly to break up repetition.
     */
    private static void drawRound(Graphics2D g, PlantSpec spec, int w, int h,
                                  int trunkTopY, Random rng) {
        BufferedImage[] clumps = {
            tinted(Feature.SHRUB_TIP,     spec.tint),
            tinted(Feature.SHRUB_CLUMP_L, spec.tint),
            tinted(Feature.SHRUB_CLUMP_R, spec.tint),
            tinted(Feature.SHRUB_LOWER,   spec.tint)
        };

        int centerX = w / 2;
        int radiusX = (int) (w * 0.50);
        int radiusY = (int) Math.min(w * 0.45, trunkTopY * 0.55);
        int centerY = trunkTopY - radiusY / 2;

        // Stamp count scales with canopy area so a sapling still looks full.
        int stamps = 16 + (radiusX * radiusY) / 200;
        int clumpW = Math.max(22, w / 3);
        int clumpH = Math.max(20, h / 6);

        for (int i = 0; i < stamps; i++) {
            double dx, dy;
            do {
                dx = (rng.nextDouble() * 2 - 1) * radiusX;
                dy = (rng.nextDouble() * 2 - 1) * radiusY;
            } while ((dx*dx)/(double)(radiusX*radiusX) + (dy*dy)/(double)(radiusY*radiusY) > 1.0);

            BufferedImage clump = clumps[rng.nextInt(clumps.length)];
            int cw = clumpW + rng.nextInt(6) - 3;
            int ch = clumpH + rng.nextInt(6) - 3;
            int px = Math.max(0, Math.min(w - cw, centerX + (int) dx - cw / 2));
            int py = Math.max(0, Math.min(h - ch, centerY + (int) dy - ch / 2));
            stamp(g, clump, px, py, cw, ch);
        }
    }

    /**
     * Bushy cluster of shrub clumps. Clumps are seeded along an ellipse-ish
     * dome centred on the lower half of the canvas, so the silhouette reads
     * as one connected mound rather than two separated bands.
     */
    private static void drawCluster(Graphics2D g, PlantSpec spec, int w, int h,
                                    int trunkTopY, Random rng) {
        BufferedImage[] clumps = {
            tinted(Feature.SHRUB_TIP,      spec.tint),
            tinted(Feature.SHRUB_CLUMP_L,  spec.tint),
            tinted(Feature.SHRUB_CLUMP_R,  spec.tint),
            tinted(Feature.SHRUB_MID_BAND, spec.tint),
            tinted(Feature.SHRUB_LOWER,    spec.tint)
        };

        // Dome centred at the base; half-width matches the canvas, half-height
        // is roughly the trunk-top so the bush sits low and wide.
        int centerX = w / 2;
        int baseY   = trunkTopY + 2;
        int radiusX = (int) (w * 0.50);
        int radiusY = (int) (Math.max(20, (trunkTopY - h * 0.10) * 0.95));

        int stamps = 14 + rng.nextInt(6);
        int clumpW = Math.max(20, w / 3);
        int clumpH = Math.max(18, h / 4);

        for (int i = 0; i < stamps; i++) {
            double dx, dy;
            do {
                dx = (rng.nextDouble() * 2 - 1) * radiusX;
                dy = -rng.nextDouble() * radiusY;     // only upward from baseY
            } while ((dx*dx)/(double)(radiusX*radiusX) + (dy*dy)/(double)(radiusY*radiusY) > 1.0);

            BufferedImage clump = clumps[rng.nextInt(clumps.length)];
            int cw = clumpW + rng.nextInt(6) - 3;
            int ch = clumpH + rng.nextInt(6) - 3;
            int px = Math.max(0, Math.min(w - cw, centerX + (int) dx - cw / 2));
            int py = Math.max(0, Math.min(h - ch, baseY + (int) dy - ch / 2));
            stamp(g, clump, px, py, cw, ch);
        }
    }

    /**
     * Frond / palm-ish shape: shrub clumps fanned out from the top of the
     * trunk along an arc, like a crown of leaves. Outer leaves droop.
     */
    private static void drawFrond(Graphics2D g, PlantSpec spec, int w, int h,
                                  int trunkTopY, Random rng) {
        BufferedImage[] clumps = {
            tinted(Feature.SHRUB_CLUMP_L, spec.tint),
            tinted(Feature.SHRUB_CLUMP_R, spec.tint),
            tinted(Feature.SHRUB_TIP,     spec.tint)
        };

        int centerX = w / 2;
        int crownY  = trunkTopY - 2;
        int fronds  = 7;
        int reach   = (int) (w * 0.55);

        int clumpW = Math.max(18, w / 4);
        int clumpH = Math.max(16, h / 8);

        for (int i = 0; i < fronds; i++) {
            double angle = Math.toRadians(-100 + (200.0 * i) / (fronds - 1));
            double dirX = Math.sin(angle);
            double dirY = -Math.cos(angle);
            // Stamp 4 clumps along the frond, each successive one drooping more.
            int steps = 4;
            for (int s = 1; s <= steps; s++) {
                double t = s / (double) steps;
                double droop = 0.7 * t * t;
                int px = centerX + (int) (dirX * reach * t) - clumpW / 2;
                int py = crownY  + (int) (dirY * reach * t + droop * reach) - clumpH / 2;
                BufferedImage c = clumps[(i + s) % clumps.length];
                int cw = clumpW;
                int ch = clumpH;
                stamp(g, c, px, py, cw, ch);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Draw a feature image scaled to (w, h). Nearest-neighbor is configured
     *  on the Graphics2D already so this stays crisp. */
    private static void stamp(Graphics2D g, BufferedImage feature,
                              int x, int y, int w, int h) {
        g.drawImage(feature, x, y, w, h, null);
    }

    /** Lookup + colour-shift cache for the current tint. The tinted variants
     *  are tiny so we just build them on demand and cache per (feature, tint). */
    private static BufferedImage tinted(Feature f, PlantSpec.Tint tint) {
        if (tint == PlantSpec.Tint.NONE) return PlantFeatureLibrary.get(f);
        return TintCache.get(f, tint);
    }

    /**
     * Per-(feature, tint) cache. Tints are applied in HSB space so a green
     * source pixel can actually rotate to orange — RGB multiply can't do that.
     * Brown trunk pixels (red-dominant + low blue) are passed through
     * unchanged so trunks stay woody under any tint.
     */
    private static final class TintCache {
        private static final java.util.Map<String, BufferedImage> CACHE = new java.util.HashMap<>();

        static BufferedImage get(Feature f, PlantSpec.Tint tint) {
            String key = f.name() + "/" + tint.name();
            BufferedImage cached = CACHE.get(key);
            if (cached != null) return cached;
            BufferedImage src = PlantFeatureLibrary.get(f);
            BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(),
                                                  BufferedImage.TYPE_INT_ARGB);
            float[] hsb = new float[3];
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    if (a == 0) { out.setRGB(x, y, 0); continue; }
                    int r = (argb >>> 16) & 0xFF;
                    int gC = (argb >>>  8) & 0xFF;
                    int b =  argb         & 0xFF;
                    // Skip trunk pixels: red-dominant with little blue.
                    if (r > gC + 20 && r > b + 20 && b < 80) {
                        out.setRGB(x, y, argb);
                        continue;
                    }
                    java.awt.Color.RGBtoHSB(r, gC, b, hsb);
                    float h2 = (float) ((hsb[0] + tint.hueShift / 360.0) % 1.0);
                    if (h2 < 0) h2 += 1f;
                    float s2 = clamp01((float) (hsb[1] * tint.satMul));
                    float v2 = clamp01((float) (hsb[2] * tint.valMul));
                    int rgb = java.awt.Color.HSBtoRGB(h2, s2, v2) & 0x00FFFFFF;
                    out.setRGB(x, y, (a << 24) | rgb);
                }
            }
            CACHE.put(key, out);
            return out;
        }

        private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    }
}
