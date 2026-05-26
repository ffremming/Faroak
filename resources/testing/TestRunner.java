package resources.testing;

import java.util.ArrayList;
import java.util.List;

import resources.testing.probes.AnimationFrameProbe;
import resources.testing.probes.MovementPerfProbe;
import resources.testing.probes.SimulatePerfProbe;
import resources.testing.probes.TileBorderProbe;

/**
 * Entry point for the test harness. Boots a {@link TestHarness}, runs each
 * registered {@link Probe}, prints a summary, exits with code 1 if any probe
 * fails.
 *
 * Use case: invoke from CLI after a refactor to confirm gameplay invariants
 * (borders present, animation ticking, simulate() not blowing up) without a
 * full visual inspection.
 *
 * Run with:
 *   java -cp /tmp/gamebuild resources.testing.TestRunner
 */
public final class TestRunner {

    private static final Logger LOG = Logger.forClass(TestRunner.class);

    public static void main(String[] args) {
        if (args.length > 0 && "verbose".equalsIgnoreCase(args[0])) {
            Logger.setThreshold(Logger.Level.DEBUG);
        }

        List<Probe> probes = new ArrayList<>();
        probes.add(new TileBorderProbe());
        probes.add(new AnimationFrameProbe());
        probes.add(new SimulatePerfProbe());
        probes.add(new MovementPerfProbe());

        int failures = 0;
        try (TestHarness harness = new TestHarness()) {
            harness.tick(120); // warm-up: load chunks, settle animation
            LOG.info("running %d probes…", probes.size());

            for (Probe probe : probes) {
                LOG.info("→ %s", probe.name());
                ProbeResult result = probe.run(harness);
                printResult(probe, result);
                if (result.status() == ProbeResult.Status.FAIL) failures++;
            }
        }

        LOG.info("---");
        LOG.info("%d probe(s), %d failure(s)", probes.size(), failures);
        if (failures > 0) System.exit(1);
    }

    private static void printResult(Probe probe, ProbeResult result) {
        String prefix = "  " + result.status() + "  " + probe.name();
        LOG.info("%s — %s", prefix, result.headline());
        if (!result.details().isEmpty()) LOG.info("      %s", result.details());
    }
}
