package resources.testing.perf;

/** One sampled hotspot inside a measured case. */
public final class HotspotSample {

    private final String method;
    private final String location;
    private final int samples;
    private final double samplePct;
    private final double estimatedUs;
    private final double budgetPct;

    public HotspotSample(String method, String location, int samples,
                         double samplePct, double estimatedUs, double budgetPct) {
        this.method = method;
        this.location = location;
        this.samples = samples;
        this.samplePct = samplePct;
        this.estimatedUs = estimatedUs;
        this.budgetPct = budgetPct;
    }

    public String method() { return method; }
    public String location() { return location; }
    public int samples() { return samples; }
    public double samplePct() { return samplePct; }
    public double estimatedUs() { return estimatedUs; }
    public double budgetPct() { return budgetPct; }
}
