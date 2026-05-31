package resources.testing.probes;

import resources.net.multiplayer.MultiplayerRuntime;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the client reconciliation step: zero correction when prediction matches
 * the server (no jitter), a fractional nudge toward the residual error for small
 * divergence, and a hard snap on large divergence.
 */
public final class MpPredictionProbe implements Probe {

    @Override public String name() { return "mp-prediction"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        double blend = 0.15;
        double warp = 96.0;

        // 1. Server agrees with our prediction at the ack -> no correction (dead-zone),
        //    even though our *current* predicted pose has moved on.
        double[] noOp = MultiplayerRuntime.reconcileStep(
            500.0, 500.0,   // current predicted pose (moved ahead)
            300.0, 300.0,   // server pose at ack
            300.0, 300.0,   // predicted pose at ack (matches server)
            blend, warp);
        boolean noCorrection = noOp[0] == 500.0 && noOp[1] == 500.0;

        // 2. Small residual error (server was 10px ahead of our prediction at the ack):
        //    nudge current pose by blend * error.
        double[] nudged = MultiplayerRuntime.reconcileStep(
            500.0, 500.0,
            310.0, 300.0,   // server 10px right of where we predicted at ack
            300.0, 300.0,
            blend, warp);
        double expectedX = 500.0 + (10.0 * blend);
        boolean nudgedOk = Math.abs(nudged[0] - expectedX) < 1.0e-9 && nudged[1] == 500.0;

        // 3. Large residual error (beyond warp distance): snap to the server pose.
        double[] warped = MultiplayerRuntime.reconcileStep(
            500.0, 500.0,
            1000.0, 1000.0, // far from predicted-at-ack -> warp
            300.0, 300.0,
            blend, warp);
        boolean warpedOk = warped[0] == 1000.0 && warped[1] == 1000.0;

        boolean ok = noCorrection && nudgedOk && warpedOk;
        String details = "noCorrection=" + noCorrection + " nudged=" + nudgedOk + " warped=" + warpedOk;
        return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
    }
}
