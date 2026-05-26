package resources.testing.perf;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import resources.app.GameContext;
import resources.domain.player.Playable;
import resources.testing.TestHarness;

/** Individual performance scenarios used by PerformanceRunner. */
final class PerfCases {

    private static final long FRAME_BUDGET_US = 16_667L;
    private static final long FRAME_FAIL_US = 33_334L;
    private static final long UPDATE_BUDGET_US = 8_000L;
    private static final long UPDATE_FAIL_US = 25_000L;
    private static final long RENDER_BUDGET_US = 10_000L;
    private static final long RENDER_FAIL_US = 30_000L;
    private static final long SIM_BUDGET_US = 5_000L;
    private static final long VIS_BUDGET_US = 2_000L;
    private static final int MOVE_DELTA = 1024;
    private static final int MOVEMENT_TICKS = 90;

    private PerfCases() {}

    static long frameBudgetUs() { return FRAME_BUDGET_US; }

    static PerfResult boot(TestHarness harness, long bootUs) {
        GameContext ctx = harness.context();
        return PerfResult.of("boot", "startup", "Construct GamePanel, load resources, and generate initial world.",
            3_000_000L, 10_000_000L, new long[] { bootUs })
            .counter("chunks", ctx.world().getChunks().size())
            .counter("tiles", ctx.world().getTiles().size())
            .counter("entities", ctx.world().getEntities().size());
    }

    static PerfResult worldSimulate(TestHarness harness, int samples) {
        GameContext ctx = harness.context();
        long[] timings = PerfSampler.sampleMicros(samples, () -> ctx.world().simulate());
        PerfResult result = PerfResult.of("world-simulate", "simulation",
            "WorldRuntime.simulate only; isolates entity/tile update cost.",
            SIM_BUDGET_US, UPDATE_FAIL_US, timings)
            .counter("entities", ctx.world().getEntities().size())
            .counter("tiles", ctx.world().getTiles().size());
        result.hotspots(PerfRunSupport.hotspots(result, samples, i -> ctx.world().simulate()));
        return result;
    }

    static PerfResult gameUpdate(TestHarness harness, int samples) {
        long[] timings = PerfSampler.sampleMicros(samples, () -> harness.tick(1));
        PerfResult result = PerfResult.of("game-update", "simulation",
            "One GamePanel.update tick: environment, input, world simulate, and clock.",
            UPDATE_BUDGET_US, UPDATE_FAIL_US, timings);
        result.hotspots(PerfRunSupport.hotspots(result, samples, i -> harness.tick(1)));
        return result;
    }

    static PerfResult visibility(TestHarness harness, int samples) {
        GameContext ctx = harness.context();
        long[] timings = PerfSampler.sampleMicros(samples, () -> {
            ctx.world().getVisibleTiles(ctx.camera());
            ctx.world().getVisibleEntities(ctx.camera());
        });
        PerfResult result = PerfResult.of("visibility-query", "render-prep",
            "Camera visibility culling without drawing pixels.",
            VIS_BUDGET_US, 10_000L, timings)
            .counter("visible_tiles", ctx.world().getVisibleTiles(ctx.camera()).size())
            .counter("visible_entities", ctx.world().getVisibleEntities(ctx.camera()).size());
        result.hotspots(PerfRunSupport.hotspots(result, samples, i -> {
            ctx.world().getVisibleTiles(ctx.camera());
            ctx.world().getVisibleEntities(ctx.camera());
        }));
        return result;
    }

    static PerfResult render(TestHarness harness, int samples) {
        GameContext ctx = harness.context();
        BufferedImage canvas = PerfRunSupport.canvas(harness);
        Graphics2D g2 = canvas.createGraphics();
        try {
            long[] timings = new long[samples];
            for (int i = 0; i < samples; i++) {
                harness.tick(1);
                timings[i] = PerfSampler.timeDrawingMicros(canvas, i, () -> ctx.camera().draw(g2));
            }
            PerfResult result = PerfResult.of("render", "rendering",
                "Camera.draw after an untimed update; includes scene, lighting, UI, and debug overlay.",
                RENDER_BUDGET_US, RENDER_FAIL_US, timings)
                .counter("canvas_width", canvas.getWidth())
                .counter("canvas_height", canvas.getHeight())
                .note("Each sample updates once before timing so the render state matches a live frame.");
            result.hotspots(PerfRunSupport.hotspots(result, samples, i -> {
                harness.tick(1);
                PerfSampler.timeDrawingMicros(canvas, i, () -> ctx.camera().draw(g2));
            }));
            return result;
        } finally {
            g2.dispose();
        }
    }

