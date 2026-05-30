package resources.world.placement;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.farming.FarmTile;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;

/**
 * Strategy: decides whether a candidate placement is acceptable on a given
 * tile/world position. Each rule is stateless and can be added to a
 * {@link PlacementSpec} without touching the placement pipeline.
 *
 * Built-in rules cover the common cases; custom rules plug in as lambdas.
 */
@FunctionalInterface
public interface SurfaceRule {

    boolean allows(GameContext ctx, int worldX, int worldY, HitBox candidate);

    /** No constraint — anywhere collision-clear is fine. */
    SurfaceRule ANY = (ctx, x, y, hb) -> true;

    /** Tilled-able terrain: matches {@link TileRules#isTillable(String)}. */
    SurfaceRule TILLABLE = (ctx, x, y, hb) -> {
        Tile t = ctx.world().getTile(new Point(x + hb.width / 2, y + hb.height / 2));
        return t != null && TileRules.isTillable(t.getName());
    };

    /** The tile under the candidate centre must be an unplanted
     *  {@link FarmTile}. Used by seed items so seeds only go on tilled soil. */
    SurfaceRule ON_FARMTILE_UNPLANTED = (ctx, x, y, hb) -> {
        Tile t = ctx.world().getTile(new Point(x + hb.width / 2, y + hb.height / 2));
        return t instanceof FarmTile && !((FarmTile) t).isPlanted();
    };

    /** Anywhere that's not a water tile under the candidate centre. */
    SurfaceRule NOT_WATER = (ctx, x, y, hb) -> {
        Tile t = ctx.world().getTile(new Point(x + hb.width / 2, y + hb.height / 2));
        return t == null || !TileRules.isWater(t.getName());
    };

    /** Plains or forest terrain only. */
    SurfaceRule GRASS_OR_FOREST = (ctx, x, y, hb) -> {
        Tile t = ctx.world().getTile(new Point(x + hb.width / 2, y + hb.height / 2));
        if (t == null) return false;
        String n = t.getName();
        return n != null && (n.startsWith("plains") || n.startsWith("forest"));
    };
}
