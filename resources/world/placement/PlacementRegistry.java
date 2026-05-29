package resources.world.placement;

import java.util.HashMap;
import java.util.Map;

import resources.app.GamePanel;
import resources.domain.farming.Farmland;
import resources.domain.object.Barrel;
import resources.domain.object.CraftingTable;
import resources.domain.object.Fence;
import resources.domain.object.Torch;
import resources.world.placement.PlacementSpec.SnapPolicy;

/**
 * Registry of every item name that can be placed/used via a left-click in
 * the world. Single source of truth for the mouse pipeline:
 *
 *   itemName  →  PlacementSpec(factory, surfaceRule, snap, action, ghost)
 *
 * Adding a new placeable is one {@link #register} call. Boats keep their
 * dedicated path in {@link resources.world.WorldInteraction#tryPlaceBoat}
 * because their water-only rule is too specialised to fit
 * {@link SurfaceRule} cleanly.
 */
public final class PlacementRegistry {

    private static final Map<String, PlacementSpec> SPECS = new HashMap<>();

    public static void register(PlacementSpec spec) {
        if (spec == null || spec.itemName == null) return;
        SPECS.put(spec.itemName, spec);
    }

    public static PlacementSpec get(String itemName) {
        if (itemName == null) return null;
        return SPECS.get(itemName);
    }

    public static boolean isPlaceable(String itemName) {
        return itemName != null && SPECS.containsKey(itemName);
    }

    /**
     * Populate the registry with the built-in placeables. Safe to call
     * repeatedly — entries replace themselves. Wire from
     * {@link resources.domain.inventory.ItemManager#setupPR} so it runs
     * once at game-panel construction.
     */
    public static void registerDefaults(GamePanel panel) {
        register(new PlacementSpec(
            "fence",
            p -> new Fence(p, 0, 0),
            SurfaceRule.ANY,
            SnapPolicy.TILE,
            PlacementAction.PLACE_ENTITY,
            null,
            true));

        register(new PlacementSpec(
            "farmland",
            p -> new Farmland(p, 0, 0),
            SurfaceRule.TILLABLE,
            SnapPolicy.TILE,
            PlacementAction.PLACE_ENTITY,
            null,
            true));

        register(new PlacementSpec(
            "torch",
            p -> new Torch(p, 0, 0),
            SurfaceRule.NOT_WATER,
            SnapPolicy.FREE,
            PlacementAction.PLACE_ENTITY,
            null,
            true));

        register(new PlacementSpec(
            "barrel",
            p -> new Barrel(p, 0, 0),
            SurfaceRule.NOT_WATER,
            SnapPolicy.TILE,
            PlacementAction.PLACE_ENTITY,
            null,
            true));

        register(new PlacementSpec(
            "crafting_table",
            p -> new CraftingTable(p, 0, 0),
            SurfaceRule.NOT_WATER,
            SnapPolicy.TILE,
            PlacementAction.PLACE_ENTITY,
            null,
            true));

        register(new PlacementSpec(
            "seeds_wheat",
            p -> null,
            SurfaceRule.ON_FARMLAND_UNPLANTED,
            SnapPolicy.TILE,
            PlacementAction.PLANT_SEED,
            null,
            false));

        register(new PlacementSpec(
            "seeds_carrot",
            p -> null,
            SurfaceRule.ON_FARMLAND_UNPLANTED,
            SnapPolicy.TILE,
            PlacementAction.PLANT_SEED,
            null,
            false));
    }

    private PlacementRegistry() {}
}
