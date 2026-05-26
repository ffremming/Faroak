package resources.testing.perf;

import java.nio.file.Path;
import java.nio.file.Paths;

/** CLI options for the performance runner. */
public final class PerfRunConfig {

    private final int samples;
    private final int warmupTicks;
    private final Path output;
    private final boolean strictExit;

    private PerfRunConfig(int samples, int warmupTicks, Path output, boolean strictExit) {
        this.samples = samples;
        this.warmupTicks = warmupTicks;
        this.output = output;
        this.strictExit = strictExit;
    }

    public static PerfRunConfig from(String[] args) {
        int samples = 180;
        int warmup = 120;
        Path output = Paths.get("perf-results", "latest.json");
        boolean strict = true;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--samples".equals(arg) && i + 1 < args.length) {
                samples = Integer.parseInt(args[++i]);
            } else if ("--warmup".equals(arg) && i + 1 < args.length) {
                warmup = Integer.parseInt(args[++i]);
            } else if ("--out".equals(arg) && i + 1 < args.length) {
                output = Paths.get(args[++i]);
            } else if ("--report-only".equals(arg)) {
                strict = false;
            } else if ("--help".equals(arg)) {
                printHelpAndExit();
            }
        }
        return new PerfRunConfig(Math.max(30, samples), Math.max(0, warmup), output, strict);
    }

    public int samples() { return samples; }
    public int warmupTicks() { return warmupTicks; }
    public Path output() { return output; }
    public boolean strictExit() { return strictExit; }

    private static void printHelpAndExit() {
        System.out.println("Usage: java -cp /tmp/gamebuild resources.testing.perf.PerformanceRunner [options]");
        System.out.println("  --samples N      samples per steady-state case (default: 180)");
        System.out.println("  --warmup N       update ticks before measuring (default: 120)");
        System.out.println("  --out PATH       JSON report path (default: perf-results/latest.json)");
        System.out.println("  --report-only    always exit 0 after writing the report");
        System.exit(0);
    }
}
