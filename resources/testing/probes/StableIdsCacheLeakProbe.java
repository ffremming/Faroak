package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Regression for "stable-ids-signature-cache-leak": when an entity leaves the world the
 * delta path must (a) emit a removal payload, (b) drop its lastEntitySig cache entry, and
 * (c) call StableEntityIds.forget() so the registry stops holding a strong reference to the
 * dead engine instance and can reuse the id.
 *
 * <p>Asserts via the hostauth path (EngineSnapshotBuilder + StableEntityIds).
 *
 * <p>Before the fix: ids.forget() was never invoked from the lobby/builder, so after a
 * removed entity is reported the registry still resolves its id to the dead instance
 * (entityFor(id) != null) and byEntity keeps growing -> this probe FAILS.
 * After the fix: entityFor(id) == null for the removed entity -> this probe PASSES.
 *
 * Run: java -cp out resources.testing.probes.StableIdsCacheLeakProbe
 */
public final class StableIdsCacheLeakProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        StableEntityIds ids = new StableEntityIds();
        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, ids);

        // Baseline seeds the id registry and the signature cache for every present entity.
        builder.buildBaseline(0L);

        // Pick a non-player entity, capture its assigned stable id.
        Entity victim = null;
        for (Entity e : panel.world().getEntities()) {
            if (e != null && e != panel.player()) { victim = e; break; }
        }
        if (victim == null) { fail(panel, "no entity to remove"); return; }
        long victimId = ids.idFor(victim);
        if (ids.entityFor(victimId) != victim) { fail(panel, "registry did not resolve victim before removal"); return; }

        // Remove the entity from the authoritative world via the deferred removal queue,
        // then update so the queue drains and getEntities() no longer reports it.
        panel.world().addToRemovalQueue(victim);
        for (int i = 0; i < 3; i++) panel.update(1.0);
        boolean gone = true;
        for (Entity e : panel.world().getEntities()) {
            if (e == victim) { gone = false; break; }
        }
        if (!gone) { fail(panel, "victim still present after removal-queue drain"); return; }

        ProtocolPayloads.Snapshot delta = builder.buildDelta(0L);

        // (a) the delta must report the removal as a tombstone.
        boolean reportedRemoval = false;
        for (ProtocolPayloads.EntityStatePayload p : delta.entities) {
            if (p.entityId == victimId && p.removed) { reportedRemoval = true; break; }
        }
        // (c) the registry must have forgotten the dead instance (no strong ref, id freeable).
        Object stillKnown = ids.entityFor(victimId);

        System.out.println("[StableIdsCacheLeak] victimId=" + victimId
            + " reportedRemoval=" + reportedRemoval
            + " entityForAfter=" + (stillKnown == null ? "null" : "PRESENT"));

        boolean ok = reportedRemoval && stillKnown == null;
        if (!ok) { fail(panel, "removed entity still retained by StableEntityIds (leak)"); return; }

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
