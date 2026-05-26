package resources.testing.perf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/** Tiny sampling profiler that reports hotspots by class.method(file:line). */
public final class HotspotProfiler {

    private HotspotProfiler() {}

    public interface Iteration {
        void run(int iteration);
    }

    public static List<HotspotSample> profile(int iterations, long sampleIntervalNs,
                                              long caseAvgUs, long caseBudgetUs, int topK,
                                              Iteration iteration) {
        AtomicBoolean measureWindow = new AtomicBoolean(false);
        AtomicBoolean done = new AtomicBoolean(false);
        CountDownLatch workerReady = new CountDownLatch(1);
        Map<String, Integer> hits = new HashMap<>();

        Thread worker = new Thread(() -> {
            workerReady.countDown();
            measureWindow.set(true);
            for (int i = 0; i < iterations; i++) iteration.run(i);
            measureWindow.set(false);
            done.set(true);
        }, "perf-hotspot-worker");

        Thread sampler = new Thread(() -> {
            await(workerReady);
            while (!done.get()) {
                if (measureWindow.get()) record(worker, hits);
                LockSupport.parkNanos(sampleIntervalNs);
            }
            if (measureWindow.get()) record(worker, hits);
        }, "perf-hotspot-sampler");
        sampler.setDaemon(true);

        sampler.start();
        worker.start();
        join(worker);
        join(sampler);

        return toSamples(hits, caseAvgUs, caseBudgetUs, topK);
    }

    private static void record(Thread worker, Map<String, Integer> hits) {
        StackTraceElement[] stack = worker.getStackTrace();
        for (StackTraceElement frame : stack) {
            String cls = frame.getClassName();
            if (!cls.startsWith("resources.")) continue;
            if (cls.startsWith("resources.testing.perf.")) continue;
            String key = frame.getClassName() + "#" + frame.getMethodName()
                + "|" + frame.getFileName() + ":" + frame.getLineNumber();
            hits.merge(key, 1, Integer::sum);
            return;
        }
    }

    private static List<HotspotSample> toSamples(Map<String, Integer> hits,
                                                 long caseAvgUs, long caseBudgetUs, int topK) {
        ArrayList<Map.Entry<String, Integer>> ordered = new ArrayList<>(hits.entrySet());
        ordered.sort(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue()).reversed());

        int total = 0;
        for (Map.Entry<String, Integer> e : ordered) total += e.getValue();
        ArrayList<HotspotSample> out = new ArrayList<>();
        if (total == 0) return out;

        int max = Math.min(Math.max(1, topK), ordered.size());
        for (int i = 0; i < max; i++) {
            Map.Entry<String, Integer> e = ordered.get(i);
            String[] parts = e.getKey().split("\\|", 2);
            String method = parts[0].replace('#', '.');
            String location = parts.length > 1 ? parts[1] : "unknown:0";
            double samplePct = 100.0 * e.getValue() / total;
            double estimatedUs = caseAvgUs * (samplePct / 100.0);
            double budgetPct = caseBudgetUs <= 0 ? 0.0 : 100.0 * estimatedUs / caseBudgetUs;
            out.add(new HotspotSample(method, location, e.getValue(),
                round2(samplePct), round2(estimatedUs), round2(budgetPct)));
        }
        return out;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join(30_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static double round2(double value) {
        return Double.parseDouble(String.format(Locale.US, "%.2f", value));
    }
}
