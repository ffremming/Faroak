package resources.testing.probes;

import resources.net.multiplayer.MultiplayerRuntime;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the client reconciliation decision: within the tolerance, crisp local
 * movement is left untouched (no snap, no drift, no mushy lerp); beyond it, a
 * genuine desync triggers a snap. The collision-safety of the snap (never onto
 * water) is enforced in the runtime against live world state.
 */
public final class MpPredictionProbe implements Probe {

    @Override public String name() { return "mp-prediction"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        double tolerance = 64.0;

        // Aligned -> no snap.
        boolean alignedNoSnap = !MultiplayerRuntime.needsReconcileSnap(
            300.0, 300.0, 300.0, 300.0, tolerance);

        // Small divergence within tolerance (10px) -> still no snap (crisp local
        // movement stays in charge; this is what removed the mushy coasting).
        boolean smallNoSnap = !MultiplayerRuntime.needsReconcileSnap(
            300.0, 300.0, 310.0, 300.0, tolerance);

        // Just under tolerance -> no snap.
        boolean underNoSnap = !MultiplayerRuntime.needsReconcileSnap(
            300.0, 300.0, 300.0 + (tolerance - 1.0), 300.0, tolerance);

        // Beyond tolerance (real desync) -> snap.
        boolean largeSnaps = MultiplayerRuntime.needsReconcileSnap(
            300.0, 300.0, 300.0 + (tolerance + 50.0), 300.0, tolerance);

        boolean ok = alignedNoSnap && smallNoSnap && underNoSnap && largeSnaps;
        String details = "alignedNoSnap=" + alignedNoSnap + " smallNoSnap=" + smallNoSnap
            + " underNoSnap=" + underNoSnap + " largeSnaps=" + largeSnaps;
        return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
    }
}
