package resources.input.click;

import java.awt.Point;
import java.util.List;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Stack;
import resources.domain.object.Boat;
import resources.domain.object.Chest;
import resources.domain.object.CraftingTable;
import resources.domain.object.GameObject;
import resources.world.placement.PlacementRegistry;

/**
 * Ordered chain of {@link ClickInteraction} strategies for an offline left
 * click on the world. The first strategy that consumes the click wins.
 *
 * Order matters and encodes intent priority:
 *   1. Board a clicked boat (unless holding a boat item — then placing wins).
 *   2. Open a clicked, in-reach container (chest, crafting table).
 *   3. Use the equipped item via the {@link PlacementRegistry} (place an
 *      entity, hoe a tile, plant a seed, water) — the player's intent was to
 *      act with what they're holding.
 *   4. Harvest the entity under the cursor (axe/pickaxe/bare hands).
 *
 * This is the single dispatch site for cursor-targeted world actions; spacebar
 * handles only non-targeted interactions (boat dismount, nearest container).
 */
public final class ClickRouter {

    /** Omnidirectional reach (px) for opening a clicked container. ~2 tiles. */
    private static final double CONTAINER_REACH_PX = 2 * 64;

    private final List<ClickInteraction> chain = List.of(
        ClickRouter::boardBoat,
        ClickRouter::openContainer,
        ClickRouter::useEquippedItem,
        ClickRouter::harvest
    );

    /** Run the chain; returns true if some strategy consumed the click. */
    public boolean route(GamePanel panel, Point worldPoint) {
        for (ClickInteraction interaction : chain) {
            if (interaction.handle(panel, worldPoint)) return true;
        }
        return false;
    }

    // ---- strategies ----

    /** Board a clicked boat the player is next to — unless holding a boat item,
     *  in which case placement (a later strategy) should win. */
    private static boolean boardBoat(GamePanel panel, Point at) {
        if (panel.player() == null || heldItemIs(panel, "boat")) return false;
        for (BaseEntity ent : panel.world().getEntitiesCollidedWith(at)) {
            if (!(ent instanceof Boat)) continue;
            Boat boat = (Boat) ent;
            if (!boat.getHitBox().collision(at)) continue;
            return boat.tryBoardFromClick(panel.player());
        }
        return false;
    }

    /** Open a clicked chest / crafting table within reach via its interact(). */
    private static boolean openContainer(GamePanel panel, Point at) {
        if (panel.player() == null) return false;
        for (BaseEntity ent : panel.world().getEntitiesCollidedWith(at)) {
            if (!(ent instanceof Chest) && !(ent instanceof CraftingTable)) continue;
            GameObject obj = (GameObject) ent;
            if (!obj.getHitBox().collision(at)) continue;
            if (!withinReach(panel, obj)) continue;
            obj.interact(panel.player());
            return true;
        }
        return false;
    }

    /** Use the equipped item if it's registered in the {@link PlacementRegistry}
     *  (place entity, till, plant, water). */
    private static boolean useEquippedItem(GamePanel panel, Point at) {
        Stack equipped = panel.player() == null ? null : panel.player().getEquipped();
        String itemName = (equipped != null && !equipped.isEmpty()) ? equipped.getName() : null;
        if (itemName == null || !PlacementRegistry.isPlaceable(itemName)) return false;
        return panel.world.tryPlaceEntity(equipped);
    }

    /** Harvest the entity under the cursor (tool gating applies inside). */
    private static boolean harvest(GamePanel panel, Point at) {
        if (panel.player() == null) return false;
        if (panel.world.tryHarvestAtMouse(panel.player(), panel)) return true;
        // Legacy fallback: items with a physical representation that aren't in
        // the placement registry (hammer/demoHouse/block) still place free-form.
        Stack equipped = panel.player().getEquipped();
        String itemName = (equipped != null && !equipped.isEmpty()) ? equipped.getName() : null;
        return itemName != null && panel.world.tryPlaceEntity(equipped);
    }

    // ---- shared helpers ----

    private static boolean withinReach(GamePanel panel, GameObject obj) {
        double pcx = panel.player().getWorldX() + panel.player().getWidth()  / 2.0;
        double pcy = panel.player().getWorldY() + panel.player().getHeight() / 2.0;
        double ocx = obj.getWorldX() + obj.getWidth()  / 2.0;
        double ocy = obj.getWorldY() + obj.getHeight() / 2.0;
        double dx = ocx - pcx;
        double dy = ocy - pcy;
        return (dx * dx + dy * dy) <= (CONTAINER_REACH_PX * CONTAINER_REACH_PX);
    }

    private static boolean heldItemIs(GamePanel panel, String name) {
        Stack eq = panel.player().getEquipped();
        return eq != null && !eq.isEmpty() && name.equals(eq.getName());
    }
}
