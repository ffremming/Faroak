package resources.domain.farming;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.inventory.Stack;
import resources.domain.player.Playable;

/**
 * Stateless helpers for the tile-layer farming flow: hoeing grass into a
 * {@link FarmTile}, planting seeds on a tilled tile, and watering it. Kept out
 * of the input layer and out of {@link Playable} so every caller (mouse
 * placement actions, AI, tests) funnels through one place.
 *
 * Farming state lives on the {@link FarmTile} itself (see that class); this
 * service only resolves the equipped item to an action and applies it.
 */
public final class FarmingService {

    private static final String CROP_PREFIX  = "crop_";
    private static final String SEEDS_PREFIX = "seeds_";

    /**
     * Hoe the tile under {@code worldTarget} into a {@link FarmTile} in place.
     * The hoe is a tool, not a consumable, so nothing is removed from the stack.
     * @return true if a tile was tilled (or was already tilled soil).
     */
    public static boolean tillAt(GameContext ctx, Point worldTarget) {
        return ctx.world().tillTileAt(worldTarget) != null;
    }

    /**
     * Plant the equipped seed on the {@link FarmTile} under {@code worldTarget}.
     * Resolves the inventory seed name ({@code seeds_<name>}) to its crop key
     * ({@code crop_<name>}); items already named {@code crop_*} are accepted as-is
     * (direct-plant / test path). Consumes one seed on success.
     * @return true if a crop was planted.
     */
    public static boolean plantSeedAt(Playable player, GameContext ctx, Point worldTarget) {
        Stack eq = player.getEquipped();
        if (eq == null || eq.isEmpty()) return false;
        String cropName = cropKeyFor(eq.getName());
        if (cropName == null) return false;

        FarmTile tile = ctx.world().farmTileAt(worldTarget);
        if (tile == null || tile.isPlanted()) return false;
        if (!tile.plant(cropName)) return false;
        eq.removeOneItem();
        return true;
    }

    /**
     * Water the {@link FarmTile} under {@code worldTarget}. The watering can is a
     * tool, not a consumable.
     * @return true if a tilled tile was watered.
     */
    public static boolean waterAt(GameContext ctx, Point worldTarget) {
        FarmTile tile = ctx.world().farmTileAt(worldTarget);
        if (tile == null) return false;
        tile.water();
        return true;
    }

    /**
     * Map an equipped item name to the crop key registered in
     * {@link CropRegistry}, or null if the item isn't a plantable seed.
     * Accepts both {@code seeds_<name>} (inventory) and {@code crop_<name>}.
     */
    public static String cropKeyFor(String itemName) {
        if (itemName == null) return null;
        String crop = null;
        if (itemName.startsWith(SEEDS_PREFIX)) {
            crop = CROP_PREFIX + itemName.substring(SEEDS_PREFIX.length());
        } else if (itemName.startsWith(CROP_PREFIX)) {
            crop = itemName;
        }
        return (crop != null && CropRegistry.get(crop) != null) ? crop : null;
    }

    private FarmingService() {}
}
