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

    public static final ItemType AXE          = define("axe",          "axe",          1,  null);
    public static final ItemType HAMMER       = define("hammer",       "hammer",       1,  null);
    public static final ItemType BLOCK        = define("block",        "block",        64, "block");
    public static final ItemType EMPTY        = define("empty",        "empty",        1,  null);

    // Phase 4 tools
    public static final ItemType HOE          = define("hoe",          "hoe",          1,  null);
    public static final ItemType WATERING_CAN = define("watering_can", "watering_can", 1,  null);
    public static final ItemType SHOVEL       = define("shovel",       "shovel",       1,  null);
    public static final ItemType SWORD        = define("sword",        "sword",        1,  null);
    public static final ItemType PICKAXE      = define("pickaxe",      "pickaxe",      1,  null);

    // Placeables
    public static final ItemType TORCH        = define("torch",        "torch",        16, "torch");
    public static final ItemType FENCE        = define("fence",        "fence",        64, "fence");
    public static final ItemType STONE_WALL   = define("stone_wall",   "stone_wall",   64, "stone_wall");
    public static final ItemType BARREL       = define("barrel",       "barrel",       16, "barrel");
    public static final ItemType CHEST        = define("chest",        "chest",        16, "chest");
    public static final ItemType FARMLAND     = define("farmland",     "farmland",     16, "farmland");
    public static final ItemType BOAT         = define("boat",         "boat",         1,  "boat");
    public static final ItemType CRAFTING_TABLE = define("crafting_table", "crafting_table", 16, "crafting_table");

    // Seeds
    public static final ItemType SEEDS_WHEAT  = define("seeds_wheat",  "seeds_wheat",  64, "crop_wheat");
    public static final ItemType SEEDS_CARROT = define("seeds_carrot", "seeds_carrot", 64, "crop_carrot");

    // Fantasy crop seeds (see FARM_CROPS.md)
    public static final ItemType SEEDS_EMBERWHEAT = define("seeds_emberwheat", "seeds_emberwheat", 64, "crop_emberwheat");
    public static final ItemType SEEDS_FROSTBLOOM = define("seeds_frostbloom", "seeds_frostbloom", 64, "crop_frostbloom");
    public static final ItemType SEEDS_GLOWCAP    = define("seeds_glowcap",    "seeds_glowcap",    64, "crop_glowcap");
    public static final ItemType SEEDS_MANABERRY  = define("seeds_manaberry",  "seeds_manaberry",  64, "crop_manaberry");
    public static final ItemType SEEDS_IRONVINE   = define("seeds_ironvine",   "seeds_ironvine",   64, "crop_ironvine");
    public static final ItemType SEEDS_SUNGOURD   = define("seeds_sungourd",   "seeds_sungourd",   64, "crop_sungourd");
    public static final ItemType SEEDS_BLOODROOT  = define("seeds_bloodroot",  "seeds_bloodroot",  64, "crop_bloodroot");
    public static final ItemType SEEDS_STARDROP   = define("seeds_stardrop",   "seeds_stardrop",   64, "crop_stardrop");

    // Harvest products
    public static final ItemType WHEAT        = define("wheat",        "wheat",        64, null);
    public static final ItemType CARROT       = define("carrot",       "carrot",       64, null);
    public static final ItemType EMBERWHEAT   = define("emberwheat",   "emberwheat",   64, null);
    public static final ItemType FROSTBLOOM   = define("frostbloom",   "frostbloom",   64, null);
    public static final ItemType GLOWCAP      = define("glowcap",      "glowcap",      64, null);
    public static final ItemType MANABERRY    = define("manaberry",    "manaberry",    64, null);
    public static final ItemType IRONVINE     = define("ironvine",     "ironvine",     64, null);
    public static final ItemType SUNGOURD     = define("sungourd",     "sungourd",     64, null);
    public static final ItemType BLOODROOT    = define("bloodroot",    "bloodroot",    64, null);
    public static final ItemType STARDROP     = define("stardrop",     "stardrop",     64, null);
    public static final ItemType STONE        = define("stone",        "stone",        64, null);
    public static final ItemType IRON_ORE     = define("iron_ore",     "iron_ore",     64, null);
    public static final ItemType CRYSTAL      = define("crystal",      "crystal",      64, null);
    public static final ItemType MUSHROOM     = define("mushroom",     "mushroom",     64, null);
    public static final ItemType BERRY        = define("berry",        "berry",        64, null);
    public static final ItemType MEAT         = define("meat",         "meat",         64, null);
    public static final ItemType HIDE         = define("hide",         "hide",         64, null);

    private ItemTypeRegistry() {}

    public static Registry<ItemType> instance() { return REG; }

    private static ItemType define(String name, String sprite, int maxStack, String placed) {
        Identifier id = Identifier.of(name);
        ItemType t = new ItemType(id, sprite, maxStack, placed);
        REG.register(id, t);
        return t;
    }
}
