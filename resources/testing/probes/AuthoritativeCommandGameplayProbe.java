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

/** Verifies the typed command path for inventory, placement, and boats. */
public final class AuthoritativeCommandGameplayProbe implements Probe {

    private static final int HOTBAR_OFFSET = 27;
    private static final int SLOT_CHEST = HOTBAR_OFFSET + 4;
    private static final int SLOT_BOAT = HOTBAR_OFFSET + 6;
    private static final int SLOT_TORCH = HOTBAR_OFFSET + 7;

    @Override public String name() { return "mp-authoritative-command-gameplay"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        MultiplayerConfig cfg = new MultiplayerConfig(
            MultiplayerMode.HOST, "loopback", "p1", 10, 30, 20, 1, 120, 2.0, 4096.0, 8192.0, "test.db");
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
            Step chestStep = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.useEquippedAt(land[0], land[1], "chest", SLOT_CHEST));
            if (!chestStep.accepted) return fail("chest placement rejected", chestStep.reason);
            ProtocolPayloads.InventoryStatePayload playerInv = playerInventory(chestStep.snapshots, "p1");
            if (amountAt(playerInv, SLOT_CHEST) != 2) return fail("chest item was not consumed", "amount=" + amountAt(playerInv, SLOT_CHEST));
            ProtocolPayloads.EntityStatePayload chest = entity(chestStep.snapshots, "chest");
            ProtocolPayloads.InventoryStatePayload chestInv = inventoryForOwner(chestStep.snapshots, chest.entityId);
            if (chestInv == null || chestInv.slots.size() != 27) return fail("chest inventory missing");

            Step pickTorch = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.inventoryClick(playerInv.inventoryId, SLOT_TORCH, 1));
            if (!pickTorch.accepted) return fail("player inventory click rejected", pickTorch.reason);
            Step dropTorch = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.inventoryClick(chestInv.inventoryId, 0, 1));
            if (!dropTorch.accepted) return fail("chest inventory click rejected", dropTorch.reason);
            ProtocolPayloads.InventoryStatePayload movedChestInv = inventoryForOwner(dropTorch.snapshots, chest.entityId);
            if (!"torch".equals(itemAt(movedChestInv, 0)) || amountAt(movedChestInv, 0) != 10) {
                return fail("torch did not move into shared chest", slotDetail(movedChestInv, 0));
            }

            double[] water = nearbyBoatWater(terrain, spawn[0], spawn[1], 4096.0);
            if (water == null) return ProbeResult.skip(name() + " no nearby boat water");
            Step boatPlace = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.useEquippedAt(water[0], water[1], "boat", SLOT_BOAT));
            if (!boatPlace.accepted) return fail("boat placement rejected", boatPlace.reason);
            ProtocolPayloads.EntityStatePayload boat = entity(boatPlace.snapshots, "boat");
            if (boat == null) return fail("boat entity missing after placement");
            ProtocolPayloads.InventoryStatePayload afterBoatInv = playerInventory(boatPlace.snapshots, "p1");
            if (amountAt(afterBoatInv, SLOT_BOAT) != 2) return fail("boat item was not consumed", "amount=" + amountAt(afterBoatInv, SLOT_BOAT));

            Step board = command(lobby, codec, seq++,
                ProtocolPayloads.CommandRequest.interactEntity(boat.entityId, boat.worldX, boat.worldY));
            if (!board.accepted) return fail("boat boarding rejected", board.reason);
            input(lobby, codec, seq++, "p1", false, false, false, true);
            for (int i = 0; i < 8; i++) lobby.tick();
            List<ProtocolPayloads.Snapshot> afterMove = snapshots(lobby.drainFor("p1"), codec);
            ProtocolPayloads.EntityStatePayload movedBoat = entity(afterMove, "boat");
            if (movedBoat == null || movedBoat.worldX <= boat.worldX) {
                return fail("ridden boat did not move on server", "before=" + boat.worldX + ", after=" + (movedBoat == null ? "missing" : movedBoat.worldX));
            }
            if (!terrain.canPlaceObject("boat", movedBoat.worldX, movedBoat.worldY)) {
                return fail("ridden boat moved off water", "x=" + movedBoat.worldX + ", y=" + movedBoat.worldY);
            }

            return ProbeResult.pass(name(), "commands, inventory, placement, boat movement");
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

    private static void input(LobbyRuntime lobby, ProtocolPayloadCodec codec, long seq, String playerId, boolean up, boolean left, boolean down, boolean right) {
        lobby.receive(new ProtocolEnvelope(1, playerId, seq, 0L, 0L, ProtocolMessageType.INPUT_STATE,
            codec.encodeInputState(new ProtocolPayloads.InputState(up, left, down, right))));
        lobby.tick();
        lobby.drainFor(playerId);
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
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.InventoryStatePayload inventory : snapshot.inventories) {
                if (type.equals(inventory.inventoryType)) return inventory;
            }
        }
        return null;
    }

    private static ProtocolPayloads.InventoryStatePayload inventoryForOwner(List<ProtocolPayloads.Snapshot> snapshots, long ownerId) {
        for (ProtocolPayloads.Snapshot snapshot : snapshots) {
            for (ProtocolPayloads.InventoryStatePayload inventory : snapshot.inventories) {
                if (inventory.ownerEntityId == ownerId) return inventory;
            }
        }
        return null;
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

    private static double[] nearbyBoatWater(ServerTerrainRules terrain, double nearX, double nearY, double maxRadius) {
        for (int radius = 0; radius <= (int) maxRadius; radius += 64) {
            for (int dx = -radius; dx <= radius; dx += 64) {
                for (int dy = -radius; dy <= radius; dy += 64) {
                    if (radius > 0 && Math.abs(dx) < radius && Math.abs(dy) < radius) continue;
                    double x = nearX + dx;
                    double y = nearY + dy;
                    if (terrain.canPlaceObject("boat", x, y)) return new double[] { x, y };
                }
            }
        }
        return null;
    }

    private static ProbeResult fail(String headline) {
        return ProbeResult.fail("mp-authoritative-command-gameplay " + headline);
    }

    private static ProbeResult fail(String headline, String detail) {
        return ProbeResult.fail("mp-authoritative-command-gameplay " + headline, detail);
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
