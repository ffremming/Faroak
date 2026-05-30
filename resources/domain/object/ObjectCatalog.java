package resources.domain.object;

import java.util.List;

import resources.app.GamePanel;
import resources.core.id.Identifier;
import resources.domain.inventory.ItemType;
import resources.domain.inventory.ItemTypeRegistry;
import resources.world.placement.PlacementAction;
import resources.world.placement.PlacementRegistry;
import resources.world.placement.PlacementSpec;
import resources.world.placement.PlacementSpec.SnapPolicy;
import resources.world.placement.SurfaceRule;

/**
 * Single source of truth for the objects sliced out of
 * resources/images/objects/_spritesheets/mega_spritesheet.png by
 * {@link resources.testing.tools.SpriteSheetSlicer}.
 *
 * 96 cells (7 rows). Names are best-effort, read from the sheet's category
 * captions; a handful were ambiguous and carry generic/suffixed names (e.g.
 * "crate_2", or "misc_*" where the name would collide with an existing tool
 * item). The slicer's CELLS table and this ENTRIES list are kept name-for-name
 * consistent.
 *
 * registerAll(panel) loops ENTRIES + EXTRA at boot, registering each as an
 * ItemType (if absent) and a placeable PlacementSpec. Collision is
 * smart-per-category via the per-entry `solid` flag.
 */
public final class ObjectCatalog {

    /** One object: its id, where its art lives, collision, footprint, stack. */
    public static final class Entry {
        public final String name;
        public final String category;
        public final boolean solid;
        public final int wTiles;
        public final int hTiles;
        public final int maxStack;

        public Entry(String name, String category, boolean solid, int wTiles, int hTiles, int maxStack) {
            this.name = name; this.category = category; this.solid = solid;
            this.wTiles = wTiles; this.hTiles = hTiles; this.maxStack = maxStack;
        }
    }

    private static final int TILE = 64;

