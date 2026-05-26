package resources.testing.probes;

import java.awt.image.BufferedImage;

import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Inspects per-edge alpha density of key overlay sprites and reports which
 * side carries the visible content. Useful for verifying my assumption that
 * {@code wetBeachB1} is west-aligned vs east-aligned.
 *
 * Not a pass/fail — informational. Always returns PASS with the orientation
 * map in details.
 */
public final class SpriteOrientationProbe implements Probe {

    private static final Logger LOG = Logger.forClass(SpriteOrientationProbe.class);

    @Override public String name() { return "sprite-orientation"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        String[] keys = { "beachB1", "wetBeachB1", "wetBeach1B1", "wetBeachB3", "wetBeach1B3" };
        StringBuilder details = new StringBuilder();
        for (String key : keys) {
            BufferedImage img = harness.context().images().getTileImage(key);
            details.append(String.format("%-15s %s%n", key, describe(img)));
        }
        LOG.info("orientation summary:\n%s", details);
        return ProbeResult.pass(name(), details.toString().trim());
    }

    /** Sum alpha along each edge band; report dominant side. */
    private String describe(BufferedImage img) {
        if (img == null) return "null";
        int w = img.getWidth(), h = img.getHeight();
        int band = Math.max(1, w / 6);
        long north = 0, south = 0, east = 0, west = 0;
        for (int y = 0; y < band; y++)
            for (int x = 0; x < w; x++) north += alpha(img, x, y);
        for (int y = h - band; y < h; y++)
            for (int x = 0; x < w; x++) south += alpha(img, x, y);
        for (int x = w - band; x < w; x++)
            for (int y = 0; y < h; y++) east += alpha(img, x, y);
        for (int x = 0; x < band; x++)
            for (int y = 0; y < h; y++) west += alpha(img, x, y);
        return String.format("size=%dx%d  N=%d  E=%d  S=%d  W=%d", w, h, north, east, south, west);
    }

    private int alpha(BufferedImage img, int x, int y) {
        return (img.getRGB(x, y) >> 24) & 0xFF;
    }
}
