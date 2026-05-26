package resources.testing.probes;

import java.util.ArrayList;

import resources.net.multiplayer.protocol.BinaryEnvelopeCodec;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies protocol envelope/payload round-trip and malformed-frame handling.
 */
public final class ProtocolCodecProbe implements Probe {

    @Override public String name() { return "protocol-codec"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        BinaryEnvelopeCodec envelopeCodec = new BinaryEnvelopeCodec();
        ProtocolPayloadCodec payloadCodec = new ProtocolPayloadCodec();

        ArrayList<ProtocolPayloads.PlayerState> players = new ArrayList<>();
        players.add(new ProtocolPayloads.PlayerState("p-a", 10.0, 20.0, 1.0, -1.0, 11L));
        players.add(new ProtocolPayloads.PlayerState("p-b", 40.0, 80.0, 0.0, 2.0, 7L));
        ArrayList<ProtocolPayloads.WorldObjectState> worldObjects = new ArrayList<>();
        worldObjects.add(new ProtocolPayloads.WorldObjectState(99L, "block", 320.0, 192.0, false, 5L));
        ProtocolPayloads.Snapshot snapshot = new ProtocolPayloads.Snapshot(false, 11L, players, worldObjects);
        byte[] payload = payloadCodec.encodeSnapshot(snapshot);
        ProtocolEnvelope source = new ProtocolEnvelope(
            1, "p-a", 12L, 11L, 33L, ProtocolMessageType.DELTA_SNAPSHOT, payload);

        ProtocolEnvelope decoded = envelopeCodec.decode(envelopeCodec.encode(source));
        if (decoded == null) return ProbeResult.fail(name() + " envelope decode null");
        ProtocolPayloads.Snapshot decodedSnap = payloadCodec.decodeSnapshot(decoded.payload());
        boolean structureOk = decoded.protocolVersion() == 1
            && ProtocolMessageType.DELTA_SNAPSHOT.equals(decoded.messageType())
            && decodedSnap.players.size() == 2
            && "p-b".equals(decodedSnap.players.get(1).playerId)
            && decodedSnap.worldObjects.size() == 1
            && "block".equals(decodedSnap.worldObjects.get(0).objectType)
            && decodedSnap.acknowledgedSequence == 11L;
        if (!structureOk) return ProbeResult.fail(name() + " round-trip mismatch");

        ProtocolEnvelope malformed = envelopeCodec.decode(new byte[] {1, 2, 3});
        if (malformed != null) return ProbeResult.fail(name() + " malformed bytes accepted");

        return ProbeResult.pass(name(), "snapshot-players=2, snapshot-objects=1, malformed-rejected=true");
    }
}
