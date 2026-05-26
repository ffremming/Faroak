package resources.testing.perf;

import java.util.Arrays;

/** Immutable timing distribution in microseconds. */
public final class PerfStats {

    private final long[] sorted;
    private final long total;

    private PerfStats(long[] samples) {
        this.sorted = samples.clone();
        Arrays.sort(this.sorted);
        long sum = 0;
        for (long sample : sorted) sum += sample;
        this.total = sum;
    }

    public static PerfStats of(long[] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("PerfStats needs at least one sample");
        }
        return new PerfStats(samples);
    }

    public int count() { return sorted.length; }
    public long min() { return sorted[0]; }
    public long max() { return sorted[sorted.length - 1]; }
    public long total() { return total; }
    public double avg() { return total / (double) sorted.length; }
    public long p50() { return percentile(50); }
    public long p90() { return percentile(90); }
    public long p95() { return percentile(95); }
    public long p99() { return percentile(99); }

    public double estimatedFps() {
        double avgUs = avg();
        return avgUs <= 0 ? 0.0 : 1_000_000.0 / avgUs;
    }

    public double budgetMissRate(long budgetUs) {
        int misses = 0;
        for (long sample : sorted) {
            if (sample > budgetUs) misses++;
        }
        return misses / (double) sorted.length;
    }

    private long percentile(int p) {
        int idx = (int) Math.ceil((p / 100.0) * sorted.length) - 1;
        idx = Math.max(0, Math.min(sorted.length - 1, idx));
        return sorted[idx];
    }
}
