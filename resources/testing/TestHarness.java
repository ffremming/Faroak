package resources.testing;

import javax.swing.JFrame;

import resources.app.GameContext;
import resources.app.GamePanel;

/**
 * Boots a {@link GamePanel} without a visible window and exposes it for
 * programmatic ticking and inspection. Lets probes drive the simulation
 * forward and read state from the same {@link GameContext} the live game uses.
 *
 * Construction triggers the same world-generation + image-loading path as the
 * live game; expect the constructor to be the heaviest call site in any test.
 */
public final class TestHarness implements AutoCloseable {

    private static final Logger LOG = Logger.forClass(TestHarness.class);

    private final JFrame    frame;
    private final GamePanel panel;

    public TestHarness() {
        LOG.info("booting headless game…");
        long start = System.nanoTime();
        this.frame = new JFrame("test-harness");
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.panel = new GamePanel(frame, true);
        this.frame.add(panel);
        this.frame.pack();
        // Intentionally NOT setVisible(true): tests run headless.
        long ms = (System.nanoTime() - start) / 1_000_000L;
        LOG.info("boot complete in %d ms", ms);
    }

    public GameContext context() { return panel; }
    public GamePanel   panel()   { return panel; }

    /** Advance the simulation by {@code times} update steps (no rendering). */
    public void tick(int times) {
        for (int i = 0; i < times; i++) {
            panel.update(1.0);
        }
    }

    /** Advance simulation while measuring per-tick cost. Returns avg microseconds. */
    public long tickAndMeasure(int times) {
        long total = 0;
        for (int i = 0; i < times; i++) {
            long t0 = System.nanoTime();
            panel.update(1.0);
            total += System.nanoTime() - t0;
        }
        return total / Math.max(1, times) / 1_000L;
    }

    @Override
    public void close() {
        panel.stopGameThread();
        frame.dispose();
        LOG.info("harness closed");
    }
}
