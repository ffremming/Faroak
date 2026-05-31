package resources.net.multiplayer.state;

/**
 * Serializable item stack state independent of UI/GamePanel objects.
 */
public final class ItemStackState {

    public static final ItemStackState EMPTY = new ItemStackState("empty", 0);

    private final String itemType;
    private final int amount;

    public ItemStackState(String itemType, int amount) {
        String safeType = itemType == null || itemType.isBlank() ? "empty" : itemType.trim().toLowerCase();
        int safeAmount = Math.max(0, amount);
        if (safeAmount == 0) safeType = "empty";
        this.itemType = safeType;
        this.amount = "empty".equals(safeType) ? 0 : safeAmount;
    }

    public String itemType() { return itemType; }
    public int amount() { return amount; }
    public boolean isEmpty() { return amount <= 0 || "empty".equals(itemType); }

    public ItemStackState withAmount(int newAmount) {
        return new ItemStackState(itemType, newAmount);
    }
}
