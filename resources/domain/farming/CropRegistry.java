package resources.domain.farming;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Static lookup from cropName -> (produceItemName, requiredTool).
 *
 * Lives apart from {@link resources.domain.inventory.HarvestRegistry} because
 * crop entries are dynamic (per-stage names) and produce-centric rather than
 * tool-centric. {@link FarmingRegistry} reads from this table when it
 * registers mature-stage harvest profiles, and {@link Crop} reads from it
 * during construction.
 */
public final class CropRegistry {

    private static final Map<String, Entry> ENTRIES = new HashMap<>();

    static {
        // null tool = any tool (including bare hands) works.
        register("crop_wheat",  new Entry("wheat",  null));
        register("crop_carrot", new Entry("carrot", null));

        // Fantasy crops (see FARM_CROPS.md). Sprites: crop_<name>_stage0..3
        // sliced from objects/_spritesheets/crops_spritesheet.png. Seed items
        // are added later; the planted-entity + harvest side is wired here.
        register("crop_emberwheat", new Entry("emberwheat", null));
        register("crop_frostbloom", new Entry("frostbloom", null));
        register("crop_glowcap",    new Entry("glowcap",    null));
        register("crop_manaberry",  new Entry("manaberry",  null));
        register("crop_ironvine",   new Entry("ironvine",   null));
        register("crop_sungourd",   new Entry("sungourd",   null));
        register("crop_bloodroot",  new Entry("bloodroot",  null));
        register("crop_stardrop",   new Entry("stardrop",   null));
    }

    public static void register(String cropName, Entry entry) {
        ENTRIES.put(cropName, entry);
    }

    public static Entry get(String cropName) {
        return ENTRIES.get(cropName);
    }

    public static Map<String, Entry> all() {
        return Collections.unmodifiableMap(ENTRIES);
    }

    /** Crop config — produce item dropped on harvest + required tool name. */
    public static final class Entry {
        public final String produceName;
        public final String requiredTool;

        public Entry(String produceName, String requiredTool) {
            this.produceName  = produceName;
            this.requiredTool = requiredTool;
        }
    }

    private CropRegistry() {}
}
