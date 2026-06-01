package resources.testing.probes;

import java.util.ArrayList;
import java.util.List;

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

/** Verifies server-owned farming lifecycle: plant, grow, harvest, inventory update. */
public final class AuthoritativeFarmingGameplayProbe implements Probe {

    private static final int HOTBAR_OFFSET = 27;
    private static final int SLOT_HOE = HOTBAR_OFFSET;
    private static final int SLOT_WATERING_CAN = HOTBAR_OFFSET + 1;
    private static final int SLOT_SEEDS_WHEAT = HOTBAR_OFFSET + 2;
    private static final int EMPTY_SLOT = 11;

    @Override public String name() { return "mp-authoritative-farming-gameplay"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 30, 1, 120, 2.0, 4096.0, 8192.0, "test.db");
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
        ServerTerrainRules terrain = new ServerTerrainRules();
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
        try {
            join(lobby, codec, "p1");
            List<ProtocolPayloads.Snapshot> initial = snapshots(lobby.drainFor("p1"), codec);
            double[] spawn = player(initial, "p1");
            double[] land = nearbyLand(terrain, spawn[0] + 128.0, spawn[1], 512.0);
            if (land == null) return ProbeResult.skip(name() + " no nearby land");

            long seq = 1L;
            Step till = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.useEquippedAt(land[0], land[1], "hoe", SLOT_HOE));
            if (!till.accepted) return fail("till rejected", till.reason);

            Step water = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.useEquippedAt(land[0], land[1], "watering_can", SLOT_WATERING_CAN));
            if (!water.accepted) return fail("water rejected", water.reason);

            Step plant = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.useEquippedAt(land[0], land[1], "seeds_wheat", SLOT_SEEDS_WHEAT));
            if (!plant.accepted) return fail("plant rejected", plant.reason);
            ProtocolPayloads.InventoryStatePayload plantedInv = playerInventory(plant.snapshots, "p1");
            if (amountAt(plantedInv, SLOT_SEEDS_WHEAT) != 15) {
                return fail("seed was not consumed", "amount=" + amountAt(plantedInv, SLOT_SEEDS_WHEAT));
            }

            for (int i = 0; i < 480; i++) lobby.tick();
            List<ProtocolPayloads.Snapshot> grown = snapshots(lobby.drainFor("p1"), codec);
            ProtocolPayloads.TileMutationPayload mature = latestFarmTile(grown);
            if (mature == null || mature.cropStage < 3 || !"crop_wheat".equals(mature.cropType)) {
                return fail("crop did not mature", mature == null ? "missing tile" : mature.cropType + " stage " + mature.cropStage);
            }

            Step harvest = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.useEquippedAt(land[0], land[1], "", EMPTY_SLOT));
            if (!harvest.accepted) return fail("harvest rejected", harvest.reason);
            ProtocolPayloads.TileMutationPayload cleared = latestFarmTile(harvest.snapshots);
            if (cleared == null || !cleared.cropType.isBlank()) {
                return fail("crop was not cleared after harvest", cleared == null ? "missing tile" : cleared.cropType);
            }
            ProtocolPayloads.InventoryStatePayload harvestedInv = playerInventory(harvest.snapshots, "p1");
            if (totalItem(harvestedInv, "wheat") < 1) {
                return fail("harvest produce missing from inventory");
            }

            return ProbeResult.pass(name(), "farming lifecycle replicated");
        } finally {
            lobby.close();
        }
    }

    private static void join(LobbyRuntime lobby, ProtocolPayloadCodec codec, String playerId) {
        lobby.receive(new ProtocolEnvelope(1, playerId, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
        lobby.tick();
    }

    private static Step command(LobbyRuntime lobby, ProtocolPayloadCodec codec, long seq, ProtocolPayloads.CommandRequest command) {
        lobby.receive(new ProtocolEnvelope(1, "p1", seq, 0L, 0L, ProtocolMessageType.COMMAND, codec.encodeCommand(command)));
        lobby.tick();
        List<ProtocolEnvelope> envelopes = lobby.drainFor("p1");
        ProtocolPayloads.CommandResult result = null;
        for (ProtocolEnvelope envelope : envelopes) {
            if (ProtocolMessageType.COMMAND_RESULT.equals(envelope.messageType())) {
                result = codec.decodeCommandResult(envelope.payload());
            }
        }
        return new Step(result != null && result.accepted, result == null ? "missing result" : result.reason, snapshots(envelopes, codec));
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
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.PlayerState player : snapshot.players) {
                if (playerId.equals(player.playerId)) return new double[] { player.worldX, player.worldY };
            }
        }
        return new double[] { 0.0, 0.0 };
    }

    private static ProtocolPayloads.InventoryStatePayload playerInventory(List<ProtocolPayloads.Snapshot> snapshots, String playerId) {
        String type = "player:" + playerId;
        ProtocolPayloads.InventoryStatePayload found = null;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.InventoryStatePayload inventory : snapshot.inventories) {
                if (type.equals(inventory.inventoryType)) found = inventory;
            }
        }
        return found;
    }

    private static ProtocolPayloads.TileMutationPayload latestFarmTile(List<ProtocolPayloads.Snapshot> snapshots) {
        ProtocolPayloads.TileMutationPayload found = null;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.TileMutationPayload tile : snapshot.tileMutations) {
                if (tile.tileType.startsWith("farmland")) found = tile;
            }
        }
        return found;
    }

    private static int amountAt(ProtocolPayloads.InventoryStatePayload inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.slots.size()) return -1;
        return inventory.slots.get(slot).amount;
    }

    private static int totalItem(ProtocolPayloads.InventoryStatePayload inventory, String itemType) {
        if (inventory == null) return 0;
        int total = 0;
        for (ProtocolPayloads.ItemStackPayload slot : inventory.slots) {
            if (itemType.equals(slot.itemType)) total += slot.amount;
        }
        return total;
    }

    private static double[] nearbyLand(ServerTerrainRules terrain, double nearX, double nearY, double maxRadius) {
        for (int radius = 0; radius <= (int) maxRadius; radius += 32) {
            for (int dx = -radius; dx <= radius; dx += 32) {
                for (int dy = -radius; dy <= radius; dy += 32) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dy) < radius) continue;
                    double x = nearX + dx;
                    double y = nearY + dy;
                    if (!terrain.isWaterAt(x, y)) return new double[] { x, y };
                }
            }
        }
        return null;
    }

    private static ProbeResult fail(String headline) {
        return ProbeResult.fail("mp-authoritative-farming-gameplay " + headline);
    }

    private static ProbeResult fail(String headline, String detail) {
        return ProbeResult.fail("mp-authoritative-farming-gameplay " + headline, detail);
    }

    private static final class Step {
        final boolean accepted;
        final String reason;
        final List<ProtocolPayloads.Snapshot> snapshots;

        Step(boolean accepted, String reason, List<ProtocolPayloads.Snapshot> snapshots) {
            this.accepted = accepted;
            this.reason = reason == null ? "" : reason;
            this.snapshots = snapshots;
        }
    }
}
