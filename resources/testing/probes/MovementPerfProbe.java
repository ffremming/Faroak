package resources.testing.probes;

import resources.app.GameContext;
import resources.domain.player.Playable;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Teleports the player across chunk boundaries and measures the simulate()
 * spike that follows — the EnvironmentManager's per-second {@code world.update}
 * triggers chunk reload, neighbour-rewiring, and visibility re-sort, all of
 * which feel like "lag while moving" to the user.
 *
 * Reports baseline avg vs post-move spike. A pass needs the spike to stay
 * within an order of magnitude of baseline.
 */
public final class MovementPerfProbe implements Probe {

    private static final Logger LOG = Logger.forClass(MovementPerfProbe.class);

    private static final int  WARMUP_TICKS         = 120;
    private static final int  BASELINE_SAMPLES     = 120;
    private static final int  POST_MOVE_SAMPLES    = 120;
    private static final int  MOVE_DELTA           = 1024;   // ~16 tiles
    private static final long PASS_RATIO           = 10;     // spike <= 10x baseline

    @Override public String name() { return "movement-perf"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");

        // Baseline: tick in place.
        harness.tick(WARMUP_TICKS);
        long baselineAvg = avgTickMicros(harness, BASELINE_SAMPLES);

        // Move player AND camera across chunk boundaries. EnvironmentManager
        // reads camera-center for world.update — without moving the camera,
        // chunks never reload and the spike is hidden.
        Playable player = (Playable) ctx.player();
        player.setWorldX(player.getWorldX() + MOVE_DELTA);
        player.setWorldY(player.getWorldY() + MOVE_DELTA);
        ctx.camera().moveX(MOVE_DELTA);
        ctx.camera().moveY(MOVE_DELTA);
        ctx.camera().getHitBox().centerAtPosition(player.getPoint());

        long postAvg = avgTickMicros(harness, POST_MOVE_SAMPLES);
        long worstPostMove = worstTickMicros(harness, POST_MOVE_SAMPLES);

        long ratio = postAvg == 0 ? 0 : postAvg / Math.max(1, baselineAvg);
        String detail = String.format("baseline=%d us, post-move=%d us, ratio=%dx, worst-post=%d us",
            baselineAvg, postAvg, ratio, worstPostMove);
        LOG.info(detail);

        return ratio <= PASS_RATIO
            ? ProbeResult.pass(name(), detail)
            : ProbeResult.fail(name() + " spike too high", detail);
    }

    private long avgTickMicros(TestHarness harness, int n) {
        long total = 0;
        for (int i = 0; i < n; i++) {
            long t0 = System.nanoTime();
            harness.tick(1);
            total += (System.nanoTime() - t0) / 1_000L;
        }
        return total / Math.max(1, n);
    }

    private long worstTickMicros(TestHarness harness, int n) {
        long max = 0;
        for (int i = 0; i < n; i++) {
            long t0 = System.nanoTime();
            harness.tick(1);
            long us = (System.nanoTime() - t0) / 1_000L;
            if (us > max) max = us;
        }
        return max;
    }
}
