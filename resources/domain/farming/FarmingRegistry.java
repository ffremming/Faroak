package resources.domain.farming;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import resources.domain.entity.component.HarvestableComponent;
import resources.domain.inventory.DropSpec;
import resources.domain.inventory.DropTable;
import resources.domain.inventory.HarvestRegistry;
import resources.presentation.image.ImageContainer;

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

    /**
     * Make {@link FarmTile}'s soil/watered sprites resolvable by the tile image
     * loader. The farmland art ships as an <em>object</em> sprite
     * (objects/.../soil/farmland/), so a {@link FarmTile} asking the tile loader
     * for "farmland" would otherwise fall back to a colour swatch. Pre-seed the
     * tile cache with the object art under the tile keys so no new assets are
     * needed. Idempotent — the loader only reads the cache on miss.
     */
    public static void registerTileSprites(ImageContainer images) {
        if (images == null) return;
        aliasObjectIntoTileCache(images, FarmTile.SOIL_SPRITE);
        aliasObjectIntoTileCache(images, FarmTile.WATERED_SPRITE);
    }

    private static void aliasObjectIntoTileCache(ImageContainer images, String name) {
        if (images.containsImage(name)) return; // already resolvable as a tile
        ArrayList<BufferedImage> objectStack = images.getObjectImages(name);
        if (objectStack == null || objectStack.isEmpty()) return;
        BufferedImage first = objectStack.get(0);
        if (first != null) images.images.put(name, first);
    }

    private FarmingRegistry() {}
}
