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
import resources.net.multiplayer.server.ServerTerrainRules;
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
        List<ProtocolPayloads.Snapshot> initial = drainSnapshots(first.drainFor("p1"), codec); // consume baseline
        double[] spawn = findPlayer(initial, "p1");
        ServerTerrainRules terrain = new ServerTerrainRules();
        double[] target1 = findNearbyLand(terrain, spawn[0] + 96.0, spawn[1], 256.0);
        double[] target2 = findNearbyLand(terrain, spawn[0] - 160.0, spawn[1] + 64.0, 256.0);
        if (target1 == null || target2 == null) {
            first.close();
            return ProbeResult.skip(name() + " no land placement targets near spawn");
        }

        ProtocolPayloads.ActionRequest place1 = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.PLACE, true, target1[0], target1[1], "block");
        first.receive(new ProtocolEnvelope(1, "p1", 1L, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(place1)));
        first.tick();
        List<ProtocolPayloads.Snapshot> afterPlace = drainSnapshots(first.drainFor("p1"), codec);
        boolean placedSeen = hasObject(afterPlace, "block", false);

        ProtocolPayloads.ActionRequest attack = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.ATTACK, true, target1[0], target1[1], "");
        first.receive(new ProtocolEnvelope(1, "p1", 2L, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(attack)));
        first.tick();
        List<ProtocolPayloads.Snapshot> afterAttack = drainSnapshots(first.drainFor("p1"), codec);
        boolean removedSeen = hasObject(afterAttack, "block", true);

        first.tick();
        first.tick();
        first.tick(); // PLACE cooldown window
        ProtocolPayloads.ActionRequest place2 = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.PLACE, true, target2[0], target2[1], "fence");
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

    private static double[] findPlayer(List<ProtocolPayloads.Snapshot> snapshots, String playerId) {
        double[] out = new double[] { 0.0, 0.0 };
        if (snapshots == null) return out;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            if (snapshot == null) continue;
            for (ProtocolPayloads.PlayerState ps : snapshot.players) {
                if (!playerId.equals(ps.playerId)) continue;
                out[0] = ps.worldX;
                out[1] = ps.worldY;
            }
        }
        return out;
    }

    private static double[] findNearbyLand(ServerTerrainRules terrain, double nearX, double nearY, double maxRadius) {
        for (int radius = 0; radius <= (int) maxRadius; radius += 16) {
            for (int dx = -radius; dx <= radius; dx += 16) {
                for (int dy = -radius; dy <= radius; dy += 16) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dy) < radius) continue;
                    double x = nearX + dx;
                    double y = nearY + dy;
                    if (!terrain.isWaterAt(x, y)) return new double[] { x, y };
                }
            }
        }
        return null;
    }
}
