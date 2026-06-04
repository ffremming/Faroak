package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.codec.SnapshotCodec;

/**
 * The host player's cursor (the item held on the mouse, {@code tempInHand}) is serialized into
 * the host-authoritative snapshot as a one-slot inventory keyed {@code "cursor:<playerId>"},
 * surviving the codec round-trip so guests can render what the host is holding.
 *
 * <p>Pre-fix this FAILS: {@code produceSnapshots()} never emitted a cursor inventory, so no
 * {@code cursor:host} typed inventory exists in the decoded snapshot. Post-fix it PASSES.
 *
 * <p>Asserts via the hostauth path: {@link EngineSnapshotBuilder#cursorPayload}, the real
 * snapshot codec, then the same lookup-by-type the client ({@code ReplicatedWorldState
 * .cursorForPlayer} -> {@code MultiplayerRuntime.toCursorStack}) performs.
 *
 * Run: java -cp out resources.testing.probes.RemoteCursorInventoryProbe
 */
public final class RemoteCursorInventoryProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        final String hostId = "host";

        // (1) Host holds a sword in its cursor.
        Stack held = new Stack(panel, new Item(panel, "wood_sword"), 1);
        panel.player().setTempInHand(held);
        System.out.println("[Cursor] host holds=" + (held.isEmpty() ? "empty" : held.getName()));

        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, new StableEntityIds());
        SnapshotCodec codec = new DefaultSnapshotCodec();

        // Build the cursor inventory exactly as HostAuthoritativeLobby.produceSnapshots() does.
        String cursorKey = "cursor:" + hostId.toLowerCase();
        java.util.List<ProtocolPayloads.PlayerState> noPeers = new java.util.ArrayList<>();
        java.util.List<ProtocolPayloads.InventoryStatePayload> invs = new java.util.ArrayList<>();
        invs.add(builder.cursorPayload(
            panel.player().getTempInHand(), builder.inventoryId(cursorKey.intern()), cursorKey));

        // (2) Guest receives the snapshot (codec round-trip).
        ProtocolPayloads.Snapshot decoded = codec.decode(codec.encode(
            builder.buildBaseline(0L, noPeers, invs)));

        // (3) Client-side lookup: find the cursor:host inventory and read slot 0, mirroring
        //     ReplicatedWorldState.cursorForPlayer + MultiplayerRuntime.toCursorStack.
        ProtocolPayloads.InventoryStatePayload cursorInv = null;
        for (ProtocolPayloads.InventoryStatePayload inv : decoded.inventories) {
            if (inv != null && cursorKey.equals(inv.inventoryType)) { cursorInv = inv; break; }
        }
        boolean found = cursorInv != null && !cursorInv.slots.isEmpty();
        String heldItem = found ? cursorInv.slots.get(0).itemType : "<none>";
        int heldAmount = found ? cursorInv.slots.get(0).amount : 0;
        boolean ok = found
            && !"empty".equals(heldItem)
            && heldAmount > 0
            && "wood_sword".equals(heldItem);
        System.out.println("[Cursor] replicated=" + (cursorInv != null)
            + " item=" + heldItem + " amount=" + heldAmount);

        if (!ok) {
            System.err.println("FAIL: host cursor (tempInHand) not replicated as cursor:host inventory");
            panel.stopGameThread();
            System.exit(1);
        }

        // (4) An empty cursor must still serialize (one "empty" slot) so guests can clear theirs.
        panel.player().setTempInHand(null);
        java.util.List<ProtocolPayloads.InventoryStatePayload> emptyInvs = new java.util.ArrayList<>();
        emptyInvs.add(builder.cursorPayload(
            panel.player().getTempInHand(), builder.inventoryId(cursorKey.intern()), cursorKey));
        ProtocolPayloads.Snapshot decodedEmpty = codec.decode(codec.encode(
            builder.buildBaseline(0L, noPeers, emptyInvs)));
        ProtocolPayloads.InventoryStatePayload emptyCursor = null;
        for (ProtocolPayloads.InventoryStatePayload inv : decodedEmpty.inventories) {
            if (inv != null && cursorKey.equals(inv.inventoryType)) { emptyCursor = inv; break; }
        }
        boolean emptyOk = emptyCursor != null && emptyCursor.slots.size() == 1
            && "empty".equals(emptyCursor.slots.get(0).itemType);
        System.out.println("[Cursor] emptyReplicated=" + (emptyCursor != null)
            + " slot0=" + (emptyCursor == null || emptyCursor.slots.isEmpty()
                ? "<none>" : emptyCursor.slots.get(0).itemType));
        if (!emptyOk) {
            System.err.println("FAIL: empty cursor not serialized as a single empty slot");
            panel.stopGameThread();
            System.exit(1);
        }

        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }
}
