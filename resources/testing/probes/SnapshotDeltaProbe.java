package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Phase 5.3: delta filtering. After a baseline, a quiet delta is empty; moving one entity
 * yields a delta containing exactly that entity.
 * Run: java -cp out resources.testing.probes.SnapshotDeltaProbe
 */
public final class SnapshotDeltaProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, new StableEntityIds());

        ProtocolPayloads.Snapshot baseline = builder.buildBaseline(0L);
        int baseCount = baseline.entities.size();

        ProtocolPayloads.Snapshot quiet = builder.buildDelta(0L);
        int quietCount = quiet.entities.size();

        // Move one non-player entity, then take another delta.
        Entity moved = null;
        for (Entity e : panel.world().getEntities()) {
            if (e != null && e != panel.player()) { moved = e; break; }
        }
        if (moved == null) { System.err.println("FAIL: no entity to move"); panel.stopGameThread(); System.exit(1); }
        moved.setWorldX(moved.getWorldX() + 64);
        moved.getHitBox().updateCoords();

        ProtocolPayloads.Snapshot afterMove = builder.buildDelta(0L);
        int afterCount = afterMove.entities.size();

        System.out.println("[Delta] baseline=" + baseCount + " quietDelta=" + quietCount
            + " afterMoveDelta=" + afterCount);

        boolean ok = baseCount > 0 && quietCount == 0 && afterCount == 1;
        if (!ok) { System.err.println("FAIL"); panel.stopGameThread(); System.exit(1); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }
}
