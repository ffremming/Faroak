package resources.domain.inventory;

import java.util.Random;

/**
 * One entry in a {@link DropTable}: which item, how many. Quantity is a closed
 * range — each harvest roll picks a uniform value in [min, max].
 *
 * Value object; share freely.
 */
public final class DropSpec {

    private final String itemName;
    private final int    minQty;
    private final int    maxQty;

    public DropSpec(String itemName, int minQty, int maxQty) {
        if (minQty < 0 || maxQty < minQty) {
            throw new IllegalArgumentException("invalid qty range " + minQty + ".." + maxQty);
        }
        this.itemName = itemName;
        this.minQty   = minQty;
        this.maxQty   = maxQty;
    }

    public DropSpec(String itemName, int fixedQty) { this(itemName, fixedQty, fixedQty); }

    public String itemName() { return itemName; }

    /** Pick a quantity in [minQty, maxQty] inclusive. */
    public int rollQuantity(Random rng) {
        return minQty == maxQty ? minQty : minQty + rng.nextInt(maxQty - minQty + 1);
    }
}
