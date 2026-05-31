package resources.testing.probes;

import resources.net.multiplayer.MultiplayerRuntime;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the client reconciliation step corrects the current pose toward the
 * authoritative server pose and, crucially, CONVERGES: applied repeatedly with a
 * fixed target it must approach the target and then stop (no perpetual drift).
 * This is the invariant whose absence caused the "moves without input" bug.
 */
public final class MpPredictionProbe implements Probe {

    @Override public String name() { return "mp-prediction"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        double blend = 0.15;
        double warp = 96.0;

        // 1. Already aligned with the server -> no drift, even applied many times.
        double cx = 300.0, cy = 300.0;
        for (int i = 0; i < 120; i++) {
            double[] r = MultiplayerRuntime.reconcileStep(cx, cy, 300.0, 300.0, blend, warp);
            cx = r[0]; cy = r[1];
        }
        boolean noDrift = Math.abs(cx - 300.0) < 1.0e-9 && Math.abs(cy - 300.0) < 1.0e-9;

        // 2. Small divergence: a single step moves a fraction toward the target.
        double[] one = MultiplayerRuntime.reconcileStep(300.0, 300.0, 310.0, 300.0, blend, warp);
        boolean movesTowardTarget = one[0] > 300.0 && one[0] < 310.0 && Math.abs(one[1] - 300.0) < 1.0e-9;

        // 3. CONVERGENCE: applied repeatedly toward a FIXED target, the pose must
        //    approach the target and settle there (this is the bug regression test).
        cx = 300.0; cy = 300.0;
        for (int i = 0; i < 200; i++) {
            double[] r = MultiplayerRuntime.reconcileStep(cx, cy, 350.0, 300.0, blend, warp);
            cx = r[0]; cy = r[1];
        }
        boolean converges = Math.abs(cx - 350.0) < 0.5 && Math.abs(cy - 300.0) < 1.0e-9;

        // 4. Large divergence beyond warp distance: snap straight to the target.
        double[] warped = MultiplayerRuntime.reconcileStep(300.0, 300.0, 1000.0, 1000.0, blend, warp);
        boolean warpedOk = warped[0] == 1000.0 && warped[1] == 1000.0;

        boolean ok = noDrift && movesTowardTarget && converges && warpedOk;
        String details = "noDrift=" + noDrift + " movesToward=" + movesTowardTarget
            + " converges=" + converges + " (settledX=" + cx + ") warp=" + warpedOk;
        return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
    }
}
