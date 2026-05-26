package resources.testing.perf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Writes a stable JSON report for humans and AI agents to diff between runs. */
public final class PerfReportWriter {

    private PerfReportWriter() {}

    public static void write(Path out, Map<String, String> metadata,
                             List<PerfResult> results, List<String> recommendations)
            throws IOException {
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.write(out, toJson(metadata, results, recommendations).getBytes(StandardCharsets.UTF_8));
    }

    private static String toJson(Map<String, String> metadata,
                                 List<PerfResult> results,
                                 List<String> recommendations) {
        StringBuilder sb = new StringBuilder(12_000);
        sb.append("{\n");
        line(sb, "schema_version", "1", true);
        line(sb, "generated_at", quote(Instant.now().toString()), true);
        appendStringMap(sb, "metadata", metadata, true);
        appendStringArray(sb, "ai_usage", aiUsage(), true);
        appendStringArray(sb, "recommendations", recommendations, true);
        sb.append("  \"cases\": [\n");
        for (int i = 0; i < results.size(); i++) {
            appendResult(sb, results.get(i));
            sb.append(i == results.size() - 1 ? "\n" : ",\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendResult(StringBuilder sb, PerfResult result) {
        PerfStats s = result.stats();
        sb.append("    {\n");
        line(sb, "name", quote(result.name()), true, 6);
        line(sb, "category", quote(result.category()), true, 6);
        line(sb, "status", quote(result.status().name()), true, 6);
        line(sb, "description", quote(result.description()), true, 6);
        line(sb, "unit", quote("microseconds"), true, 6);
        line(sb, "samples", Integer.toString(s.count()), true, 6);
        line(sb, "budget_us", Long.toString(result.budgetUs()), true, 6);
        line(sb, "fail_us", Long.toString(result.failUs()), true, 6);
        line(sb, "budget_miss_rate", fmt(s.budgetMissRate(result.budgetUs())), true, 6);
        line(sb, "estimated_fps_from_avg", fmt(s.estimatedFps()), true, 6);
        appendStats(sb, s);
        sb.append(",\n");
        appendLongMap(sb, "counters", result.counters(), true, 6);
        appendHotspots(sb, result.hotspots(), true, 6);
        appendStringArray(sb, "notes", result.notes(), false, 6);
        sb.append("    }");
    }

    private static void appendStats(StringBuilder sb, PerfStats s) {
        sb.append("      \"stats\": {");
        sb.append("\"min_us\": ").append(s.min()).append(", ");
        sb.append("\"avg_us\": ").append(fmt(s.avg())).append(", ");
        sb.append("\"p50_us\": ").append(s.p50()).append(", ");
        sb.append("\"p90_us\": ").append(s.p90()).append(", ");
        sb.append("\"p95_us\": ").append(s.p95()).append(", ");
        sb.append("\"p99_us\": ").append(s.p99()).append(", ");
        sb.append("\"max_us\": ").append(s.max()).append("}");
    }

    private static void appendStringMap(StringBuilder sb, String name,
                                        Map<String, String> values, boolean comma) {
        sb.append("  \"").append(name).append("\": {\n");
        int i = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            sb.append("    ").append(quote(entry.getKey())).append(": ")
              .append(quote(entry.getValue()));
            sb.append(++i == values.size() ? "\n" : ",\n");
        }
        sb.append("  }").append(comma ? ",\n" : "\n");
    }

    private static void appendLongMap(StringBuilder sb, String name,
                                      Map<String, Long> values, boolean comma, int indent) {
        spaces(sb, indent).append("\"").append(name).append("\": {");
        int i = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            if (i > 0) sb.append(", ");
            sb.append(quote(entry.getKey())).append(": ").append(entry.getValue());
            i++;
        }
        sb.append("}").append(comma ? ",\n" : "\n");
    }

    private static void appendStringArray(StringBuilder sb, String name,
                                          List<String> values, boolean comma) {
        appendStringArray(sb, name, values, comma, 2);
    }

    private static void appendStringArray(StringBuilder sb, String name,
                                          List<String> values, boolean comma, int indent) {
        spaces(sb, indent).append("\"").append(name).append("\": [");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(quote(values.get(i)));
        }
        sb.append("]").append(comma ? ",\n" : "\n");
    }

    private static List<String> aiUsage() {
        return java.util.Arrays.asList(
            "Start with cases whose status is FAIL, then WARN.",
            "Use p95/p99/max for lag spikes; avg alone can hide stutter.",
            "Compare full-frame against update and render to find the dominant cost.",
            "In each case, inspect hotspots[0..n] to see exact methods eating budget.",
            "After an optimization, rerun and diff this JSON against the previous result."
        );
    }

    private static void appendHotspots(StringBuilder sb, List<HotspotSample> hotspots,
                                       boolean comma, int indent) {
        spaces(sb, indent).append("\"hotspots\": [");
        if (hotspots.isEmpty()) {
            sb.append("]").append(comma ? ",\n" : "\n");
            return;
        }
        sb.append("\n");
        for (int i = 0; i < hotspots.size(); i++) {
            HotspotSample h = hotspots.get(i);
            spaces(sb, indent + 2).append("{");
            sb.append("\"method\": ").append(quote(h.method())).append(", ");
            sb.append("\"location\": ").append(quote(h.location())).append(", ");
            sb.append("\"samples\": ").append(h.samples()).append(", ");
            sb.append("\"sample_pct\": ").append(fmt(h.samplePct())).append(", ");
            sb.append("\"estimated_us\": ").append(fmt(h.estimatedUs())).append(", ");
            sb.append("\"budget_pct\": ").append(fmt(h.budgetPct())).append("}");
            sb.append(i == hotspots.size() - 1 ? "\n" : ",\n");
        }
        spaces(sb, indent).append("]").append(comma ? ",\n" : "\n");
    }

    private static void line(StringBuilder sb, String key, String value, boolean comma) {
        line(sb, key, value, comma, 2);
    }

    private static void line(StringBuilder sb, String key, String value, boolean comma, int indent) {
        spaces(sb, indent).append(quote(key)).append(": ").append(value);
        sb.append(comma ? ",\n" : "\n");
    }

    private static StringBuilder spaces(StringBuilder sb, int count) {
        for (int i = 0; i < count; i++) sb.append(' ');
        return sb;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
