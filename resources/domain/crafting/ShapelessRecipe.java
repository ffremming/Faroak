package resources.domain.crafting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import resources.app.GamePanel;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;

/**
 * Position-independent recipe: the grid matches as long as it contains
 * exactly the required item names in the required quantities, with no extras.
 *
 * Example: {@code new ShapelessRecipe("planks", "log", 1, panel, "planks", 4)}
 * — one "log" anywhere on the grid produces four "planks".
 *
 * Built via the static factory so callers can declare recipes fluently in
 * {@link RecipeRegistry}.
 */
public final class ShapelessRecipe implements Recipe {

    private final String id;
    private final Map<String, Integer> ingredients;
    private final String outputName;
    private final int    outputAmount;

    private ShapelessRecipe(String id,
                            Map<String, Integer> ingredients,
                            String outputName,
                            int outputAmount) {
        this.id           = id;
        this.ingredients  = Collections.unmodifiableMap(new HashMap<>(ingredients));
        this.outputName   = outputName;
        this.outputAmount = outputAmount;
    }

    public static Builder of(String id) { return new Builder(id); }

    @Override public String id() { return id; }

    @Override
    public boolean matches(CraftingGrid grid) {
        Map<String, Integer> bag = bagOf(grid);
        if (bag.size() != ingredients.size()) return false;
        for (Map.Entry<String, Integer> e : ingredients.entrySet()) {
            Integer have = bag.get(e.getKey());
            if (have == null || have < e.getValue()) return false;
        }
        // Reject extras: bag may not carry any name not in the ingredient set.
        for (String name : bag.keySet()) {
            if (!ingredients.containsKey(name)) return false;
        }
        return true;
    }

    @Override
    public Stack produce(CraftingGrid grid) {
        GamePanel panel = grid.getStack(0).getItem() != null
            ? grid.getStack(0).getItem().panel
            : null;
        // Fallback: scan for any non-empty slot to recover a panel reference.
        if (panel == null) {
            for (int i = 0; i < grid.getSize(); i++) {
                Stack s = grid.getStack(i);
                if (s != null && s.getItem() != null) {
                    panel = s.getItem().panel;
                    break;
                }
            }
        }
        Item out = new Item(panel, outputName);
        Stack result = new Stack(panel, out, 0);
        for (int i = 0; i < outputAmount; i++) result.addItem(new Item(panel, outputName));
        return result;
    }

    private static Map<String, Integer> bagOf(CraftingGrid grid) {
        Map<String, Integer> bag = new HashMap<>();
        for (int i = 0; i < grid.getSize(); i++) {
            Stack s = grid.getStack(i);
            if (s == null || s.isEmpty()) continue;
            String name = s.getName();
            if ("empty".equals(name)) continue;
            bag.merge(name, s.getAmount(), Integer::sum);
        }
        return bag;
    }

    public static final class Builder {
        private final String id;
        private final Map<String, Integer> ingredients = new HashMap<>();
        private String outputName;
        private int    outputAmount = 1;

        private Builder(String id) { this.id = id; }

        public Builder ingredient(String name, int amount) {
            ingredients.merge(name, amount, Integer::sum);
            return this;
        }
        public Builder ingredient(String name) { return ingredient(name, 1); }

        public Builder produces(String name, int amount) {
            this.outputName   = name;
            this.outputAmount = amount;
            return this;
        }
        public Builder produces(String name) { return produces(name, 1); }

        public ShapelessRecipe build() {
            if (outputName == null) throw new IllegalStateException("recipe " + id + " has no output");
            if (ingredients.isEmpty()) throw new IllegalStateException("recipe " + id + " has no ingredients");
            return new ShapelessRecipe(id, ingredients, outputName, outputAmount);
        }
    }
}