    // Sliced misc objects, in sheet row-major order (14/14/14/14/13/13/14).
    public static final List<Entry> ENTRIES = List.of(
        new Entry("bush", "nature/misc", false, 1, 1, 16),
        new Entry("shrub", "nature/misc", false, 1, 1, 16),
        new Entry("fern", "nature/misc", false, 1, 1, 16),
        new Entry("tall_grass", "nature/misc", false, 1, 1, 16),
        new Entry("flower", "nature/misc", true, 1, 1, 8),
        new Entry("log", "nature/misc", true, 1, 1, 8),
        new Entry("wood_pile", "nature/misc", true, 1, 1, 8),
        new Entry("tree_stump", "nature/misc", true, 1, 1, 8),
        new Entry("driftwood", "nature/misc", true, 2, 1, 8),
        new Entry("rock", "nature/misc", true, 1, 1, 8),
        new Entry("stone_pile", "nature/misc", false, 1, 1, 16),
        new Entry("boulder", "nature/misc", true, 1, 1, 8),
        new Entry("ore_rock", "nature/misc", true, 1, 1, 8),
        new Entry("cactus", "nature/misc", true, 1, 1, 8),
        new Entry("crate", "structures/misc", true, 1, 1, 1),
        new Entry("misc_barrel", "structures/misc", true, 1, 1, 1),
        new Entry("basket", "structures/misc", true, 1, 1, 1),
        new Entry("clay_pot", "structures/misc", true, 1, 1, 1),
        new Entry("sack", "structures/misc", true, 1, 1, 1),
        new Entry("brick_wall", "structures/misc", true, 1, 1, 1),
        new Entry("plank_wall", "structures/misc", true, 1, 1, 1),
        new Entry("log_wall", "structures/misc", true, 1, 1, 1),
        new Entry("picket_fence", "structures/misc", true, 1, 1, 1),
        new Entry("stone_post", "structures/misc", true, 1, 1, 1),
        new Entry("signpost", "structures/misc", true, 1, 1, 1),
        new Entry("torii_gate", "structures/misc", true, 1, 1, 1),
        new Entry("totem_pole", "structures/misc", true, 1, 1, 1),
        new Entry("crate_2", "structures/misc", true, 1, 1, 1),
        new Entry("furnace", "structures/crafting", true, 1, 1, 1),
        new Entry("kiln", "structures/crafting", true, 1, 1, 1),
        new Entry("anvil", "structures/crafting", true, 1, 1, 1),
        new Entry("loom", "structures/crafting", true, 1, 1, 1),
        new Entry("spinning_wheel", "structures/crafting", true, 1, 1, 1),
        new Entry("forge", "structures/crafting", true, 1, 1, 1),
        new Entry("cauldron", "structures/crafting", true, 1, 1, 1),
        new Entry("workbench", "structures/crafting", true, 1, 1, 1),
        new Entry("table", "structures/crafting", true, 1, 1, 1),
        new Entry("stool", "structures/crafting", true, 1, 1, 1),
        new Entry("bookshelf", "structures/crafting", true, 1, 1, 1),
        new Entry("cabinet", "structures/crafting", true, 1, 1, 1),
        new Entry("barrel_rack", "structures/crafting", true, 1, 1, 1),
        new Entry("shelf", "structures/crafting", true, 1, 1, 1),
        new Entry("candle", "structures/lights", false, 1, 2, 16),
        new Entry("wall_torch", "structures/lights", false, 1, 2, 16),
        new Entry("lantern", "structures/lights", false, 1, 2, 16),
        new Entry("standing_lamp", "structures/lights", false, 1, 2, 16),
        new Entry("brazier", "structures/lights", false, 1, 2, 16),
        new Entry("hanging_lantern", "structures/lights", false, 1, 2, 16),
        new Entry("lamp_post", "structures/lights", false, 1, 2, 16),
        new Entry("red_banner", "structures/lights", false, 1, 2, 16),
        new Entry("blue_banner", "structures/lights", false, 1, 2, 16),
        new Entry("pennant", "structures/lights", false, 1, 2, 16),
        new Entry("wall_scroll", "structures/lights", false, 1, 2, 16),
        new Entry("hanging_sign", "structures/lights", false, 1, 2, 16),
        new Entry("chime", "structures/lights", false, 1, 2, 16),
        new Entry("mobile", "structures/lights", false, 1, 2, 16),
        new Entry("lettuce", "nature/food", false, 1, 1, 16),
        new Entry("cabbage", "nature/food", false, 1, 1, 16),
        new Entry("tomato", "nature/food", false, 1, 1, 16),
        new Entry("corn", "nature/food", false, 1, 1, 16),
        new Entry("market_stall", "nature/food", false, 2, 1, 16),
        new Entry("onion", "nature/food", false, 1, 1, 16),
        new Entry("garlic", "nature/food", false, 1, 1, 16),
        new Entry("misc_carrot", "nature/food", false, 1, 1, 16),
        new Entry("wheat_sheaf", "nature/food", false, 1, 1, 16),
        new Entry("pepper", "nature/food", false, 1, 1, 16),
        new Entry("eggplant", "nature/food", false, 1, 1, 16),
        new Entry("radish", "nature/food", false, 1, 1, 16),
        new Entry("turnip", "nature/food", false, 1, 1, 16),
        new Entry("misc_sword", "items/tools", false, 1, 1, 16),
        new Entry("misc_axe", "items/tools", false, 1, 1, 16),
        new Entry("misc_pickaxe", "items/tools", false, 1, 1, 16),
        new Entry("misc_hoe", "items/tools", false, 1, 1, 16),
        new Entry("scythe", "items/tools", false, 1, 1, 16),
        new Entry("misc_hammer", "items/tools", false, 1, 1, 16),
        new Entry("misc_shovel", "items/tools", false, 1, 1, 16),
        new Entry("fishing_rod", "items/tools", false, 1, 1, 16),
        new Entry("misc_watering_can", "items/tools", false, 1, 1, 16),
        new Entry("bucket", "items/tools", false, 1, 1, 16),
        new Entry("torch_item", "items/tools", false, 1, 1, 16),
        new Entry("key", "items/tools", false, 1, 1, 16),
        new Entry("gem", "items/tools", false, 1, 1, 16),
        new Entry("potion_red", "items/misc", false, 1, 1, 16),
        new Entry("potion_blue", "items/misc", false, 1, 1, 16),
        new Entry("misc_meat", "items/misc", false, 1, 1, 16),
        new Entry("misc_hide", "items/misc", false, 2, 1, 16),
        new Entry("gold_pile", "items/misc", false, 1, 1, 16),
        new Entry("cheese", "items/misc", false, 1, 1, 16),
        new Entry("bread", "items/misc", false, 1, 1, 16),
        new Entry("fish", "items/misc", false, 1, 1, 16),
        new Entry("egg", "items/misc", false, 1, 1, 16),
        new Entry("apple", "items/misc", false, 1, 1, 16),
        new Entry("misc_mushroom", "items/misc", false, 1, 1, 16),
        new Entry("herb", "items/misc", false, 1, 1, 16),
        new Entry("scroll", "items/misc", false, 1, 1, 16),
        new Entry("treasure_chest", "items/misc", false, 2, 1, 16)
    );

    /** Objects whose art is already extracted (not on the mega sheet, so the
     *  slicer ignores them). Registered alongside ENTRIES at boot. */
    public static final List<Entry> EXTRA = List.of(
        new Entry("door",        "structures/buildings", true,  1, 1, 8),
        new Entry("cave_portal", "structures/terrain",   false, 1, 1, 1),
        new Entry("oak_M",       "nature/plants/trees",  true,  2, 3, 1)
    );

    private ObjectCatalog() {}

    /**
     * Register every catalog entry as an ItemType (if absent) and a placeable
     * PlacementSpec. Called once at boot from ItemManager.
     */
    public static void registerAll(GamePanel panel) {
        for (Entry e : ENTRIES) registerOne(panel, e);
        for (Entry e : EXTRA)   registerOne(panel, e);
    }

    static void registerOne(GamePanel panel, Entry e) {
        Identifier id = Identifier.of(e.name);
        if (!ItemTypeRegistry.instance().contains(id)) {
            ItemTypeRegistry.instance().register(
                id, new ItemType(id, e.name, e.maxStack, e.name));
        }
        PlacementRegistry.register(new PlacementSpec(
            e.name,
            p -> new GameObject(p, e.name, 0, 0,
                    e.wTiles * TILE, e.hTiles * TILE,
                    e.wTiles * TILE, e.hTiles * TILE,
                    0, 0,
                    e.solid),
            SurfaceRule.NOT_WATER,
            SnapPolicy.TILE,
            PlacementAction.PLACE_ENTITY,
            null,
            true));
    }
}
