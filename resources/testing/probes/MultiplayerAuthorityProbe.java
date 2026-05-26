package resources.testing.probes;

import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.MultiplayerAction;
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
 * Verifies sequence checks, action range checks, and ack behavior.
 */
public final class MultiplayerAuthorityProbe implements Probe {

    @Override public String name() { return "mp-authority"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 2.0, 128.0, 2048.0, "test.db");
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();

        lobby.receive(new ProtocolEnvelope(1, "p1", 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        lobby.tick();

        ProtocolPayloads.InputState up = new ProtocolPayloads.InputState(true, false, false, false);
        lobby.receive(new ProtocolEnvelope(1, "p1", 1L, 0L, 0L, ProtocolMessageType.INPUT_STATE, codec.encodeInputState(up)));
        lobby.tick();

        ProtocolPayloads.InputState stale = new ProtocolPayloads.InputState(false, false, true, false);
        lobby.receive(new ProtocolEnvelope(1, "p1", 1L, 0L, 0L, ProtocolMessageType.INPUT_STATE, codec.encodeInputState(stale)));
        lobby.tick();

        ProtocolPayloads.ActionRequest farPlace = new ProtocolPayloads.ActionRequest(MultiplayerAction.PLACE, true, 5000.0, 5000.0);
        lobby.receive(new ProtocolEnvelope(1, "p1", 2L, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(farPlace)));
        lobby.tick();
        lobby.tick();
        lobby.tick();

        ProtocolPayloads.ActionRequest nearAttack = new ProtocolPayloads.ActionRequest(MultiplayerAction.ATTACK, false, 0.0, 0.0);
        lobby.receive(new ProtocolEnvelope(1, "p1", 3L, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(nearAttack)));
        lobby.tick();

        List<ProtocolEnvelope> out = new ArrayList<>(lobby.drainFor("p1"));
        lobby.close();

        boolean ack1 = false;
        boolean ack2 = false;
        boolean ack3 = false;
        long maxSnapshotAck = 0L;
        for (ProtocolEnvelope e : out) {
            if (ProtocolMessageType.ACK.equals(e.messageType())) {
                long seq = codec.decodeAck(e.payload()).acknowledgedSequence;
                if (seq == 1L) ack1 = true;
                if (seq == 2L) ack2 = true;
                if (seq == 3L) ack3 = true;
            }
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
                    || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType())) {
                ProtocolPayloads.Snapshot snap = codec.decodeSnapshot(e.payload());
                maxSnapshotAck = Math.max(maxSnapshotAck, snap.acknowledgedSequence);
            }
        }

        String detail = "ack1=" + ack1 + ", ack2=" + ack2 + ", ack3=" + ack3 + ", maxSnapAck=" + maxSnapshotAck;
        if (!ack1 || ack2 || Math.max(maxSnapshotAck, ack3 ? 3L : 0L) < 3L) {
            return ProbeResult.fail(name() + " invalid ack gating", detail);
        }
        return ProbeResult.pass(name(), detail);
    }
}
