package resources.testing.probes;

import java.util.ArrayList;

import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/** Verifies optional v2 entity/inventory/tile snapshot sections round-trip. */
public final class ProtocolV2SnapshotProbe implements Probe {

    @Override public String name() { return "protocol-v2-snapshot"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();

        ArrayList<ProtocolPayloads.ComponentStatePayload> components = new ArrayList<>();
        components.add(new ProtocolPayloads.ComponentStatePayload("container", "chest"));
        ArrayList<ProtocolPayloads.EntityStatePayload> entities = new ArrayList<>();
        entities.add(new ProtocolPayloads.EntityStatePayload(
            7L, "chest", "core:overworld", 128.0, 256.0, false, 3L, components));

        ArrayList<ProtocolPayloads.ItemStackPayload> slots = new ArrayList<>();
        slots.add(new ProtocolPayloads.ItemStackPayload("stone", 12));
        slots.add(new ProtocolPayloads.ItemStackPayload("empty", 0));
        ArrayList<ProtocolPayloads.InventoryStatePayload> inventories = new ArrayList<>();
        inventories.add(new ProtocolPayloads.InventoryStatePayload(7L, 7L, "chest", 4L, slots));

        ArrayList<ProtocolPayloads.TileMutationPayload> tiles = new ArrayList<>();
        tiles.add(new ProtocolPayloads.TileMutationPayload(
            "core:overworld", 2, -3, "farmland_watered", true, "crop_wheat", 1, 5L));

        ProtocolPayloads.Snapshot source = new ProtocolPayloads.Snapshot(
            true, 22L, new ArrayList<>(), new ArrayList<>(), entities, inventories, tiles);
        ProtocolPayloads.Snapshot decoded = codec.decodeSnapshot(codec.encodeSnapshot(source));

        boolean ok = decoded.entities.size() == 1
            && decoded.inventories.size() == 1
            && decoded.tileMutations.size() == 1
            && "chest".equals(decoded.entities.get(0).entityType)
            && "container".equals(decoded.entities.get(0).components.get(0).key)
            && decoded.inventories.get(0).slots.size() == 2
            && "stone".equals(decoded.inventories.get(0).slots.get(0).itemType)
            && decoded.tileMutations.get(0).watered
            && "crop_wheat".equals(decoded.tileMutations.get(0).cropType);

        if (!ok) return ProbeResult.fail(name() + " v2 snapshot round-trip mismatch");
        return ProbeResult.pass(name(), "entities=1 inventories=1 tiles=1");
    }
}
