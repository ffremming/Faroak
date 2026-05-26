package resources.domain.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Ordered list of {@link DropSpec}s rolled together on a single harvest event.
 * Each spec produces a {@link Drop} (item name + quantity).
 *
 * Kept as a separate value object (rather than inlined on HarvestableComponent)
 * so the same table can be shared between many entities and so future
 * weighted-or-conditional drop logic can extend a single type.
 */
public final class DropTable {

    private final List<DropSpec> specs;

    public DropTable(List<DropSpec> specs) {
        this.specs = Collections.unmodifiableList(new ArrayList<>(specs));
    }

    public static DropTable of(DropSpec... specs) {
        List<DropSpec> list = new ArrayList<>(specs.length);
        for (DropSpec s : specs) list.add(s);
        return new DropTable(list);
    }

    public List<Drop> roll(Random rng) {
        List<Drop> out = new ArrayList<>(specs.size());
        for (DropSpec s : specs) {
            int q = s.rollQuantity(rng);
            if (q > 0) out.add(new Drop(s.itemName(), q));
        }
        return out;
    }

    public List<DropSpec> specs() { return specs; }

    /** Result of one roll: a concrete item-name + quantity pair. */
    public static final class Drop {
        public final String itemName;
        public final int    quantity;
        public Drop(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }
}
