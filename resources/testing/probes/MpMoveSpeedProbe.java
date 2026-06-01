package resources.testing.probes;

import resources.domain.player.MovementController;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Regression test for the multiplayer movement-drift bug: the local player's walk
 * step is delta-corrected so its distance-per-second is frame-rate-independent and
 * matches the fixed-rate authoritative server (600 px/s), instead of scaling with
 * frame rate (the old 10px-per-frame behaviour drifted whenever FPS != 60 and made
 * reconciliation snap the player every so often).
 */
public final class MpMoveSpeedProbe implements Probe {

    @Override public String name() { return "mp-move-speed"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        double baseStep = 10.0;          // client per-frame walk step at delta == 1
        double serverPxPerSec = 600.0;   // 20 px/tick * 30 ticks/s

        // For each real frame rate, one wall-clock second is FPS frames, each with
        // delta = (1/FPS) / (1/60) = 60/FPS. Distance = sum(frameCorrectedStep).
        double[] fpsValues = { 60.0, 59.0, 50.0, 144.0, 30.0, 75.0 };
        double worstError = 0.0;
        StringBuilder sb = new StringBuilder();
        for (double fps : fpsValues) {
            double delta = 60.0 / fps;   // delta at this frame rate
            int frames = (int) Math.round(fps);
            double distance = 0.0;
            for (int i = 0; i < frames; i++) {
                distance += MovementController.frameCorrectedStep(baseStep, delta);
            }
            double error = Math.abs(distance - serverPxPerSec);
            worstError = Math.max(worstError, error);
            sb.append(String.format("fps=%.0f:%.1fpx ", fps, distance));
        }

        // Sanity: the OLD frame-locked model (no delta) drifts hard off 60 FPS.
        double oldAt144 = 144.0 * baseStep;            // 1440 px/s
        boolean oldModelDrifted = Math.abs(oldAt144 - serverPxPerSec) > 100.0;

        boolean frameRateIndependent = worstError < 1.0;   // within 1px of server/sec
        boolean ok = frameRateIndependent && oldModelDrifted;
        String details = "worstError=" + String.format("%.3f", worstError)
            + "px " + sb.toString().trim() + " oldModelDrifted=" + oldModelDrifted;
        return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
    }
}