    static PerfResult fullFrame(TestHarness harness, int samples) {
        GameContext ctx = harness.context();
        BufferedImage canvas = PerfRunSupport.canvas(harness);
        Graphics2D g2 = canvas.createGraphics();
        try {
            long[] timings = PerfSampler.sampleDrawingMicros(samples, canvas, () -> {
                harness.tick(1);
                ctx.camera().draw(g2);
            });
            PerfResult result = PerfResult.of("full-frame", "frame-rate",
                "One logical update plus one camera draw; use this as the FPS proxy.",
                FRAME_BUDGET_US, FRAME_FAIL_US, timings);
            result.hotspots(PerfRunSupport.hotspots(result, samples, i -> {
                harness.tick(1);
                PerfSampler.timeDrawingMicros(canvas, i, () -> ctx.camera().draw(g2));
            }));
            return result;
        } finally {
            g2.dispose();
        }
    }

    static PerfResult movementLag(TestHarness harness) {
        GameContext ctx = harness.context();
        Playable player = ctx.player();
        long[] timings = new long[MOVEMENT_TICKS * 4];
        long worstChunkUpdateUs = 0;
        int idx = 0;
        for (int move = 0; move < 4; move++) {
            PerfRunSupport.movePlayerAndCamera(ctx, player, MOVE_DELTA);
            for (int tick = 0; tick < MOVEMENT_TICKS; tick++) {
                timings[idx++] = PerfSampler.timeMicros(() -> harness.tick(1));
                worstChunkUpdateUs = Math.max(worstChunkUpdateUs,
                    ctx.camera().getObservedChunkUpdateTime() / 1_000L);
            }
        }
        PerfResult result = PerfResult.of("movement-chunk-lag", "stutter",
            "Teleport across chunk boundaries and measure update spikes that feel like movement lag.",
            FRAME_BUDGET_US, 50_000L, timings)
            .counter("move_delta_px", MOVE_DELTA)
            .counter("ticks_after_each_move", MOVEMENT_TICKS)
            .counter("worst_chunk_update_us", worstChunkUpdateUs);
        result.hotspots(PerfRunSupport.hotspots(result, MOVEMENT_TICKS * 2, i -> {
            if (i % MOVEMENT_TICKS == 0) PerfRunSupport.movePlayerAndCamera(ctx, player, MOVE_DELTA);
            harness.tick(1);
        }));
        return result;
    }

    static PerfResult gcPressure(TestHarness harness, int samples) {
        GameContext ctx = harness.context();
        BufferedImage canvas = PerfRunSupport.canvas(harness);
        Graphics2D g2 = canvas.createGraphics();
        GcSnapshot before = GcSnapshot.now();
        try {
            long[] timings = PerfSampler.sampleDrawingMicros(samples, canvas, () -> {
                harness.tick(1);
                ctx.camera().draw(g2);
            });
            GcSnapshot after = GcSnapshot.now();
            PerfResult result = PerfResult.of("full-frame-gc-pressure", "memory",
                "Repeated full frames with heap and GC counters to expose allocation churn.",
                FRAME_BUDGET_US, FRAME_FAIL_US, timings)
                .counter("heap_used_delta_kb", after.usedHeapDeltaKb(before))
                .counter("gc_collections_delta", after.collectionDelta(before))
                .counter("gc_collection_time_delta_ms", after.collectionTimeDeltaMs(before));
            result.hotspots(PerfRunSupport.hotspots(result, samples, i -> {
                harness.tick(1);
                PerfSampler.timeDrawingMicros(canvas, i, () -> ctx.camera().draw(g2));
            }));
            return result;
        } finally {
            g2.dispose();
        }
    }
}
