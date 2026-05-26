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
 * Verifies replicated object place/remove deltas and world restore on restart.
 */
public final class MultiplayerWorldReplicationProbe implements Probe {

    @Override public String name() { return "mp-world-replication"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 2.0, 256.0, 2048.0, "test.db");
        InMemoryPersistenceStore store = new InMemoryPersistenceStore();
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();

        LobbyRuntime first = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        first.receive(new ProtocolEnvelope(1, "p1", 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        first.tick();
        drainSnapshots(first.drainFor("p1"), codec); // consume baseline

        ProtocolPayloads.ActionRequest place1 = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.PLACE, true, 80.0, 80.0, "block");
        first.receive(new ProtocolEnvelope(1, "p1", 1L, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(place1)));
        first.tick();
        List<ProtocolPayloads.Snapshot> afterPlace = drainSnapshots(first.drainFor("p1"), codec);
        boolean placedSeen = hasObject(afterPlace, "block", false);

        ProtocolPayloads.ActionRequest attack = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.ATTACK, true, 80.0, 80.0, "");
        first.receive(new ProtocolEnvelope(1, "p1", 2L, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(attack)));
        first.tick();
        List<ProtocolPayloads.Snapshot> afterAttack = drainSnapshots(first.drainFor("p1"), codec);
        boolean removedSeen = hasObject(afterAttack, "block", true);

        first.tick();
        first.tick();
        first.tick(); // PLACE cooldown window
        ProtocolPayloads.ActionRequest place2 = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.PLACE, true, 160.0, 160.0, "fence");
        first.receive(new ProtocolEnvelope(1, "p1", 3L, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(place2)));
        first.tick();
        first.close();

        LobbyRuntime second = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        second.receive(new ProtocolEnvelope(1, "p1", 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        second.tick();
        List<ProtocolPayloads.Snapshot> restored = drainSnapshots(second.drainFor("p1"), codec);
        second.close();

        boolean restoredFence = hasObject(restored, "fence", false);
        String detail = "placedSeen=" + placedSeen + ", removedSeen=" + removedSeen + ", restoredFence=" + restoredFence;
        if (!placedSeen || !removedSeen || !restoredFence) {
            return ProbeResult.fail(name() + " world replication mismatch", detail);
        }
        return ProbeResult.pass(name(), detail);
    }

    private static List<ProtocolPayloads.Snapshot> drainSnapshots(List<ProtocolEnvelope> envelopes, ProtocolPayloadCodec codec) {
        ArrayList<ProtocolPayloads.Snapshot> snapshots = new ArrayList<>();
        if (envelopes == null) return snapshots;
        for (ProtocolEnvelope envelope : envelopes) {
            ProtocolMessageType type = envelope.messageType();
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(type) || ProtocolMessageType.DELTA_SNAPSHOT.equals(type)) {
                snapshots.add(codec.decodeSnapshot(envelope.payload()));
            }
        }
        return snapshots;
    }

    private static boolean hasObject(List<ProtocolPayloads.Snapshot> snapshots, String type, boolean removed) {
        if (snapshots == null) return false;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            if (snapshot == null) continue;
            for (ProtocolPayloads.WorldObjectState object : snapshot.worldObjects) {
                if (object == null) continue;
                if (removed != object.removed) continue;
                if (type.equals(object.objectType)) return true;
            }
        }
        return false;
    }
}
