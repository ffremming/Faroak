package resources.testing.perf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One measured performance aspect plus its budget decision. */
public final class PerfResult {

    public enum Status { PASS, WARN, FAIL }

    private final String name;
    private final String category;
    private final String description;
    private final long budgetUs;
    private final long failUs;
    private final PerfStats stats;
    private final Status status;
    private final LinkedHashMap<String, Long> counters = new LinkedHashMap<>();
    private final ArrayList<String> notes = new ArrayList<>();
    private final ArrayList<HotspotSample> hotspots = new ArrayList<>();

    private PerfResult(String name, String category, String description,
                       long budgetUs, long failUs, PerfStats stats) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.budgetUs = budgetUs;
        this.failUs = failUs;
        this.stats = stats;
        this.status = statusFor(stats, budgetUs, failUs);
    }

    public static PerfResult of(String name, String category, String description,
                                long budgetUs, long failUs, long[] samplesUs) {
        return new PerfResult(name, category, description, budgetUs, failUs, PerfStats.of(samplesUs));
    }

    public PerfResult counter(String key, long value) {
        counters.put(key, value);
        return this;
    }

    public PerfResult note(String note) {
        if (note != null && !note.isEmpty()) notes.add(note);
        return this;
    }

    public PerfResult hotspots(List<HotspotSample> values) {
        hotspots.clear();
        if (values != null) hotspots.addAll(values);
        return this;
    }

    public String name() { return name; }
    public String category() { return category; }
    public String description() { return description; }
    public long budgetUs() { return budgetUs; }
    public long failUs() { return failUs; }
    public PerfStats stats() { return stats; }
    public Status status() { return status; }
    public Map<String, Long> counters() { return counters; }
    public List<String> notes() { return notes; }
    public List<HotspotSample> hotspots() { return hotspots; }

    private static Status statusFor(PerfStats stats, long budgetUs, long failUs) {
        double missRate = stats.budgetMissRate(budgetUs);
        if (stats.p99() > failUs) return Status.FAIL;
        if (stats.count() >= 30 && missRate > 0.20) return Status.FAIL;
        if (stats.p95() > budgetUs || missRate > 0.05) return Status.WARN;
        return Status.PASS;
    }
}
