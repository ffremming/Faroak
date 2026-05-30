package resources.testing.probes;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import resources.app.GamePanel;
import resources.domain.tile.Tile;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Renders the real layered image stack for each water depth tier to a PNG so the
 * seabed-under-water compositing can be eyeballed. Not a pass/fail invariant —
 * it always passes if it can write the file; the artifact is the point.
 *
 * Output: perf-results/water/water_render.png — four 64px cells:
 * deep | medium | shallow | (shallow with no seabed, for contrast).
 */
public final class WaterRenderProbe implements Probe {

    private static final Logger LOG = Logger.forClass(WaterRenderProbe.class);
    private static final int PX = 64;

    @Override public String name() { return "water-render"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GamePanel panel = harness.panel();
        String[] tiers = {"ocean", "mediumWater", "shallowWater"};
        BufferedImage out = new BufferedImage(PX * tiers.length, PX, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        for (int i = 0; i < tiers.length; i++) {
            Tile t = new Tile(panel, tiers[i], 64 * i, 0, 0);
            ArrayList<BufferedImage> stack = t.getImages();
            for (BufferedImage layer : stack) {
                g.drawImage(layer, i * PX, 0, PX, PX, null);
            }
        }
        g.dispose();

        try {
            File dir = new File("perf-results/water");
            dir.mkdirs();
            File f = new File(dir, "water_render.png");
            ImageIO.write(out, "png", f);
            LOG.info("wrote " + f.getAbsolutePath());
            return ProbeResult.pass(name(), "wrote water_render.png");
        } catch (IOException e) {
            return ProbeResult.fail(name() + " could not write artifact: " + e.getMessage());
        }
    }
}
