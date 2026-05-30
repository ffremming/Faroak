package resources.testing.probes;

import java.util.ArrayList;

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
 * Restart persistence check using shared in-memory store.
 */
public final class MultiplayerPersistenceProbe implements Probe {

    @Override public String name() { return "mp-persistence"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 2.0, 128.0, 2048.0, "test.db");
        InMemoryPersistenceStore store = new InMemoryPersistenceStore();
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();

        LobbyRuntime first = new AuthoritativeLobbyRuntime(cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        first.receive(new ProtocolEnvelope(1, "p1", 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        first.tick();
        ProtocolPayloads.InputState right = new ProtocolPayloads.InputState(false, false, false, true);
        first.receive(new ProtocolEnvelope(1, "p1", 1L, 0L, 0L, ProtocolMessageType.INPUT_STATE, codec.encodeInputState(right)));
        for (int i = 0; i < 40; i++) first.tick();
        double savedX = latestPlayerX(first.drainFor("p1"), codec, "p1");
        first.onDisconnect("p1");
        first.close();

        LobbyRuntime second = new AuthoritativeLobbyRuntime(cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        second.receive(new ProtocolEnvelope(1, "p1", 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        second.tick();
        second.tick();
        ArrayList<ProtocolEnvelope> out = new ArrayList<>(second.drainFor("p1"));
        second.close();

        double restoredX = 0.0;
        boolean found = false;
        for (ProtocolEnvelope e : out) {
            if (!ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())) continue;
            ProtocolPayloads.Snapshot snap = codec.decodeSnapshot(e.payload());
            for (ProtocolPayloads.PlayerState ps : snap.players) {
                if ("p1".equals(ps.playerId)) {
                    restoredX = ps.worldX;
                    found = true;
                }
            }
        }

        String detail = "found=" + found
            + ", savedX=" + String.format("%.2f", savedX)
            + ", restoredX=" + String.format("%.2f", restoredX);
        if (!found || Math.abs(restoredX - savedX) > 0.01) {
            return ProbeResult.fail(name() + " player state not restored", detail);
        }
        return ProbeResult.pass(name(), detail);
    }

    private static double latestPlayerX(
            java.util.List<ProtocolEnvelope> envelopes,
            ProtocolPayloadCodec codec,
            String playerId) {
        double latestX = 0.0;
        if (envelopes == null) return latestX;
        for (ProtocolEnvelope e : envelopes) {
            if (!ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                    && !ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) {
                continue;
            }
            ProtocolPayloads.Snapshot snap = codec.decodeSnapshot(e.payload());
            for (ProtocolPayloads.PlayerState ps : snap.players) {
                if (!playerId.equals(ps.playerId)) continue;
                latestX = ps.worldX;
            }
        }
        return latestX;
    }
}
