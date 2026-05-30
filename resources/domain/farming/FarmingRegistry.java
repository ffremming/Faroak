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

    /** Highest crop stage index ({@code crop_<name>_stage<MATURE_STAGE>}). */
    private static final int MAX_STAGE = MATURE_STAGE;

    /**
     * Donor crop (which ships full stage art) lent to a legacy crop that has
     * none, so planting a legacy seed shows a sprite instead of a "?" swatch.
     */
    private static final Map<String, String> LEGACY_CROP_DONOR = buildLegacyCropDonors();

    private static Map<String, String> buildLegacyCropDonors() {
        Map<String, String> m = new HashMap<>();
        m.put("crop_wheat",  "crop_emberwheat"); // golden grain ≈ wheat
        m.put("crop_carrot", "crop_sungourd");   // orange gourd ≈ carrot
        return m;
    }

    /**
     * Wire up art for crops/produce that have no PNG of their own by aliasing to
     * existing sprites at load time (no new assets):
     *   - legacy {@code crop_wheat}/{@code crop_carrot} borrow a fantasy crop's
     *     per-stage art so planted seeds render through every growth stage, and
     *   - every crop's produce item ({@code wheat}, {@code emberwheat}, …) borrows
     *     its own mature-stage sprite so the harvest drop reads as that produce
     *     rather than the missing-art placeholder.
     *
     * The aliased names ship no PNG of their own, so the borrowed art is written
     * unconditionally (overwriting any placeholder cached by an earlier lookup).
     * Idempotent — re-running just re-points the same slots at the same donor.
     * Call once at bootstrap, after {@link #registerTileSprites}.
     */
    public static void registerArtAliases(ImageContainer images) {
        if (images == null) return;
        for (Map.Entry<String, String> e : LEGACY_CROP_DONOR.entrySet()) {
            aliasCropStages(images, e.getKey(), e.getValue());
        }
        for (CropRegistry.Entry cfg : CropRegistry.all().values()) {
            aliasProduceToMatureCrop(images, cfg.produceName);
        }
    }

    /** Point every {@code <legacy>_stage0..N} at the donor's matching stage art.
     *  Legacy crops ship no art of their own, so this overwrites unconditionally
     *  — a placeholder may already be cached from an earlier lookup, and the
     *  donor (a real fantasy-crop sprite) must win over it. */
    private static void aliasCropStages(ImageContainer images, String legacyCrop, String donorCrop) {
        for (int stage = 0; stage <= MAX_STAGE; stage++) {
            String legacyName = legacyCrop + "_stage" + stage;
            ArrayList<BufferedImage> donor = images.getObjectImages(donorCrop + "_stage" + stage);
            if (donor != null && !donor.isEmpty()) {
                images.objectImages.put(legacyName, donor);
            }
        }
    }

    /** Seed the item cache for a produce name with its mature crop sprite.
     *  Produce items ship no icon of their own, so this overwrites any cached
     *  placeholder. {@code crop_<produce>} resolves through the legacy-crop alias
     *  above for wheat/carrot, so even those borrow real art. */
    private static void aliasProduceToMatureCrop(ImageContainer images, String produceName) {
        if (produceName == null) return;
        // produceName is the crop's bare name (e.g. "emberwheat"); the mature
        // sprite lives under crop_<name>_stage<MATURE>.
        ArrayList<BufferedImage> mature =
            images.getObjectImages("crop_" + produceName + "_stage" + MATURE_STAGE);
        if (mature != null && !mature.isEmpty() && mature.get(0) != null) {
            images.itemImages.put(produceName, mature.get(0));
        }
    }

    private FarmingRegistry() {}
}
