package resources.testing.probes;

import java.awt.image.BufferedImage;

import resources.app.GameContext;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the layered water assets are wired: the three depth-tier base frames
 * are distinct, shallow/medium have baked transparency (so seabed shows
 * through), and the seabed/foam/detail overlay sprites resolve.
 */
public final class WaterDepthProbe implements Probe {

    private static final Logger LOG = Logger.forClass(WaterDepthProbe.class);

    @Override public String name() { return "water-depth"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        BufferedImage deep    = ctx.images().getTileImage("ocean0");
        BufferedImage medium  = ctx.images().getTileImage("mediumWater0");
        BufferedImage shallow = ctx.images().getTileImage("shallowWater0");
        if (deep == null || medium == null || shallow == null)
            return ProbeResult.fail(name() + " missing a water base frame");

        if (sameImage(deep, medium) || sameImage(medium, shallow))
            return ProbeResult.fail(name() + " depth tiers are not visually distinct");

        if (!hasTransparency(shallow) || !hasTransparency(medium))
            return ProbeResult.fail(name() + " shallow/medium not alpha-baked (seabed can't show through)");

        for (String n : new String[]{"seabed0", "oceanFoamB1", "oceanDetail0"}) {
            if (ctx.images().getTileImage(n) == null)
                return ProbeResult.fail(name() + " missing overlay sprite " + n);
        }
        LOG.info("water depth tiers, alpha, and overlays all present");
        return ProbeResult.pass(name());
    }

    private static boolean hasTransparency(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                if (((img.getRGB(x, y) >>> 24) & 0xff) < 250) return true;
        return false;
    }

    private static boolean sameImage(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++)
                if (a.getRGB(x, y) != b.getRGB(x, y)) return false;
        return true;
    }
}
