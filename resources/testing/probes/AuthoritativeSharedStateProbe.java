package resources.testing.probes;

import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.MultiplayerAction;
import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.MultiplayerMode;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.ServerTerrainRules;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.InMemoryPersistenceStore;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Exercises the new authoritative shared state sections: entity components,
 * container inventory state, tile mutations, and restart restore.
 */
public final class AuthoritativeSharedStateProbe implements Probe {

    @Override public String name() { return "mp-authoritative-shared-state"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 2.0, 512.0, 2048.0, "test.db");
        InMemoryPersistenceStore store = new InMemoryPersistenceStore();
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        ServerTerrainRules terrain = new ServerTerrainRules();

        LobbyRuntime first = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        first.receive(new ProtocolEnvelope(1, "p1", 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        first.tick();
        List<ProtocolPayloads.Snapshot> initial = snapshots(first.drainFor("p1"), codec);
        double[] spawn = player(initial, "p1");
        double[] land = nearbyLand(terrain, spawn[0] + 96.0, spawn[1], 384.0);
        if (land == null) {
            first.close();
            return ProbeResult.skip(name() + " no nearby land target");
        }

        long seq = 1L;
        place(first, codec, seq++, land[0], land[1], "chest");
        first.tick();
        waitCooldown(first);
        place(first, codec, seq++, land[0] + 64.0, land[1], "hoe");
        first.tick();
        waitCooldown(first);
        place(first, codec, seq++, land[0] + 64.0, land[1], "watering_can");
        first.tick();
        waitCooldown(first);
        place(first, codec, seq++, land[0] + 64.0, land[1], "seeds_wheat");
        first.tick();

        List<ProtocolPayloads.Snapshot> after = snapshots(first.drainFor("p1"), codec);
        boolean chestEntity = hasEntity(after, "chest");
        boolean chestInventory = hasInventory(after, "chest", 27);
        boolean farmTile = hasFarmTile(after, true, "crop_wheat");
        first.close();

        LobbyRuntime second = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        second.receive(new ProtocolEnvelope(1, "p1", 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        second.tick();
        List<ProtocolPayloads.Snapshot> restored = snapshots(second.drainFor("p1"), codec);
        second.close();

        boolean restoredEntity = hasEntity(restored, "chest");
        boolean restoredInventory = hasInventory(restored, "chest", 27);
        boolean restoredFarm = hasFarmTile(restored, true, "crop_wheat");

        String detail = "chestEntity=" + chestEntity
            + ", chestInventory=" + chestInventory
            + ", farmTile=" + farmTile
            + ", restoredEntity=" + restoredEntity
            + ", restoredInventory=" + restoredInventory
            + ", restoredFarm=" + restoredFarm;
        if (!chestEntity || !chestInventory || !farmTile
                || !restoredEntity || !restoredInventory || !restoredFarm) {
            return ProbeResult.fail(name() + " shared state mismatch", detail);
        }
        return ProbeResult.pass(name(), detail);
    }

    private static void place(LobbyRuntime lobby, ProtocolPayloadCodec codec, long seq, double x, double y, String type) {
        ProtocolPayloads.ActionRequest request = new ProtocolPayloads.ActionRequest(
            MultiplayerAction.PLACE, true, x, y, type);
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.ACTION, codec.encodeAction(request)));
    }

    private static void waitCooldown(LobbyRuntime lobby) {
        for (int i = 0; i < 4; i++) lobby.tick();
    }

    private static List<ProtocolPayloads.Snapshot> snapshots(List<ProtocolEnvelope> envelopes, ProtocolPayloadCodec codec) {
        ArrayList<ProtocolPayloads.Snapshot> out = new ArrayList<>();
        if (envelopes == null) return out;
        for (ProtocolEnvelope envelope : envelopes) {
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(envelope.messageType())
                    || ProtocolMessageType.DELTA_SNAPSHOT.equals(envelope.messageType())) {
                out.add(codec.decodeSnapshot(envelope.payload()));
            }
        }
        return out;
    }

    private static double[] player(List<ProtocolPayloads.Snapshot> snapshots, String playerId) {
        double[] out = new double[] { 0.0, 0.0 };
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.PlayerState player : snapshot.players) {
                if (playerId.equals(player.playerId)) {
                    out[0] = player.worldX;
                    out[1] = player.worldY;
                }
            }
        }
        return out;
    }

    private static boolean hasEntity(List<ProtocolPayloads.Snapshot> snapshots, String type) {
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.EntityStatePayload entity : snapshot.entities) {
                if (!entity.removed && type.equals(entity.entityType)) return true;
            }
        }
        return false;
    }

    private static boolean hasInventory(List<ProtocolPayloads.Snapshot> snapshots, String type, int slots) {
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.InventoryStatePayload inventory : snapshot.inventories) {
                if (type.equals(inventory.inventoryType) && inventory.slots.size() == slots) return true;
            }
        }
        return false;
    }

    private static boolean hasFarmTile(List<ProtocolPayloads.Snapshot> snapshots, boolean watered, String crop) {
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.TileMutationPayload tile : snapshot.tileMutations) {
                if (!tile.tileType.startsWith("farmland")) continue;
                if (watered != tile.watered) continue;
                if (crop.equals(tile.cropType)) return true;
            }
        }
        return false;
    }

    private static double[] nearbyLand(ServerTerrainRules terrain, double nearX, double nearY, double maxRadius) {
        for (int radius = 0; radius <= (int) maxRadius; radius += 16) {
            for (int dx = -radius; dx <= radius; dx += 16) {
                for (int dy = -radius; dy <= radius; dy += 16) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dy) < radius) continue;
                    double x = nearX + dx;
                    double y = nearY + dy;
                    if (!terrain.isWaterAt(x, y)) return new double[] { x, y };
                }
            }
        }
        return null;
    }
}
