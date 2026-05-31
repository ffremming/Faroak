package resources.domain.tile;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Flattens a water tile's base frame plus its depth-transition crescents into a
 * SINGLE translucent layer, so the seabed underneath shows through uniformly and
 * the crescent region does not stack alpha against the base.
 *
 * Why: water tiles are drawn semi-transparent (the seabed sprite shows through).
 * Drawing a translucent crescent ON TOP of the translucent base with normal
 * source-over compositing makes that region MORE opaque than the rest of the
 * water — a visible darker band / hard step at the tile boundary. Flattening with
 * "replace" semantics fixes that: the combined layer keeps the base's alpha
 * everywhere and only shifts COLOUR toward the crescent's tier where a crescent
 * covers it. Result: a soft wavy colour change at the depth boundary, no band and
 * no outline, with the seabed still visible through the whole thing.
 *
 * The output alpha at each pixel is the MAX of the base and crescent alphas (they
 * are authored to the same per-tier value, so this is just the tier alpha) — never
 * the sum — which is what removes the stacking.
 */
final class WaterLayerFlattener {

    private WaterLayerFlattener() {}

    /**
     * Combine {@code base} with the {@code crescents} (drawn in order, later ones
     * winning on colour) into one image. Each crescent pixel's alpha is treated as
     * a COVERAGE weight: it lerps the output colour toward the crescent colour but
     * does not add to the output alpha. Returns a new image; inputs are untouched.
     */
    static BufferedImage flatten(BufferedImage base, List<BufferedImage> crescents) {
        int w = base.getWidth(), h = base.getHeight();
        int[] out = base.getRGB(0, 0, w, h, null, 0, w);

        for (BufferedImage cres : crescents) {
            if (cres == null) continue;
            // Crescents may be authored at a different resolution than the base;
            // sample nearest so this works regardless.
            int cw = cres.getWidth(), ch = cres.getHeight();
            int[] cpx = cres.getRGB(0, 0, cw, ch, null, 0, cw);
            for (int y = 0; y < h; y++) {
                int sy = (cw == w && ch == h) ? y : (y * ch) / h;
                for (int x = 0; x < w; x++) {
                    int sx = (cw == w && ch == h) ? x : (x * cw) / w;
                    int cc = cpx[sy * cw + sx];
                    int ca = (cc >>> 24) & 0xFF;
                    if (ca == 0) continue;
                    int oi = y * w + x;
                    int oc = out[oi];
                    int oa = (oc >>> 24) & 0xFF;
                    // coverage = how strongly the crescent colour replaces the base
                    // colour here (0..1). Use the crescent's own alpha as coverage,
                    // normalised by the base alpha so a same-alpha crescent fully
                    // recolours its region.
                    double cov = oa == 0 ? 1.0 : Math.min(1.0, ca / (double) oa);
                    int nr = lerp((oc >> 16) & 0xFF, (cc >> 16) & 0xFF, cov);
                    int ng = lerp((oc >>  8) & 0xFF, (cc >>  8) & 0xFF, cov);
                    int nb = lerp( oc        & 0xFF,  cc        & 0xFF, cov);
                    // keep the larger alpha (never the sum) → no stacking
                    int na = Math.max(oa, ca);
                    out[oi] = (na << 24) | (nr << 16) | (ng << 8) | nb;
                }
            }
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, out, 0, w);
        return img;
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }
}
