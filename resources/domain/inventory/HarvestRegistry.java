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
