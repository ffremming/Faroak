package resources.testing.probes;

import javax.swing.JFrame;
import java.awt.Point;

import resources.app.GamePanel;
import resources.domain.farming.FarmTile;
import resources.net.multiplayer.hostauth.EngineSnapshotBuilder;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.codec.SnapshotCodec;

/**
 * Phase 5.2: a farm tile tilled + planted in the host engine is serialized into the
 * snapshot as a TileMutationPayload (tileType/watered/cropType/cropStage), surviving the
 * codec round-trip so guests can render the crop.
 * Run: java -cp out resources.testing.probes.RemoteTileInventoryProbe
 */
public final class RemoteTileInventoryProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        // Till a tile near the player and plant wheat.
        double px = panel.player().getWorldX();
        double py = panel.player().getWorldY();
        FarmTile farm = null;
        outer:
        for (int r = 0; r <= 24; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    farm = panel.world().tillTileAt(new Point((int) px + dx * 64, (int) py + dy * 64));
                    if (farm != null) break outer;
                }
            }
        }
        if (farm == null) { System.err.println("FAIL: could not till any tile near player"); panel.stopGameThread(); System.exit(1); }
        boolean planted = farm.plant("wheat");
        // Refresh the tile index so the freshly tilled tile is enumerable by getTiles()
        // (the index is rebuilt by world.update(point), which the real game runs each
        // frame via the render path; the snapshot builder reads it the following frame).
        panel.world().update(new Point((int) farm.getWorldX(), (int) farm.getWorldY()));
        System.out.println("[Tile] tilled at (" + (int) farm.getWorldX() + "," + (int) farm.getWorldY()
            + ") planted=" + planted + " stage=" + (farm.crop() == null ? -1 : farm.crop().stage()));

        EngineSnapshotBuilder builder = new EngineSnapshotBuilder(panel, new StableEntityIds());
        ProtocolPayloads.Snapshot snap = builder.buildBaseline(0L);
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloads.Snapshot decoded = codec.decode(codec.encode(snap));

        int wantX = (int) Math.floor(farm.getWorldX() / 64.0);
        int wantY = (int) Math.floor(farm.getWorldY() / 64.0);
        ProtocolPayloads.TileMutationPayload found = null;
        for (ProtocolPayloads.TileMutationPayload t : decoded.tileMutations) {
            if (t.tileX == wantX && t.tileY == wantY) { found = t; break; }
        }
        boolean ok = found != null
            && found.tileType.startsWith("farmland")
            && (!planted || "wheat".equals(found.cropType));
        System.out.println("[Tile] replicated=" + (found != null)
            + (found != null ? " type=" + found.tileType + " crop=" + found.cropType + " stage=" + found.cropStage : ""));

        if (!ok) { System.err.println("FAIL: farm tile not replicated correctly"); panel.stopGameThread(); System.exit(1); }

        // --- inventory replication: host player inventory + any container inventory ---
        java.util.List<ProtocolPayloads.PlayerState> noPeers = new java.util.ArrayList<>();
        java.util.List<ProtocolPayloads.InventoryStatePayload> playerInvs = new java.util.ArrayList<>();
        playerInvs.add(builder.inventoryPayload(panel.player().getInventory(),
            builder.inventoryId(panel.player().getInventory()), 0L, "player:host"));
        ProtocolPayloads.Snapshot invSnap = codec.decode(codec.encode(
            builder.buildBaseline(0L, noPeers, playerInvs)));
        boolean hasPlayerInv = false, hasContainerInv = false;
        for (ProtocolPayloads.InventoryStatePayload inv : invSnap.inventories) {
            if (inv.inventoryType.startsWith("player:")) hasPlayerInv = true;
            else if (inv.ownerEntityId > 0L) hasContainerInv = true;
        }
        System.out.println("[Inv] playerInv=" + hasPlayerInv + " containerInv=" + hasContainerInv
            + " totalInventories=" + invSnap.inventories.size());
        if (!hasPlayerInv) { System.err.println("FAIL: player inventory not replicated"); panel.stopGameThread(); System.exit(1); }

        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }
}
