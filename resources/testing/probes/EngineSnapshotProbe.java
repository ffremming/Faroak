package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.codec.SnapshotCodec;

/**
 * Verifies EngineSnapshotBuilder turns the real (offline) engine world into a
 * Snapshot payload that (1) contains entities and (2) survives a codec round-trip
 * with type + center coordinates preserved.
 * Run: java -cp out resources.testing.probes.EngineSnapshotProbe
 */
public final class EngineSnapshotProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, new StableEntityIds());
        ProtocolPayloads.Snapshot snap = builder.buildBaseline(1L);

        int entities = snap.entities == null ? 0 : snap.entities.size();
        System.out.println("[Snap] entities=" + entities
            + " tiles=" + (snap.tileMutations == null ? 0 : snap.tileMutations.size())
            + " worldTime=" + snap.worldTimeTicks);
        if (entities == 0) { fail("no entities serialized"); }

        // Round-trip through the wire codec; assert the first entity is preserved.
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloads.Snapshot decoded = codec.decode(codec.encode(snap));
        int decodedCount = decoded.entities == null ? 0 : decoded.entities.size();
        if (decodedCount != entities) {
            fail("entity count changed across codec: " + entities + " -> " + decodedCount);
        }
        ProtocolPayloads.EntityStatePayload before = snap.entities.get(0);
        ProtocolPayloads.EntityStatePayload after = byId(decoded, before.entityId);
        if (after == null) { fail("entity " + before.entityId + " lost across codec"); }
        boolean typeOk = before.entityType.equals(after.entityType);
        boolean posOk = Math.abs(before.worldX - after.worldX) < 0.5
            && Math.abs(before.worldY - after.worldY) < 0.5;
        if (!typeOk || !posOk) {
            fail("entity field mismatch: type " + before.entityType + "->" + after.entityType
                + " pos (" + before.worldX + "," + before.worldY + ")->(" + after.worldX + "," + after.worldY + ")");
        }

        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static ProtocolPayloads.EntityStatePayload byId(ProtocolPayloads.Snapshot s, long id) {
        for (ProtocolPayloads.EntityStatePayload e : s.entities) if (e.entityId == id) return e;
        return null;
    }

    private static void fail(String msg) {
        System.err.println("FAIL: " + msg);
        System.exit(1);
    }
}
