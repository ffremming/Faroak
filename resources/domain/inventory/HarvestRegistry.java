package resources.domain.inventory;

import java.util.HashMap;
import java.util.Map;

import resources.domain.entity.component.HarvestableComponent;

/**
 * Static lookup from spawned object-name → harvest profile. Lets the
 * {@link resources.generation.factory.EntityFactory} attach a
 * {@link HarvestableComponent} based purely on what asset was placed,
 * without baking the gameplay mapping into the generator.
 *
 * Replace with a registry keyed by EntityType once that type carries its own
 * component bundle — until then this is the smallest viable plumbing.
 */
public final class HarvestRegistry {

    private static final Map<String, Profile> PROFILES = new HashMap<>();

    static {
        // Trees: chop with an axe, yield 2–4 wood (mock as "block").
        register("birch_M",  new Profile("axe", 3, DropTable.of(new DropSpec("block", 2, 4))));
        register("oak_M",    new Profile("axe", 4, DropTable.of(new DropSpec("block", 3, 5))));
        register("spruce_M", new Profile("axe", 3, DropTable.of(new DropSpec("block", 2, 4))));
        register("palm_M",   new Profile("axe", 3, DropTable.of(new DropSpec("block", 2, 4))));

        // Rocks: mine with a pickaxe, yield 1–3 stone.
        register("stone",    new Profile("pickaxe", 3, DropTable.of(new DropSpec("stone", 1, 3))));

        // Rock variant pack — pickaxe, durability scales with sprite size.
        // Pebble is hand-pickable like driftwood.
        register("rock_cluster_S",    new Profile(null,       1, DropTable.of(new DropSpec("stone", 1, 2))));
        register("rock_boulder_S",    new Profile("pickaxe",  2, DropTable.of(new DropSpec("stone", 1, 3))));
        register("rock_boulder_M",    new Profile("pickaxe",  3, DropTable.of(new DropSpec("stone", 2, 4))));
        register("rock_boulder_L",    new Profile("pickaxe",  5, DropTable.of(new DropSpec("stone", 3, 6))));
        register("rock_spire",        new Profile("pickaxe",  4, DropTable.of(new DropSpec("stone", 2, 4))));
        register("rock_mossy",        new Profile("pickaxe",  3, DropTable.of(new DropSpec("stone", 2, 4))));
        register("rock_cracked",      new Profile("pickaxe",  2, DropTable.of(new DropSpec("stone", 2, 5))));
        register("rock_river_smooth", new Profile("pickaxe",  3, DropTable.of(new DropSpec("stone", 1, 3))));

        // Mineral rocks — pickaxe, drop their named ore alongside a bit of stone.
        register("rock_iron_ore",     new Profile("pickaxe",  5, DropTable.of(new DropSpec("iron_ore", 1, 3), new DropSpec("stone", 1, 2))));
        register("rock_crystal",      new Profile("pickaxe",  5, DropTable.of(new DropSpec("crystal",  1, 2), new DropSpec("stone", 1, 2))));

        // Driftwood: light beach debris — one hit, no tool requirement, yields wood.
        register("driftwood", new Profile(null, 1, DropTable.of(new DropSpec("block", 1, 2))));

        // Hand-authored plant pack (sliced from plants_green.png).
        // Trees: axe, durability scales with sprite size, yield wood (block).
        register("plant_sapling",      new Profile("axe", 1, DropTable.of(new DropSpec("block", 1, 2))));
        register("plant_oak_round",    new Profile("axe", 3, DropTable.of(new DropSpec("block", 2, 4))));
        register("plant_conifer",      new Profile("axe", 4, DropTable.of(new DropSpec("block", 3, 5))));
        register("plant_oak_large",    new Profile("axe", 5, DropTable.of(new DropSpec("block", 4, 6))));
        register("plant_palm_tree",    new Profile("axe", 4, DropTable.of(new DropSpec("block", 3, 5))));
        register("plant_bonsai",       new Profile("axe", 6, DropTable.of(new DropSpec("block", 5, 8))));
        register("plant_willow_large", new Profile("axe", 7, DropTable.of(new DropSpec("block", 6, 9))));
        register("plant_oak_mega",     new Profile("axe", 8, DropTable.of(new DropSpec("block", 8, 12))));
        // Bushes & ground decor: hand-pickable, low yield.
        register("plant_bush_berry",   new Profile(null, 1, DropTable.of(new DropSpec("berry", 1, 3), new DropSpec("block", 1, 1))));
        register("plant_bush_leafy",   new Profile(null, 1, DropTable.of(new DropSpec("block", 1, 1))));
        register("plant_bush_small",   new Profile(null, 1, DropTable.of(new DropSpec("block", 1, 1))));
        // Mushrooms: hand-pickable, yield mushroom items.
        register("plant_mushroom_red",     new Profile(null, 1, DropTable.of(new DropSpec("mushroom", 1, 1))));
        register("plant_mushroom_tall",    new Profile(null, 1, DropTable.of(new DropSpec("mushroom", 1, 2))));
        register("plant_mushroom_cluster", new Profile(null, 1, DropTable.of(new DropSpec("mushroom", 2, 3))));
        // Logs: light wood pickups like driftwood.
        register("plant_log_short", new Profile(null, 1, DropTable.of(new DropSpec("block", 2, 3))));
        register("plant_log_long",  new Profile(null, 1, DropTable.of(new DropSpec("block", 3, 5))));
        // plant_clover, plant_fern, plant_grass_tuft, plant_palm_frond are
        // pure ground decor — no harvest profile, can't be picked up.
    }

    public static HarvestableComponent componentFor(String objectName) {
        Profile p = PROFILES.get(objectName);
        if (p == null) return null;
        return new HarvestableComponent(p.tool, p.durability, p.dropTable);
    }

    public static void register(String objectName, Profile profile) {
        PROFILES.put(objectName, profile);
    }

    public static final class Profile {
        public final String     tool;
        public final int        durability;
        public final DropTable  dropTable;
        public Profile(String tool, int durability, DropTable dropTable) {
            this.tool       = tool;
            this.durability = durability;
            this.dropTable  = dropTable;
        }
    }

    private HarvestRegistry() {}
}
