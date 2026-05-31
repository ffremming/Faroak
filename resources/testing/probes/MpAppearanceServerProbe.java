package resources.testing.probes;

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
 * Verifies the server derives and replicates per-player appearance: facing and
 * moving from velocity, and a friendly displayName from the playerId.
 */
public final class MpAppearanceServerProbe implements Probe {

    @Override public String name() { return "mp-appearance-server"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "Alice-ab12cd", 10, 30, 20, 1, 120, 20.0, 768.0, 8192.0, "test.db");
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        try {
            String pid = "Alice-ab12cd";
            lobby.receive(new ProtocolEnvelope(1, pid, 0L, 0L, 0L, ProtocolMessageType.JOIN,
                codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
            lobby.tick();
            lobby.drainFor(pid);

            // Drive movement to the right and tick a few times.
            ProtocolPayloads.InputState right = new ProtocolPayloads.InputState(false, false, false, true);
            lobby.receive(new ProtocolEnvelope(1, pid, 1L, 0L, 0L, ProtocolMessageType.INPUT_STATE,
                codec.encodeInputState(right)));
            for (int i = 0; i < 5; i++) lobby.tick();

            ProtocolPayloads.PlayerState self = playerRow(snapshots(lobby.drainFor(pid), codec), pid);
            if (self == null) return ProbeResult.fail(name() + " no self row in snapshot");

            boolean movingOk = self.moving;
            boolean facingRight = self.facing == 1;           // 1 = right
            boolean nameOk = "Alice".equals(self.displayName);
            boolean aliveOk = self.alive;

            // Stop moving; facing should persist, moving should clear.
            ProtocolPayloads.InputState none = new ProtocolPayloads.InputState(false, false, false, false);
            lobby.receive(new ProtocolEnvelope(1, pid, 2L, 0L, 0L, ProtocolMessageType.INPUT_STATE,
                codec.encodeInputState(none)));
            for (int i = 0; i < 4; i++) lobby.tick();
            ProtocolPayloads.PlayerState idle = playerRow(snapshots(lobby.drainFor(pid), codec), pid);
            boolean idleStops = idle == null || !idle.moving;
            boolean facingPersists = idle == null || idle.facing == 1;

            boolean ok = movingOk && facingRight && nameOk && aliveOk && idleStops && facingPersists;
            String details = "moving=" + movingOk + " facingRight=" + facingRight + " name=" + nameOk
                + " alive=" + aliveOk + " idleStops=" + idleStops + " facingPersists=" + facingPersists;
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        }
    }

    private static List<ProtocolPayloads.Snapshot> snapshots(List<ProtocolEnvelope> envelopes, ProtocolPayloadCodec codec) {
        java.util.ArrayList<ProtocolPayloads.Snapshot> out = new java.util.ArrayList<>();
        for (ProtocolEnvelope e : envelopes) {
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) {
                out.add(codec.decodeSnapshot(e.payload()));
            }
        }
        return out;
    }

    private static ProtocolPayloads.PlayerState playerRow(List<ProtocolPayloads.Snapshot> snapshots, String playerId) {
        ProtocolPayloads.PlayerState found = null;
        for (ProtocolPayloads.Snapshot s : snapshots) {
            for (ProtocolPayloads.PlayerState p : s.players) {
                if (playerId.equals(p.playerId)) found = p;
            }
        }
        return found;
    }
}
