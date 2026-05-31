package resources.net.multiplayer.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Authoritative inventory/container state keyed by a stable owner id.
 */
public final class InventoryState {

    private final long inventoryId;
    private final long ownerEntityId;
    private final String inventoryType;
    private final ArrayList<ItemStackState> slots;
    private long revision;
    private long lastChangedTick;

    public InventoryState(long inventoryId, long ownerEntityId, String inventoryType, int slotCount, long revision, long tick) {
        this.inventoryId = Math.max(0L, inventoryId);
        this.ownerEntityId = Math.max(0L, ownerEntityId);
        this.inventoryType = inventoryType == null || inventoryType.isBlank() ? "generic" : inventoryType.trim().toLowerCase();
        this.slots = new ArrayList<>(Math.max(0, slotCount));
        for (int i = 0; i < Math.max(0, slotCount); i++) slots.add(ItemStackState.EMPTY);
        this.revision = Math.max(0L, revision);
        this.lastChangedTick = Math.max(0L, tick);
    }

    public long inventoryId() { return inventoryId; }
    public long ownerEntityId() { return ownerEntityId; }
    public String inventoryType() { return inventoryType; }
    public long revision() { return revision; }
    public long lastChangedTick() { return lastChangedTick; }
    public int slotCount() { return slots.size(); }

    public List<ItemStackState> slots() {
        return Collections.unmodifiableList(slots);
    }

    public ItemStackState slot(int index) {
        if (index < 0 || index >= slots.size()) return ItemStackState.EMPTY;
        ItemStackState stack = slots.get(index);
        return stack == null ? ItemStackState.EMPTY : stack;
    }

    public boolean setSlot(int index, ItemStackState stack, long newRevision, long tick) {
        if (index < 0 || index >= slots.size()) return false;
        slots.set(index, stack == null ? ItemStackState.EMPTY : stack);
        revision = Math.max(revision, newRevision);
        lastChangedTick = Math.max(0L, tick);
        return true;
    }

    public boolean changedSince(long sentTick) {
        return sentTick <= 0L || lastChangedTick > sentTick;
    }
}
