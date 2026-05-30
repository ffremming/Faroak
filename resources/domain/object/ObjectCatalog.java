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
 * resources/images/objects/_spritesheets/mega_spritesheet.png.
 *
 * The ENTRIES list is consumed twice:
 *   1. The offline SpriteSheetSlicer zips its row-major blobs against this list
 *      (by index) to name each cut PNG.
 *   2. registerAll(panel) loops it at boot to register each object as an
 *      ItemType + a placeable PlacementSpec.
 *
 * Order is significant: it MUST match the visual top-to-bottom, left-to-right
 * order of the sprites on the sheet, or the slicer will name them wrong.
 */
public final class ObjectCatalog {

    /** One object: its id, where its art lives, collision, footprint, stack. */
    public static final class Entry {
        public final String name;        // item + object id, e.g. "well"
        public final String category;    // folder under images/objects/, e.g. "structures/decor"
        public final boolean solid;      // true = blocks movement
        public final int wTiles;         // footprint width in tiles
        public final int hTiles;         // footprint height in tiles
        public final int maxStack;       // inventory stack cap

        public Entry(String name, String category, boolean solid, int wTiles, int hTiles, int maxStack) {
            this.name = name; this.category = category; this.solid = solid;
            this.wTiles = wTiles; this.hTiles = hTiles; this.maxStack = maxStack;
        }
    }

    private static final int TILE = 64;

    // ---- The catalog. Edit this list as the sheet is read. Order = sheet order. ----
    // This is a first-draft naming; Task 2 reconciles count/order against the
    // slicer's actual blob output.
    public static final List<Entry> ENTRIES = List.of(
        // Nature / Terrain
        new Entry("berry_bush",   "nature/plants",        false, 1, 1, 1),
        new Entry("dead_bush",    "nature/plants",        false, 1, 1, 1),
        new Entry("tall_grass",   "nature/plants",        false, 1, 1, 1),
        new Entry("flower_patch", "nature/plants",        false, 1, 1, 1),
        new Entry("log",          "nature/wood",          true,  1, 1, 16),
        new Entry("stump",        "nature/wood",          true,  1, 1, 8),
        new Entry("boulder",      "nature/rocks",         true,  1, 1, 8),
        new Entry("rock_pile",    "nature/rocks",         false, 1, 1, 16),
        new Entry("cactus",       "nature/plants",        true,  1, 1, 4),
        // Building / Structures
        new Entry("wood_crate",   "structures/storage",   true,  1, 1, 16),
        new Entry("hay_bale",     "structures/storage",   true,  1, 1, 16),
        new Entry("clay_pot",     "structures/decor",     false, 1, 1, 16),
        new Entry("stone_block",  "structures/walls",     true,  1, 1, 64),
        new Entry("wood_planks",  "structures/walls",     true,  1, 1, 64),
        new Entry("signpost",     "structures/decor",     false, 1, 1, 8),
        new Entry("scarecrow",    "structures/decor",     true,  1, 1, 4),
        new Entry("well",         "structures/buildings", true,  2, 2, 1),
        new Entry("stone_statue", "structures/decor",     true,  1, 2, 1),
        // Crafting / Furniture
        new Entry("furnace",      "structures/crafting",  true,  1, 1, 1),
        new Entry("anvil",        "structures/crafting",  true,  1, 1, 1),
        new Entry("loom",         "structures/crafting",  true,  1, 1, 1),
        new Entry("cauldron",     "structures/crafting",  true,  1, 1, 1),
        new Entry("workbench",    "structures/crafting",  true,  1, 1, 1),
        new Entry("bookshelf",    "structures/furniture", true,  1, 1, 4),
        new Entry("wood_table",   "structures/furniture", true,  1, 1, 4),
        new Entry("wood_chair",   "structures/furniture", false, 1, 1, 8),
        // Lights / Decor
        new Entry("lantern",      "structures/lights",    false, 1, 1, 16),
        new Entry("standing_torch","structures/lights",   false, 1, 1, 16),
        new Entry("candle",       "structures/lights",    false, 1, 1, 16),
        new Entry("banner",       "structures/decor",     false, 1, 1, 8),
        new Entry("brazier",      "structures/lights",    true,  1, 1, 8),
        new Entry("fountain",     "structures/buildings", true,  2, 2, 1),
        new Entry("market_stall", "structures/buildings", true,  2, 1, 1),
        new Entry("tent",         "structures/buildings", true,  2, 2, 1),
        new Entry("windmill",     "structures/buildings", true,  2, 3, 1)
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
        for (Entry e : ENTRIES) {
            registerOne(panel, e);
        }
        for (Entry e : EXTRA) {
            registerOne(panel, e);
        }
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
                    e.wTiles * TILE, e.hTiles * TILE,   // sprite footprint
                    e.wTiles * TILE, e.hTiles * TILE,   // hitbox = footprint
                    0, 0,                                // no offset
                    e.solid),
            SurfaceRule.NOT_WATER,
            SnapPolicy.TILE,
            PlacementAction.PLACE_ENTITY,
            null,
            true));
    }
}
