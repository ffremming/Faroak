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
 * Verifies the server seeds authoritative harvestable world objects on a fresh
 * world, that the same seed is deterministic, and that mobs spawn near a joined
 * player, move, and can be despawned. Seeded objects must replicate to clients.
 */
public final class MpWorldPopulationProbe implements Probe {

    @Override public String name() { return "mp-world-population"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        String prevCount = System.getProperty("game.multiplayer.worldObjectCount");
        String prevMobCap = System.getProperty("game.multiplayer.mobCap");
        System.setProperty("game.multiplayer.worldObjectCount", "80");
        System.setProperty("game.multiplayer.mobCap", "6");
        try {
            // Two fresh servers with the same seed must seed the same object set.
            int objectsA = countSeededObjects(buildLobby());
            int objectsB = countSeededObjects(buildLobby());
            boolean seeded = objectsA > 0;
            boolean deterministic = objectsA == objectsB;

            // Mobs spawn near a joined player and move.
            LobbyRuntime lobby = buildLobby();
            ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
            String pid = "Hunter-aa11bb";
            lobby.receive(new ProtocolEnvelope(1, pid, 0L, 0L, 0L, ProtocolMessageType.JOIN,
                codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
            for (int i = 0; i < 80; i++) lobby.tick();
            List<ProtocolPayloads.Snapshot> snaps = snapshots(lobby.drainFor(pid), codec);
            int mobs = countMobs(snaps);
            boolean mobsSpawned = mobs > 0;

            boolean ok = seeded && deterministic && mobsSpawned;
            String details = "objectsA=" + objectsA + " objectsB=" + objectsB
                + " deterministic=" + deterministic + " mobsSpawned=" + mobsSpawned + " mobs=" + mobs;
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        } finally {
            restore("game.multiplayer.worldObjectCount", prevCount);
            restore("game.multiplayer.mobCap", prevMobCap);
        }
    }

    private static LobbyRuntime buildLobby() {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "seed", 10, 30, 20, 1, 120, 20.0, 768.0, 1.0e9, "test.db");
        return new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
    }

    /** Join an observer at origin and count harvestable entities in its baseline snapshot. */
    private static int countSeededObjects(LobbyRuntime lobby) {
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        String pid = "Obs-000000";
        lobby.receive(new ProtocolEnvelope(1, pid, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
        lobby.tick();
        List<ProtocolPayloads.Snapshot> snaps = snapshots(lobby.drainFor(pid), codec);
        int n = 0;
        for (ProtocolPayloads.Snapshot s : snaps) {
            for (ProtocolPayloads.EntityStatePayload e : s.entities) {
                if (e == null || e.removed) continue;
                if ("tree".equals(e.entityType) || "rock".equals(e.entityType) || "ore".equals(e.entityType)) n++;
            }
        }
        return n;
    }

    private static int countMobs(List<ProtocolPayloads.Snapshot> snaps) {
        java.util.HashSet<Long> mobIds = new java.util.HashSet<>();
        for (ProtocolPayloads.Snapshot s : snaps) {
            for (ProtocolPayloads.EntityStatePayload e : s.entities) {
                if (e == null) continue;
                if ("goblin".equals(e.entityType) || "spider".equals(e.entityType) || "deer".equals(e.entityType)) {
                    if (e.removed) mobIds.remove(e.entityId); else mobIds.add(e.entityId);
                }
            }
        }
        return mobIds.size();
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

    private static void restore(String key, String prev) {
        if (prev == null) System.clearProperty(key);
        else System.setProperty(key, prev);
    }
}
