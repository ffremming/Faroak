package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.net.multiplayer.hostauth.RemoteInputApplier;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Verifies a remote player's currently-equipped (selected hotbar) item is replicated.
 *
 * <p>Before the fix, {@code RemoteAvatar.toPayload()} emitted facing/moving/health/alive
 * but NOT the equipped item, so a guest looking at a peer could never tell what tool or
 * weapon that peer was holding. This probe drives the host-authoritative
 * {@link RemoteInputApplier}: it builds a guest's headless actor, puts items in the actor's
 * hotbar, switches the selected hotbar slot, and asserts the serialized
 * {@link ProtocolPayloads.PlayerState#equippedItem} tracks the selection each time.
 *
 * Run: java -cp out resources.testing.probes.RemoteEquippedItemProbe
 */
public final class RemoteEquippedItemProbe {

    public static void main(String[] args) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        double sx = panel.player().getWorldX();
        double sy = panel.player().getWorldY();

        RemoteInputApplier remotes = new RemoteInputApplier(panel);
        remotes.join("guest-A", sx, sy);

        // Building the actor happens lazily on first interaction. Route a harmless
        // interaction at the spawn point so the headless actor (with its inventory) exists.
        remotes.applyInteraction("guest-A", sx, sy);

        Inventory inv = remotes.actorInventory("guest-A");
        if (inv == null) { fail(panel, "guest actor inventory was not built"); return; }

        // Place a hammer in hotbar column 0 and a sword in column 1.
        int slotHammer = Inventory.HOTBAR_OFFSET + 0;
        int slotSword  = Inventory.HOTBAR_OFFSET + 1;
        inv.setStack(slotHammer, new Stack(panel, new Item(panel, "hammer"), 1));
        inv.setStack(slotSword,  new Stack(panel, new Item(panel, "sword"), 1));

        // (1) Equip the hammer (hotbar column 0).
        inv.setIndex(0);
        String equippedAfterHammer = equipped(remotes, "guest-A");

        // (2) Equip the sword (hotbar column 1).
        inv.setIndex(1);
        String equippedAfterSword = equipped(remotes, "guest-A");

        System.out.println("[Equipped] after select-hammer=" + equippedAfterHammer
            + " after select-sword=" + equippedAfterSword);

        boolean ok = "hammer".equals(equippedAfterHammer) && "sword".equals(equippedAfterSword);
        if (!ok) { fail(panel, "equipped item not replicated in PlayerState"); return; }

        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    /** The equipped item name in the guest's serialized PlayerState payload. */
    private static String equipped(RemoteInputApplier remotes, String playerId) {
        RemoteInputApplier.RemoteAvatar a = remotes.avatar(playerId);
        ProtocolPayloads.PlayerState p = a.toPayload();
        return p.equippedItem;
    }

    private static void fail(GamePanel panel, String why) {
        System.err.println("[Equipped] FAIL: " + why);
        System.err.println("FAIL");
        panel.stopGameThread();
        System.exit(1);
    }
}
