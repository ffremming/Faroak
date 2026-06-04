package resources.testing.probes;

import javax.swing.JFrame;
import java.awt.Point;

import resources.app.GamePanel;
import resources.domain.crafting.CraftingGrid;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.domain.object.CraftingTable;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.codec.SnapshotCodec;

/**
 * Regression probe for "crafting-table-grid-not-replicated": a CraftingTable's 4x4 input
 * grid placed in the host engine must be serialized into the host-authoritative snapshot
 * as an InventoryStatePayload (type "crafting", owned by the table entity) so guests see
 * the host's actual grid contents instead of a default/empty grid.
 *
 * Before the fix EngineSnapshotBuilder.containerInventory() only handled Chest/Barrel, so
 * no inventory was emitted for the table and this probe FAILS. After the fix it PASSES.
 *
 * Run: java -cp out resources.testing.probes.RemoteCraftingGridProbe
 */
public final class RemoteCraftingGridProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        int px = (int) panel.player().getWorldX();
        int py = (int) panel.player().getWorldY();

        // Place a crafting table and put a recognizable item into a grid slot.
        CraftingTable table = new CraftingTable(panel, px - 8, py + 8);
        boolean placed = panel.world().placeEntity(table);
        // Refresh the entity index so the table is enumerable by getEntities() (the index is
        // rebuilt by world.update(point); placeEntity alone does not surface it for queries).
        panel.world().update(new Point(px, py));

        final int slot = 5;
        final String item = "wood";
        CraftingGrid grid = table.getService().grid();
        grid.setStack(slot, new Stack(panel, new Item(panel, item), 1));
        System.out.println("[Craft] placed=" + placed
            + " inIndex=" + panel.world().getEntities().contains(table)
            + " slot" + slot + "=" + grid.getStack(slot).getName());

        StableEntityIds ids = new StableEntityIds();
        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, ids);
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloads.Snapshot decoded =
            codec.decode(codec.encode(builder.buildBaseline(0L)));

        // Guest side: locate the crafting table's replicated inventory.
        long tableId = ids.idFor(table);
        ProtocolPayloads.InventoryStatePayload craftInv = null;
        for (ProtocolPayloads.InventoryStatePayload inv : decoded.inventories) {
            if (inv.ownerEntityId == tableId && "crafting".equals(inv.inventoryType)) {
                craftInv = inv;
                break;
            }
        }

        boolean replicated = craftInv != null;
        boolean slotMatches = replicated
            && craftInv.slots.size() > slot
            && item.equals(craftInv.slots.get(slot).itemType)
            && craftInv.slots.get(slot).amount >= 1;

        System.out.println("[Craft] replicated=" + replicated
            + (replicated ? " type=" + craftInv.inventoryType
                + " slots=" + craftInv.slots.size()
                + " slot" + slot + "=" + craftInv.slots.get(slot).itemType : ""));

        if (!replicated) {
            System.err.println("FAIL: crafting table grid inventory not replicated to snapshot");
            panel.stopGameThread(); System.exit(1);
        }
        if (!slotMatches) {
            System.err.println("FAIL: replicated grid slot does not match host grid contents");
            panel.stopGameThread(); System.exit(1);
        }

        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }
}
