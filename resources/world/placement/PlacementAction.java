package resources.world.placement;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.farming.FarmingService;
import resources.domain.player.Playable;

/**
 * Command: what should actually happen when the player left-clicks while
 * holding a registered placeable. Two built-in actions cover the current
 * needs; new actions plug in without touching the call site in
 * {@link resources.world.WorldInteraction#tryPlaceEntity}.
 *
 * Return {@code true} if the action succeeded (the caller decrements the
 * inventory stack on success — except for actions that handle consumption
 * themselves, which is documented per action).
 */
@FunctionalInterface
public interface PlacementAction {

    boolean execute(GameContext ctx, Playable player, PlacementSpec spec, Point worldTarget);

    /**
     * Standard "drop an entity at the snapped/free position" path. The
     * caller in {@link resources.world.WorldInteraction} runs collision and
     * surface checks; this action is a marker so the pipeline knows the
     * default branch was taken. The actual placement is performed inline
     * by the pipeline (factory → position → world.placeEntity).
     */
    PlacementAction PLACE_ENTITY = (ctx, player, spec, pt) -> true;

    /**
     * Till the tile under the cursor into a
     * {@link resources.domain.farming.FarmTile} in place. Delegates to
     * {@link FarmingService#tillAt}. Hoe is a non-consumable tool, so this
     * action does NOT decrement the stack; the placement pipeline must skip its
     * own decrement for TILL_TILE.
     */
    PlacementAction TILL_TILE = (ctx, player, spec, pt) ->
        FarmingService.tillAt(ctx, pt);

    /**
     * Seed planting at the cursor: delegates to
     * {@link FarmingService#plantSeedAt} which plants on the FarmTile under the
     * point and consumes one seed from the equipped stack on success. The
     * placement pipeline must NOT decrement the stack a second time when this
     * action is used.
     */
    PlacementAction PLANT_SEED = (ctx, player, spec, pt) ->
        FarmingService.plantSeedAt(player, ctx, pt);

    /**
     * Water the {@link resources.domain.farming.FarmTile} under the cursor.
     * Watering can is a tool, not a consumable — no stack decrement.
     */
    PlacementAction WATER_TILE = (ctx, player, spec, pt) ->
        FarmingService.waterAt(ctx, pt);
}
