package resources.testing;

/**
 * Outcome of running a {@link Probe}. Value object; treat as immutable.
 *
 * Carries a one-line headline (for summary tables) plus a free-form details
 * blob (numbers, sample data) printed only on failure or when verbose.
 */
public final class ProbeResult {

    public enum Status { PASS, FAIL, SKIP }

    private final Status status;
    private final String headline;
    private final String details;

    private ProbeResult(Status status, String headline, String details) {
        this.status   = status;
        this.headline = headline;
        this.details  = details;
    }

    public static ProbeResult pass(String headline)                       { return new ProbeResult(Status.PASS, headline, ""); }
    public static ProbeResult pass(String headline, String details)       { return new ProbeResult(Status.PASS, headline, details); }
    public static ProbeResult fail(String headline)                       { return new ProbeResult(Status.FAIL, headline, ""); }
    public static ProbeResult fail(String headline, String details)       { return new ProbeResult(Status.FAIL, headline, details); }
    public static ProbeResult skip(String headline)                       { return new ProbeResult(Status.SKIP, headline, ""); }

    public Status status()   { return status; }
    public String headline() { return headline; }
    public String details()  { return details; }

    public boolean isPass() { return status == Status.PASS; }
}
