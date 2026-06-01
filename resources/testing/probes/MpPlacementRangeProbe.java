package resources.testing.probes;

import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.MultiplayerMode;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.ServerTerrainRules;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.InMemoryPersistenceStore;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Regression test for the placement range mismatch: the server must measure
 * placement/use range from the player's CENTER (matching the client's cursor-
 * relative targeting), not the top-left corner. A target that is in range from the
 * center but ~54px further from the corner must still be accepted — this was the
 * "mismatch between where the player is and the distance check for placing ships".
 */
public final class MpPlacementRangeProbe implements Probe {

    private static final int HOTBAR_OFFSET = 27;
    private static final int SLOT_CHEST = HOTBAR_OFFSET + 4;
    private static final double CENTER_OFFSET_X = 24.0;
    private static final double CENTER_OFFSET_Y = 48.0;

    @Override public String name() { return "mp-placement-range"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        String prevCount = System.getProperty("game.multiplayer.worldObjectCount");
        System.setProperty("game.multiplayer.worldObjectCount", "0");
        // Tight action range so the corner-vs-center offset is decisive.
        double range = 96.0;
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 20.0, range, 1.0e9, "test.db");
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        ServerTerrainRules terrain = new ServerTerrainRules();
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        try {
            String pid = "p1";
            lobby.receive(new ProtocolEnvelope(1, pid, 0L, 0L, 0L, ProtocolMessageType.JOIN,
                codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
            lobby.tick();
            ProtocolPayloads.PlayerState self = selfRow(snaps(lobby.drainFor(pid), codec), pid);
            if (self == null) return ProbeResult.fail(name() + " no spawn");
            double px = self.worldX, py = self.worldY;
            double centerX = px + CENTER_OFFSET_X, centerY = py + CENTER_OFFSET_Y;

            // Target a point on land that is within `range` of the CENTER but, due to
            // the +54px corner offset, would be OUTSIDE `range` from the corner.
            // Place it directly down-right from the player so the corner offset adds.
            // distFromCenter ~ range*0.9; distFromCorner ~ that + offset (> range).
            double targetX = 0, targetY = 0;
            boolean found = false;
            for (double ang = 0.0; ang < Math.PI * 2 && !found; ang += Math.PI / 8) {
                double d = range * 0.9;
                double tx = centerX + Math.cos(ang) * d;
                double ty = centerY + Math.sin(ang) * d;
                double fromCorner = Math.hypot(tx - px, ty - py);
                if (fromCorner > range && !terrain.isWaterAt(tx, ty)) { targetX = tx; targetY = ty; found = true; }
            }
            if (!found) return ProbeResult.skip(name() + " no qualifying land target near spawn");

            double fromCenter = Math.hypot(targetX - centerX, targetY - centerY);
            double fromCorner = Math.hypot(targetX - px, targetY - py);

            Step place = command(lobby, codec, pid, 1L,
                ProtocolPayloads.CommandRequest.useEquippedAt(targetX, targetY, "chest", SLOT_CHEST));

            // Must be accepted (in range from center). It is NOT a "too far away"
            // rejection — that's the bug this guards against.
            boolean notTooFar = !"too far away".equals(place.reason);
            boolean accepted = place.accepted;

            boolean ok = accepted && notTooFar;
            String details = "fromCenter=" + (int) fromCenter + " fromCorner=" + (int) fromCorner
                + " range=" + (int) range + " accepted=" + accepted + " reason='" + place.reason + "'";
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        } finally {
            if (prevCount == null) System.clearProperty("game.multiplayer.worldObjectCount");
            else System.setProperty("game.multiplayer.worldObjectCount", prevCount);
        }
    }

    private static final class Step { final boolean accepted; final String reason;
        Step(boolean a, String r) { accepted = a; reason = r; } }

    private static Step command(LobbyRuntime lobby, ProtocolPayloadCodec codec, String pid, long seq,
                                ProtocolPayloads.CommandRequest cmd) {
        lobby.receive(new ProtocolEnvelope(1, pid, seq, 0L, 0L, ProtocolMessageType.COMMAND, codec.encodeCommand(cmd)));
        lobby.tick();
        boolean accepted = false; String reason = "";
        for (ProtocolEnvelope e : lobby.drainFor(pid)) {
            if (ProtocolMessageType.COMMAND_RESULT.equals(e.messageType())) {
                ProtocolPayloads.CommandResult r = codec.decodeCommandResult(e.payload());
                accepted = r.accepted; reason = r.reason;
            }
        }
        return new Step(accepted, reason);
    }

    private static List<ProtocolPayloads.Snapshot> snaps(List<ProtocolEnvelope> env, ProtocolPayloadCodec codec) {
        ArrayList<ProtocolPayloads.Snapshot> out = new ArrayList<>();
        for (ProtocolEnvelope e : env) {
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) {
                out.add(codec.decodeSnapshot(e.payload()));
            }
        }
        return out;
    }

    private static ProtocolPayloads.PlayerState selfRow(List<ProtocolPayloads.Snapshot> snaps, String pid) {
        ProtocolPayloads.PlayerState found = null;
        for (ProtocolPayloads.Snapshot s : snaps) {
            for (ProtocolPayloads.PlayerState p : s.players) if (pid.equals(p.playerId)) found = p;
        }
        return found;
    }
}
