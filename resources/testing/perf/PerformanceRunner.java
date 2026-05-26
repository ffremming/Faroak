package resources.testing.perf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import resources.testing.TestHarness;

/** Runs structured performance scenarios and writes an AI-readable JSON report. */
public final class PerformanceRunner {

    private PerformanceRunner() {}

    public static void main(String[] args) throws Exception {
        PerfRunConfig config = PerfRunConfig.from(args);
        ArrayList<PerfResult> results = new ArrayList<>();
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        GcSnapshot gcStart = GcSnapshot.now();

        long bootStart = System.nanoTime();
        try (TestHarness harness = new TestHarness()) {
            long bootUs = (System.nanoTime() - bootStart) / 1_000L;
            PerfRunSupport.ensurePanelSize(harness);
            harness.tick(config.warmupTicks());

            PerfRunSupport.addMetadata(metadata, config, harness, gcStart, PerfCases.frameBudgetUs());
            results.add(PerfCases.boot(harness, bootUs));
            results.add(PerfCases.worldSimulate(harness, config.samples()));
            results.add(PerfCases.gameUpdate(harness, config.samples()));
            results.add(PerfCases.visibility(harness, config.samples()));
            results.add(PerfCases.render(harness, config.samples()));
            results.add(PerfCases.fullFrame(harness, config.samples()));
            results.add(PerfCases.movementLag(harness));
            results.add(PerfCases.gcPressure(harness, Math.max(30, config.samples() / 2)));
        }

        GcSnapshot gcEnd = GcSnapshot.now();
        metadata.put("heap_used_after_kb", Long.toString(gcEnd.usedHeapKb()));
        metadata.put("gc_collections_delta", Long.toString(gcEnd.collectionDelta(gcStart)));
        metadata.put("gc_collection_time_delta_ms", Long.toString(gcEnd.collectionTimeDeltaMs(gcStart)));

        List<String> recommendations = PerfRunSupport.recommendations(results, PerfCases.frameBudgetUs());
        PerfReportWriter.write(config.output(), metadata, results, recommendations);
        PerfRunSupport.printSummary(config, results);
        if (config.strictExit() && PerfRunSupport.hasFailure(results)) System.exit(1);
    }
}
