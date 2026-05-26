package resources.domain.farming;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Stack;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;

/**
 * Stateless helpers for hoeing tiles into {@link Farmland} and planting seeds
 * from the player's equipped stack.
 *
 * Kept out of {@link Playable} and out of the input layer so callers (input
 * handlers, AI, tests) all funnel through one place — single place to tweak
 * range, biome rules, or seed semantics.
 */
public final class FarmingService {

    private static final String HOE_ITEM    = "hoe";
    private static final String SEED_PREFIX = "crop_";

    /**
     * If the equipped item is a seed and there's a Farmland in interaction
     * range, plant on it. Returns true if a crop was planted.
     */
    public static boolean tryPlantOnFarmland(Playable player, GameContext ctx) {
        Stack eq = player.getEquipped();
        if (eq == null || eq.isEmpty()) return false;
        String itemName = eq.getName();
        if (itemName == null || !itemName.startsWith(SEED_PREFIX)) return false;

        HitBox reach = player.getInteractionHitBox();
        for (BaseEntity ent : ctx.world().getEntities()) {
            if (!(ent instanceof Farmland)) continue;
            if (!ent.getHitBox().intersects(reach)) continue;
            Farmland fl = (Farmland) ent;
            if (fl.isPlanted()) continue;
            if (fl.plant(itemName)) {
                eq.removeOneItem();
                return true;
            }
        }
        return false;
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

    private static boolean isTillable(String tileName) {
        if (tileName == null) return false;
        return tileName.startsWith("plains")
            || tileName.startsWith("forest")
            || tileName.startsWith("savanna");
    }

    private FarmingService() {}
}
