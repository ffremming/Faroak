package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Regression for finding "stale-entity-replicas-delta": when an entity is removed
 * from the host's world, the next DELTA snapshot must carry an EntityStatePayload
 * with removed=true so clients drop the stale replica. Before the fix the builder
 * iterated only live entities, so a vanished entity was never serialized on a delta
 * and the client kept it forever.
 *
 * Run: java -cp out resources.testing.probes.SnapshotEntityRemovalProbe
 */
public final class SnapshotEntityRemovalProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, new StableEntityIds());

        // Baseline seeds the builder's per-entity cache with everything present now.
        ProtocolPayloads.Snapshot baseline = builder.buildBaseline(0L);
        int baseCount = baseline.entities.size();

        // Pick a real non-player entity that the baseline serialized, capture its id,
        // then remove it from the world.
        BaseEntity victim = null;
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof BaseEntity && e != panel.player()) { victim = (BaseEntity) e; break; }
        }
        if (victim == null) { fail(panel, "no entity to remove"); return; }

        long victimId = -1L;
        double vcx = victim.getWorldX() + victim.getWidth() / 2.0;
        double vcy = victim.getWorldY() + victim.getHeight() / 2.0;
        for (ProtocolPayloads.EntityStatePayload p : baseline.entities) {
            if (Math.round(p.worldX) == Math.round(vcx) && Math.round(p.worldY) == Math.round(vcy)) {
                victimId = p.entityId; break;
            }
        }
        if (victimId < 0) { fail(panel, "victim id not found in baseline"); return; }

        // Use the canonical removal path (queue drained by simulate() during update),
        // which is how combat/harvest/farming remove entities in the real engine.
        panel.world().addToRemovalQueue(victim);
        for (int i = 0; i < 10; i++) panel.update(1.0);

        boolean stillPresent = false;
        for (Entity e : panel.world().getEntities()) {
            if (e == victim) { stillPresent = true; break; }
        }
        if (stillPresent) { fail(panel, "world still reports the removed entity"); return; }

        // The delta must contain a tombstone (removed=true) for exactly the victim id.
        ProtocolPayloads.Snapshot delta = builder.buildDelta(0L);
        boolean tombstoneSent = false;
        for (ProtocolPayloads.EntityStatePayload p : delta.entities) {
            if (p.entityId == victimId && p.removed) { tombstoneSent = true; break; }
        }

        // A subsequent quiet delta must NOT re-emit the tombstone (cache cleaned up).
        ProtocolPayloads.Snapshot quiet = builder.buildDelta(0L);
        boolean reEmitted = false;
        for (ProtocolPayloads.EntityStatePayload p : quiet.entities) {
            if (p.entityId == victimId) { reEmitted = true; break; }
        }

        System.out.println("[EntityRemoval] baseline=" + baseCount
            + " victimId=" + victimId
            + " tombstoneInDelta=" + tombstoneSent
            + " reEmittedAfter=" + reEmitted);

        boolean ok = baseCount > 0 && tombstoneSent && !reEmitted;
        if (!ok) { fail(panel, "expected one removed=true tombstone for the victim, not re-emitted"); return; }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static void fail(GamePanel panel, String why) {
        System.err.println("FAIL: " + why);
        panel.stopGameThread();
        System.exit(1);
    }
}
