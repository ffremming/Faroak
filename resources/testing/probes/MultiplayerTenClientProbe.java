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
 * Integration sanity for 10 players + single lobby cap.
 */
public final class MultiplayerTenClientProbe implements Probe {

    @Override public String name() { return "mp-10-clients"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "host", 10, 30, 20, 1, 120, 2.0, 128.0, 2048.0, "test.db");
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();

        for (int i = 1; i <= 10; i++) {
            lobby.receive(new ProtocolEnvelope(1, "p" + i, 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        }
        lobby.tick();

        for (int i = 1; i <= 10; i++) {
            boolean up = (i % 2 == 0);
            ProtocolPayloads.InputState input = new ProtocolPayloads.InputState(up, !up, false, false);
            lobby.receive(new ProtocolEnvelope(1, "p" + i, 1L, 0L, 0L, ProtocolMessageType.INPUT_STATE, codec.encodeInputState(input)));
        }
        lobby.tick();
        lobby.tick();

        boolean allHaveSnapshots = true;
        boolean anyReject = false;
        for (int i = 1; i <= 10; i++) {
            ArrayList<ProtocolEnvelope> out = new ArrayList<>(lobby.drainFor("p" + i));
            boolean hasSnapshot = false;
            for (ProtocolEnvelope e : out) {
                if (ProtocolMessageType.REJECT.equals(e.messageType())) anyReject = true;
                if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType()) || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) {
                    hasSnapshot = true;
                }
            }
            allHaveSnapshots &= hasSnapshot;
        }

        lobby.receive(new ProtocolEnvelope(1, "p11", 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        lobby.tick();
        ArrayList<ProtocolEnvelope> extra = new ArrayList<>(lobby.drainFor("p11"));
        lobby.close();

        boolean p11Rejected = false;
        for (ProtocolEnvelope e : extra) {
            if (ProtocolMessageType.REJECT.equals(e.messageType())) p11Rejected = true;
        }

        String detail = "snapshots=" + allHaveSnapshots + ", anyReject=" + anyReject + ", p11Rejected=" + p11Rejected;
        if (!allHaveSnapshots || anyReject || !p11Rejected) return ProbeResult.fail(name() + " lobby constraint failed", detail);
        return ProbeResult.pass(name(), detail);
    }
}
