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

/** Verifies server-owned crafting table input/output/cursor behavior. */
public final class AuthoritativeCraftingGameplayProbe implements Probe {

    private static final int SLOT_WHEAT = 11;
    private static final int SLOT_CRAFTING_TABLE = 13;
    private static final int SLOT_RESULT_DEST = 14;
    private static final int CRAFT_OUTPUT_SLOT = 16;

    @Override public String name() { return "mp-authoritative-crafting-gameplay"; }

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
            Step placeTable = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.useEquippedAt(land[0], land[1], "crafting_table", SLOT_CRAFTING_TABLE));
            if (!placeTable.accepted) return fail("crafting table placement rejected", placeTable.reason);
            ProtocolPayloads.EntityStatePayload table = entity(placeTable.snapshots, "crafting_table");
            if (table == null) return fail("crafting table entity missing");
            ProtocolPayloads.InventoryStatePayload tableInv = inventoryForOwner(placeTable.snapshots, table.entityId);
            ProtocolPayloads.InventoryStatePayload playerInv = playerInventory(placeTable.snapshots, "p1");
            if (tableInv == null || tableInv.slots.size() != 17) return fail("crafting inventory missing");

            Step pickWheat = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.inventoryClick(playerInv.inventoryId, SLOT_WHEAT, 1));
            if (!pickWheat.accepted) return fail("wheat pickup rejected", pickWheat.reason);
            Step dropOne = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.inventoryClick(tableInv.inventoryId, 0, 3));
            if (!dropOne.accepted) return fail("first wheat drop rejected", dropOne.reason);
            Step dropTwo = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.inventoryClick(tableInv.inventoryId, 1, 3));
            if (!dropTwo.accepted) return fail("second wheat drop rejected", dropTwo.reason);
            tableInv = inventoryForOwner(dropTwo.snapshots, table.entityId);
            Step putBackRest = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.inventoryClick(playerInv.inventoryId, SLOT_WHEAT, 1));
            if (!putBackRest.accepted) return fail("wheat return rejected", putBackRest.reason);

            if (!"fence".equals(itemAt(tableInv, CRAFT_OUTPUT_SLOT)) || amountAt(tableInv, CRAFT_OUTPUT_SLOT) != 2) {
                return fail("crafting output did not preview fence", slotDetail(tableInv, CRAFT_OUTPUT_SLOT));
            }

            Step craft = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.inventoryClick(tableInv.inventoryId, CRAFT_OUTPUT_SLOT, 1));
            if (!craft.accepted) return fail("output craft rejected", craft.reason);
            ProtocolPayloads.InventoryStatePayload cursor = cursorInventory(craft.snapshots, "p1");
            if (!"fence".equals(itemAt(cursor, 0)) || amountAt(cursor, 0) != 2) {
                return fail("crafted fence missing from cursor", slotDetail(cursor, 0));
            }
            tableInv = inventoryForOwner(craft.snapshots, table.entityId);
            if (!itemAt(tableInv, CRAFT_OUTPUT_SLOT).isBlank() && !"empty".equals(itemAt(tableInv, CRAFT_OUTPUT_SLOT))) {
                return fail("craft output did not clear", slotDetail(tableInv, CRAFT_OUTPUT_SLOT));
            }

            Step storeResult = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.inventoryClick(playerInv.inventoryId, SLOT_RESULT_DEST, 1));
            if (!storeResult.accepted) return fail("result store rejected", storeResult.reason);
            playerInv = playerInventory(storeResult.snapshots, "p1");
            if (!"fence".equals(itemAt(playerInv, SLOT_RESULT_DEST)) || amountAt(playerInv, SLOT_RESULT_DEST) != 2) {
                return fail("crafted fence not stored in player inventory", slotDetail(playerInv, SLOT_RESULT_DEST));
            }

            return ProbeResult.pass(name(), "crafting table output is authoritative");
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

    private static ProtocolPayloads.EntityStatePayload entity(List<ProtocolPayloads.Snapshot> snapshots, String type) {
        ProtocolPayloads.EntityStatePayload found = null;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.EntityStatePayload entity : snapshot.entities) {
                if (!entity.removed && type.equals(entity.entityType)) found = entity;
            }
        }
        return found;
    }

    private static ProtocolPayloads.InventoryStatePayload inventoryForOwner(List<ProtocolPayloads.Snapshot> snapshots, long ownerId) {
        ProtocolPayloads.InventoryStatePayload found = null;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.InventoryStatePayload inventory : snapshot.inventories) {
                if (inventory.ownerEntityId == ownerId) found = inventory;
            }
        }
        return found;
    }

    private static ProtocolPayloads.InventoryStatePayload playerInventory(List<ProtocolPayloads.Snapshot> snapshots, String playerId) {
        return inventoryByType(snapshots, "player:" + playerId);
    }

    private static ProtocolPayloads.InventoryStatePayload cursorInventory(List<ProtocolPayloads.Snapshot> snapshots, String playerId) {
        return inventoryByType(snapshots, "cursor:" + playerId);
    }

    private static ProtocolPayloads.InventoryStatePayload inventoryByType(List<ProtocolPayloads.Snapshot> snapshots, String type) {
        ProtocolPayloads.InventoryStatePayload found = null;
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.InventoryStatePayload inventory : snapshot.inventories) {
                if (type.equals(inventory.inventoryType)) found = inventory;
            }
        }
        return found;
    }

    private static String itemAt(ProtocolPayloads.InventoryStatePayload inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.slots.size()) return "";
        return inventory.slots.get(slot).itemType;
    }

    private static int amountAt(ProtocolPayloads.InventoryStatePayload inventory, int slot) {
        if (inventory == null || slot < 0 || slot >= inventory.slots.size()) return -1;
        return inventory.slots.get(slot).amount;
    }

    private static String slotDetail(ProtocolPayloads.InventoryStatePayload inventory, int slot) {
        return itemAt(inventory, slot) + " x" + amountAt(inventory, slot);
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
        return ProbeResult.fail("mp-authoritative-crafting-gameplay " + headline);
    }

    private static ProbeResult fail(String headline, String detail) {
        return ProbeResult.fail("mp-authoritative-crafting-gameplay " + headline, detail);
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
