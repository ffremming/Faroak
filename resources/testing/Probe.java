package resources.testing;

/**
 * One headless inspection unit. Probes are like targeted integration tests:
 * each names a property of the running game and reports whether it holds.
 *
 * Strategy pattern — the {@link TestRunner} doesn't know what any probe does,
 * just iterates and aggregates results. New probes plug in without touching
 * the runner.
 *
 * Probes receive the {@link TestHarness} so they can both inspect state (via
 * {@code harness.context()}) and drive ticks (via {@code harness.tick(n)} or
 * {@code harness.tickAndMeasure}). Driving via the harness exercises the same
 * per-frame work the live game does — environment manager + simulate + clock.
 */
public interface Probe {

    /** Stable short name printed in run summaries. */
    String name();

    /** Inspect / drive {@code harness} and return pass/fail with detail. Must not block. */
    ProbeResult run(TestHarness harness);
}
