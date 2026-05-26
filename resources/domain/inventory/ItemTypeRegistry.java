package resources.domain.inventory;

import resources.core.id.Identifier;
import resources.core.registry.Registry;

/**
 * Central registry of {@link ItemType}s. Pre-seeded with a handful of canonical
 * items; gameplay (or mods, later) extend it by calling
 * {@link Registry#register(Identifier, Object)} on {@link #instance()}.
 */
public final class ItemTypeRegistry {

    private static final Registry<ItemType> REG = new Registry<>("item_type");

    public static final ItemType AXE     = define("axe",     "axe",     1,  null);
    public static final ItemType HAMMER  = define("hammer",  "hammer",  1,  null);
    public static final ItemType BLOCK   = define("block",   "block",   64, "block");
    public static final ItemType EMPTY   = define("empty",   "empty",   1,  null);

    private ItemTypeRegistry() {}

    public static Registry<ItemType> instance() { return REG; }

    private static ItemType define(String name, String sprite, int maxStack, String placed) {
        Identifier id = Identifier.of(name);
        ItemType t = new ItemType(id, sprite, maxStack, placed);
        REG.register(id, t);
        return t;
    }
}
