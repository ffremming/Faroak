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
 * Verifies the server replicates an advancing world-clock tick in snapshots, and
 * that a snapshot encoded without the world-time section decodes to 0 (legacy
 * compatibility).
 */
public final class MpWorldTimeProbe implements Probe {

    @Override public String name() { return "mp-world-time"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        try {
            MultiplayerConfig cfg = new MultiplayerConfig(
                MultiplayerMode.HOST, "loopback", "clockwatcher", 10, 30, 20, 1, 120, 20.0, 768.0, 1.0e9, "test.db");
            ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
            LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
                cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());

            String pid = "Watcher-001122";
            lobby.receive(new ProtocolEnvelope(1, pid, 0L, 0L, 0L, ProtocolMessageType.JOIN,
                codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
            lobby.tick();
            long t1 = latestWorldTime(snapshots(lobby.drainFor(pid), codec));
            // Move so each interval produces a delta snapshot carrying the world time.
            long seq = 1L;
            long t2 = t1;
            for (int i = 0; i < 12; i++) {
                boolean right = (i % 2) == 0;
                lobby.receive(new ProtocolEnvelope(1, pid, seq++, 0L, 0L, ProtocolMessageType.INPUT_STATE,
                    codec.encodeInputState(new ProtocolPayloads.InputState(false, !right, false, right))));
                lobby.tick();
                long t = latestWorldTime(snapshots(lobby.drainFor(pid), codec));
                if (t > 0L) t2 = Math.max(t2, t);
            }

            boolean present = t1 > 0L;       // NOON_TICK_OF_DAY offset means non-zero from the start
            boolean advancing = t2 > t1;

            // Legacy: a payload that the encoder produced before the world-time section
            // (simulate by round-tripping a snapshot whose worldTimeTicks default is 0).
            ProtocolPayloads.Snapshot bare = new ProtocolPayloads.Snapshot(
                false, 0L, new ArrayList<>());
            ProtocolPayloads.Snapshot decodedBare = codec.decodeSnapshot(codec.encodeSnapshot(bare));
            boolean defaultZero = decodedBare.worldTimeTicks == 0L;

            boolean ok = present && advancing && defaultZero;
            String details = "t1=" + t1 + " t2=" + t2 + " present=" + present
                + " advancing=" + advancing + " defaultZero=" + defaultZero;
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        }
    }

    private static long latestWorldTime(List<ProtocolPayloads.Snapshot> snaps) {
        long t = 0L;
        for (ProtocolPayloads.Snapshot s : snaps) t = Math.max(t, s.worldTimeTicks);
        return t;
    }

    private static List<ProtocolPayloads.Snapshot> snapshots(List<ProtocolEnvelope> envelopes, ProtocolPayloadCodec codec) {
        ArrayList<ProtocolPayloads.Snapshot> out = new ArrayList<>();
        for (ProtocolEnvelope e : envelopes) {
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) {
                out.add(codec.decodeSnapshot(e.payload()));
            }
        }
        return out;
    }
}
