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
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.InMemoryPersistenceStore;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Regression test for client-authoritative movement (the fix for the teleport-past-
 * collision bug). When the client reports its own collision-resolved position in an
 * InputState, the server must adopt that position (so client and server never diverge
 * and the player is never snapped), clamping only against gross teleport jumps and
 * water.
 */
public final class MpClientAuthMoveProbe implements Probe {

    @Override public String name() { return "mp-client-auth-move"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 20.0, 4096.0, 1.0e9, "test.db");
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        try {
            // Codec round-trip of the new position fields.
            ProtocolPayloads.InputState withPos = new ProtocolPayloads.InputState(false, false, false, true, true, 123.0, 456.0);
            ProtocolPayloads.InputState decoded = codec.decodeInputState(codec.encodeInputState(withPos));
            boolean codecOk = decoded.hasPosition && decoded.posX == 123.0 && decoded.posY == 456.0 && decoded.right;
            // Legacy keys-only frame decodes with hasPosition=false.
            ProtocolPayloads.InputState legacy = new ProtocolPayloads.InputState(true, false, false, false);
            ProtocolPayloads.InputState legacyDecoded = codec.decodeInputState(codec.encodeInputState(legacy));
            boolean legacyOk = !legacyDecoded.hasPosition && legacyDecoded.up;

            String pid = "p1";
            lobby.receive(new ProtocolEnvelope(1, pid, 0L, 0L, 0L, ProtocolMessageType.JOIN,
                codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
            lobby.tick();
            double[] spawn = pos(snaps(lobby.drainFor(pid), codec), pid);
            if (spawn == null) return ProbeResult.fail(name() + " no spawn");

            // Report a client position a modest distance away (within clamp + on land).
            double targetX = spawn[0] + 80.0;
            double targetY = spawn[1];
            ProtocolPayloads.InputState moved = new ProtocolPayloads.InputState(
                false, false, false, true, true, targetX, targetY);
            lobby.receive(new ProtocolEnvelope(1, pid, 1L, 0L, 0L, ProtocolMessageType.INPUT_STATE,
                codec.encodeInputState(moved)));
            for (int i = 0; i < 6; i++) lobby.tick();
            double[] after = pos(snaps(lobby.drainFor(pid), codec), pid);
            if (after == null) return ProbeResult.fail(name() + " no post-move row");

            // The server must have ADOPTED the client position (not re-simulated to a
            // different spot). It must be at the reported target, NOT drifting onward.
            boolean adopted = Math.abs(after[0] - targetX) < 24.0 && Math.abs(after[1] - targetY) < 24.0;

            boolean ok = codecOk && legacyOk && adopted;
            String details = "codec=" + codecOk + " legacy=" + legacyOk + " adopted=" + adopted
                + " target=(" + (int) targetX + "," + (int) targetY + ") server=(" + (int) after[0] + "," + (int) after[1] + ")";
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        }
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

    private static double[] pos(List<ProtocolPayloads.Snapshot> snaps, String pid) {
        double[] found = null;
        for (ProtocolPayloads.Snapshot s : snaps) {
            for (ProtocolPayloads.PlayerState p : s.players) {
                if (pid.equals(p.playerId)) found = new double[] { p.worldX, p.worldY };
            }
        }
        return found;
    }
}
