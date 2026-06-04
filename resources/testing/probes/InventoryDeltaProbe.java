package resources.testing.probes;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Inventory delta filtering (follow-up to entity/tile delta filtering). The host's player
 * inventory must ride a baseline in full, be OMITTED from a delta while unchanged, and reappear
 * in a delta the moment a slot changes. Mirrors the real lobby path: player bags are passed to
 * the builder via the overloaded build*(ack, players, playerInventories) signature.
 *
 * Pre-fix result: FAIL — every delta re-sent the full player inventory (quietDelta inv count 1,
 * not 0), so a stationary host flooded guests with redundant bag copies each frame.
 * Post-fix result: PASS.
 *
 * Run: java -cp out resources.testing.probes.InventoryDeltaProbe
 */
public final class InventoryDeltaProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, new StableEntityIds());

        Inventory bag = panel.player().getInventory();
        if (bag == null) { fail(panel, "no player inventory"); return; }
        long bagId = builder.inventoryId(bag);

        // Baseline: full inventory present.
        ProtocolPayloads.Snapshot baseline = builder.buildBaseline(0L, players(), invList(builder, bag));
        boolean baseHasBag = containsInventory(baseline, bagId);

        // Quiet delta: inventory unchanged -> must be OMITTED.
        ProtocolPayloads.Snapshot quiet = builder.buildDelta(0L, null, invList(builder, bag));
        boolean quietHasBag = containsInventory(quiet, bagId);

        // Change a slot, then take another delta -> inventory must reappear.
        bag.setStack(0, new Stack(panel, new Item(panel, "wood"), 5));
        ProtocolPayloads.Snapshot afterChange = builder.buildDelta(0L, null, invList(builder, bag));
        boolean changeHasBag = containsInventory(afterChange, bagId);

        // A subsequent quiet delta omits it again.
        ProtocolPayloads.Snapshot quiet2 = builder.buildDelta(0L, null, invList(builder, bag));
        boolean quiet2HasBag = containsInventory(quiet2, bagId);

        System.out.println("[InventoryDelta] baselineHasBag=" + baseHasBag
            + " quietHasBag=" + quietHasBag + " afterChangeHasBag=" + changeHasBag
            + " quiet2HasBag=" + quiet2HasBag);

        boolean ok = baseHasBag && !quietHasBag && changeHasBag && !quiet2HasBag;
        if (!ok) { fail(panel, "inventory not delta-filtered"); return; }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static List<ProtocolPayloads.PlayerState> players() {
        return new ArrayList<>();
    }

    private static List<ProtocolPayloads.InventoryStatePayload> invList(
            EngineSnapshotBuilder builder, Inventory bag) {
        List<ProtocolPayloads.InventoryStatePayload> out = new ArrayList<>();
        out.add(builder.inventoryPayload(bag, builder.inventoryId(bag), 0L, "player:host"));
        return out;
    }

    private static boolean containsInventory(ProtocolPayloads.Snapshot snap, long inventoryId) {
        if (snap.inventories == null) return false;
        for (ProtocolPayloads.InventoryStatePayload inv : snap.inventories) {
            if (inv != null && inv.inventoryId == inventoryId) return true;
        }
        return false;
    }

    private static void fail(GamePanel panel, String why) {
        System.err.println("FAIL: " + why);
        panel.stopGameThread();
        System.exit(1);
    }
}
