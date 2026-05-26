package resources.domain.farming;

import java.util.HashMap;
import java.util.Map;

import resources.domain.entity.component.HarvestableComponent;
import resources.domain.inventory.DropSpec;
import resources.domain.inventory.DropTable;
import resources.domain.inventory.HarvestRegistry;

/**
 * Bootstraps farming-related entries in the shared {@link HarvestRegistry}.
 *
 * For each crop in {@link CropRegistry}, this registers a profile for the
 * mature-stage object name (e.g. {@code "crop_wheat_stage3"}) so any spawn
 * path that uses the EntityFactory-style lookup picks up the right tool +
 * drop table. Crops built directly via {@link Crop} also attach their own
 * components in the constructor — this registry is the parallel path for
 * factory/generation flows.
 *
 * Call {@link #init()} once at bootstrap (e.g. from GamePanel setup).
 */
public final class FarmingRegistry {

    private static final int MATURE_DURABILITY = 1;
    private static final int MATURE_STAGE      = 3; // matches Crop.STAGES - 1
    private static boolean initialised;

    private static final Map<String, HarvestableComponent> CROP_HARVESTS = new HashMap<>();

    /** Idempotent: safe to call many times. */
    public static void init() {
        if (initialised) return;
        initialised = true;

        for (Map.Entry<String, CropRegistry.Entry> e : CropRegistry.all().entrySet()) {
            String cropName    = e.getKey();
            CropRegistry.Entry cfg = e.getValue();
            String matureName  = cropName + "_stage" + MATURE_STAGE;
            DropTable table    = DropTable.of(new DropSpec(cfg.produceName, 1, 3));

            HarvestRegistry.register(matureName,
                new HarvestRegistry.Profile(cfg.requiredTool, MATURE_DURABILITY, table));

            CROP_HARVESTS.put(matureName,
                new HarvestableComponent(cfg.requiredTool, MATURE_DURABILITY, table));
        }
    }

    /** Look up a pre-built HarvestableComponent for a mature crop object name. */
    public static HarvestableComponent harvestFor(String matureCropName) {
        return CROP_HARVESTS.get(matureCropName);
    }

    private FarmingRegistry() {}
}
