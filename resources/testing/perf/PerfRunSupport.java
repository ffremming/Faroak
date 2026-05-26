package resources.testing.perf;

import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import resources.app.GameContext;
import resources.domain.player.Playable;
import resources.testing.TestHarness;

/** Shared helpers for the performance runner. */
final class PerfRunSupport {

    private static final long HOTSPOT_SAMPLE_NS = 500_000L;
    private static final int HOTSPOT_TOP_K = 8;

    private PerfRunSupport() {}

    static void addMetadata(Map<String, String> out, PerfRunConfig config,
                            TestHarness harness, GcSnapshot gcStart, long frameBudgetUs) {
        out.put("java_version", System.getProperty("java.version"));
        out.put("jvm", ManagementFactory.getRuntimeMXBean().getVmName());
        out.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        out.put("processors", Integer.toString(Runtime.getRuntime().availableProcessors()));
        out.put("samples", Integer.toString(config.samples()));
        out.put("warmup_ticks", Integer.toString(config.warmupTicks()));
        out.put("target_fps", "60");
        out.put("frame_budget_us", Long.toString(frameBudgetUs));
        out.put("screen", harness.panel().getWidth() + "x" + harness.panel().getHeight());
        out.put("heap_used_before_kb", Long.toString(gcStart.usedHeapKb()));
        out.put("heap_max_kb", Long.toString(gcStart.maxHeapKb()));
    }

    static BufferedImage canvas(TestHarness harness) {
        int w = Math.max(1, harness.panel().getWidth());
        int h = Math.max(1, harness.panel().getHeight());
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    static void ensurePanelSize(TestHarness harness) {
        if (harness.panel().getWidth() <= 0 || harness.panel().getHeight() <= 0) {
            harness.panel().setSize(harness.context().screenWidth(), harness.context().screenHeight());
        }
    }

    static void movePlayerAndCamera(GameContext ctx, Playable player, int delta) {
        player.setWorldX(player.getWorldX() + delta);
        player.setWorldY(player.getWorldY() + delta);
        ctx.camera().moveX(delta);
        ctx.camera().moveY(delta);
        ctx.camera().getHitBox().centerAtPosition(player.getPoint());
    }

    static List<String> recommendations(List<PerfResult> results, long frameBudgetUs) {
        ArrayList<String> recs = new ArrayList<>();
        PerfResult full = find(results, "full-frame");
        PerfResult update = find(results, "game-update");
        PerfResult render = find(results, "render");
        PerfResult movement = find(results, "movement-chunk-lag");

        if (full != null && update != null && render != null
                && full.status() != PerfResult.Status.PASS) {
            String dominant = render.stats().avg() > update.stats().avg()
                ? "render" : "update/simulation";
            recs.add("Full-frame misses budget; the average cost is currently "
                + dominant + " dominated.");
        }
        if (full != null && !full.hotspots().isEmpty()) {
            HotspotSample top = full.hotspots().get(0);
            recs.add("Top full-frame hotspot: " + top.method()
                + " at " + top.location()
                + " (" + top.samplePct() + "% samples, ~" + top.budgetPct() + "% budget).");
        }
        if (movement != null && movement.stats().max() > frameBudgetUs) {
            recs.add("Movement has at least one frame-budget spike; inspect chunk update and visibility refresh work.");
        }
        if (movement != null && !movement.hotspots().isEmpty()) {
            HotspotSample top = movement.hotspots().get(0);
            recs.add("Top movement hotspot: " + top.method()
                + " at " + top.location()
                + " (~" + top.budgetPct() + "% of movement case budget).");
        }
        if (render != null && render.status() != PerfResult.Status.PASS) {
            recs.add("Render exceeds budget; Camera.draw allocates and repaints the whole scene each sample.");
        }
        if (recs.isEmpty()) recs.add("No measured case exceeded its budget on this run.");
        return recs;
    }

    static boolean hasFailure(List<PerfResult> results) {
        for (PerfResult result : results) {
            if (result.status() == PerfResult.Status.FAIL) return true;
        }
        return false;
    }

    static void printSummary(PerfRunConfig config, List<PerfResult> results) {
        System.out.println("Performance report written to " + config.output());
        for (PerfResult result : results) {
            System.out.println(result.status() + " " + result.name()
                + " avg=" + Math.round(result.stats().avg())
                + "us p95=" + result.stats().p95()
                + "us p99=" + result.stats().p99() + "us");
        }
    }

    static List<HotspotSample> hotspots(PerfResult result, int iterations,
                                        HotspotProfiler.Iteration iteration) {
        return HotspotProfiler.profile(
            Math.max(20, iterations),
            HOTSPOT_SAMPLE_NS,
            Math.round(result.stats().avg()),
            result.budgetUs(),
            HOTSPOT_TOP_K,
            iteration
        );
    }

    private static PerfResult find(List<PerfResult> results, String name) {
        for (PerfResult result : results) {
            if (result.name().equals(name)) return result;
        }
        return null;
    }
}
