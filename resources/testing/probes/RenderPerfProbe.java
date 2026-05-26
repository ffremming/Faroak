package resources.testing.probes;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import resources.app.GameContext;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Drives Camera.draw over a BufferedImage Graphics2D to measure paint cost
 * independent of simulate. Reports avg + worst sample.
 *
 * Headless caveat: software-pipeline BufferedImage rasterization is much
 * faster than on-screen rendering, so absolute numbers are not directly
 * comparable to the live game's draw cost. Still useful as a regression
 * tripwire — if avg balloons or draw starts throwing, this catches it.
 */
public final class RenderPerfProbe implements Probe {

    private static final Logger LOG = Logger.forClass(RenderPerfProbe.class);

    private static final int SAMPLES = 120;
    private static final int WARMUP  = 10;

    @Override public String name() { return "render-perf"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        int w = ctx.screenWidth();
        int h = ctx.screenHeight();

        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = canvas.createGraphics();
        try {
            for (int i = 0; i < WARMUP; i++) ctx.camera().draw(g2);

            long totalNs = 0;
            long worstNs = 0;
            for (int i = 0; i < SAMPLES; i++) {
                long t0 = System.nanoTime();
                ctx.camera().draw(g2);
                long dt = System.nanoTime() - t0;
                totalNs += dt;
                if (dt > worstNs) worstNs = dt;
            }
            long avgUs   = (totalNs / SAMPLES) / 1_000L;
            long worstUs = worstNs / 1_000L;
            String detail = String.format("avg=%d us, worst=%d us (samples=%d, %dx%d)",
                avgUs, worstUs, SAMPLES, w, h);
            LOG.info(detail);
            return ProbeResult.pass(name(), detail);
        } catch (RuntimeException ex) {
            return ProbeResult.fail(name() + " draw threw", ex.toString());
        } finally {
            g2.dispose();
        }
    }
}
