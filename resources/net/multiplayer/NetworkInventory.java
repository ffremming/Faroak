package resources.net.multiplayer;

import java.util.function.Supplier;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Read-only UI adapter over an authoritative replicated inventory snapshot.
 * Slot clicks are sent as commands; snapshots refresh the visual contents.
 */
public final class NetworkInventory extends Inventory {

    private final GamePanel panel;
    private final long inventoryId;
    private final Supplier<ProtocolPayloads.InventoryStatePayload> source;
    private long appliedRevision = -1L;
    private int appliedSlots = -1;

    public NetworkInventory(
            GamePanel panel,
            long inventoryId,
            Supplier<ProtocolPayloads.InventoryStatePayload> source) {
        super(panel);
        this.panel = panel;
        this.inventoryId = Math.max(0L, inventoryId);
        this.source = source;
    }

    public long inventoryId() {
        return inventoryId;
    }

    @Override
    public int getSize() {
        ProtocolPayloads.InventoryStatePayload payload = payload();
        return payload == null ? super.getSize() : payload.slots.size();
    }

    @Override
    public Stack getStack(int i) {
        refresh();
        if (i < 0 || i >= inventory.size()) return null;
        return inventory.get(i);
    }

    @Override
    public void setStack(int number, Stack stack) {
        // Authoritative inventories mutate only through network commands.
    }

    private ProtocolPayloads.InventoryStatePayload payload() {
        return source == null ? null : source.get();
    }

    private void refresh() {
        ProtocolPayloads.InventoryStatePayload payload = payload();
        if (payload == null) return;
        if (payload.revision == appliedRevision && payload.slots.size() == appliedSlots) return;
        ensureSlotCount(payload.slots.size());
        for (int i = 0; i < payload.slots.size(); i++) {
            ProtocolPayloads.ItemStackPayload slot = payload.slots.get(i);
            inventory.set(i, toStack(slot));
        }
        appliedRevision = payload.revision;
        appliedSlots = payload.slots.size();
    }

    private void ensureSlotCount(int size) {
        while (inventory.size() < size) inventory.add(new Stack(panel, "empty"));
        while (inventory.size() > size) inventory.remove(inventory.size() - 1);
    }

    private Stack toStack(ProtocolPayloads.ItemStackPayload slot) {
        if (slot == null || slot.amount <= 0 || "empty".equals(slot.itemType)) {
            return new Stack(panel, "empty");
        }
        return new Stack(panel, new Item(panel, slot.itemType), slot.amount);
    }
}
