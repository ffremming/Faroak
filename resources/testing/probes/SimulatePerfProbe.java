package resources.testing.probes;

import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Times the simulate() phase under steady-state. Useful for spotting the
 * "movement is laggy" class of regression — if the per-tick cost spikes when
 * the player triggers chunk reload / border rebuild, the average climbs.
 *
 * Pass threshold is intentionally generous; the goal is to flag order-of-
 * magnitude regressions, not micro-optimisations.
 */
public final class SimulatePerfProbe implements Probe {

    private static final Logger LOG = Logger.forClass(SimulatePerfProbe.class);

    private static final int  SAMPLES        = 240;
    private static final long PASS_AVG_US    = 5_000;   // 5 ms average per simulate()
    private static final long PASS_P99_US    = 25_000;  // 25 ms for the worst sample

    @Override public String name() { return "simulate-perf"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        long[] samples = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long t0 = System.nanoTime();
            harness.tick(1);
            samples[i] = (System.nanoTime() - t0) / 1_000L;
        }

        long sum = 0, max = 0;
        for (long s : samples) { sum += s; if (s > max) max = s; }
        long avg = sum / SAMPLES;
        long p99 = percentile(samples, 99);

        String detail = String.format("avg=%d us, p99=%d us, max=%d us (samples=%d)",
            avg, p99, max, SAMPLES);
        LOG.info(detail);

        boolean ok = avg <= PASS_AVG_US && p99 <= PASS_P99_US;
        return ok ? ProbeResult.pass(name(), detail)
                  : ProbeResult.fail(name() + " above thresholds", detail);
    }

    private static long percentile(long[] samples, int p) {
        long[] sorted = samples.clone();
        java.util.Arrays.sort(sorted);
        int idx = Math.min(sorted.length - 1, (int) Math.ceil(p / 100.0 * sorted.length) - 1);
        return sorted[Math.max(0, idx)];
    }
}
