package resources.domain.farming;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Stack;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;
import resources.world.placement.TileRules;

/**
 * Stateless helpers for hoeing tiles into {@link Farmland} and planting seeds
 * from the player's equipped stack.
 *
 * Kept out of {@link Playable} and out of the input layer so callers (input
 * handlers, AI, tests) all funnel through one place — single place to tweak
 * range, biome rules, or seed semantics.
 */
public final class FarmingService {

    private static final String HOE_ITEM     = "hoe";
    private static final String CROP_PREFIX  = "crop_";
    private static final String SEEDS_PREFIX = "seeds_";

    /**
     * If the equipped item is a seed and there's a Farmland in interaction
     * range, plant on it. Returns true if a crop was planted.
     *
     * Inventory seeds are named {@code seeds_<name>} while the planted crop
     * entity / {@link CropRegistry} key is {@code crop_<name>}; this resolves
     * the equipped item to its crop key before planting. Items already named
     * {@code crop_*} are accepted as-is (direct-plant / test path).
     */
    public static boolean tryPlantOnFarmland(Playable player, GameContext ctx) {
        Stack eq = player.getEquipped();
        if (eq == null || eq.isEmpty()) return false;
        String cropName = cropKeyFor(eq.getName());
        if (cropName == null) return false;

        HitBox reach = player.getInteractionHitBox();
        for (BaseEntity ent : ctx.world().getEntities()) {
            if (!(ent instanceof Farmland)) continue;
            if (!ent.getHitBox().intersects(reach)) continue;
            Farmland fl = (Farmland) ent;
            if (fl.isPlanted()) continue;
            if (fl.plant(cropName)) {
                eq.removeOneItem();
                return true;
            }
        }
        return false;
    }

    /**
     * Map an equipped item name to the crop key registered in
     * {@link CropRegistry}, or null if the item isn't a plantable seed.
     * Accepts both {@code seeds_<name>} (inventory) and {@code crop_<name>}.
     */
    static String cropKeyFor(String itemName) {
        if (itemName == null) return null;
        String crop = null;
        if (itemName.startsWith(SEEDS_PREFIX)) {
            crop = CROP_PREFIX + itemName.substring(SEEDS_PREFIX.length());
        } else if (itemName.startsWith(CROP_PREFIX)) {
            crop = itemName;
        }
        return (crop != null && CropRegistry.get(crop) != null) ? crop : null;
    }

    /**
     * If the equipped item is a hoe and the player stands on a tillable tile
     * (plains/forest/savanna), drop a {@link Farmland} GameObject at that
     * tile's coordinate. Returns true on success.
     */
    public static boolean tryHoeTile(Playable player, GameContext ctx) {
        Stack eq = player.getEquipped();
        if (eq == null || eq.isEmpty()) return false;
        if (!HOE_ITEM.equals(eq.getName())) return false;

        HitBox hb = player.getHitBox();
        hb.updateCoords();
        Point center = new Point(hb.getCenter());
        Tile tile = ctx.world().getTile(center);
        if (tile == null) return false;
        if (!isTillable(tile.getName())) return false;

        // Snap to tile grid so the Farmland sits flush.
        int ts = ctx.tileSize();
        int tx = (int) Math.floor(tile.getWorldX() / (double) ts) * ts;
        int ty = (int) Math.floor(tile.getWorldY() / (double) ts) * ts;

        Farmland fl = new Farmland(player.panel, tx, ty);
        // Don't place if there's already a farmland (or anything solid) there.
        for (BaseEntity ent : ctx.world().getEntitiesCollidedWith(fl.getHitBox())) {
            if (ent instanceof Farmland) return false;
        }
        return ctx.world().placeEntity(fl);
    }

    /** Single-source-of-truth via {@link TileRules#isTillable(String)} so
     *  this service and {@code SurfaceRule.TILLABLE} can't drift. */
    private static boolean isTillable(String tileName) {
        return TileRules.isTillable(tileName);
    }

    private FarmingService() {}
}
