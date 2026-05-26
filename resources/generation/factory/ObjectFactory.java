package resources.generation.factory;

import resources.app.GamePanel;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.inventory.HarvestRegistry;
import resources.domain.object.GameObject;
import resources.generation.factory.ObjectCatalog.ObjectSpec;

/**
 * Single chokepoint for turning an object-name into a placed {@link GameObject}.
 *
 * The generator (EntityFactory) only knows "place object X at (x,y)" — sprite
 * dimensions, hitbox footprint, solidity and any attached gameplay component
 * (harvestable, etc.) are resolved here from {@link ObjectCatalog} and
 * {@link HarvestRegistry}. Adding a new object kind = one entry in the catalog
 * (+ optional harvest profile), no changes to call sites.
 */
public final class ObjectFactory {

    public static GameObject create(GamePanel panel, String name, int worldX, int worldY) {
        ObjectSpec s = ObjectCatalog.get(name);
        GameObject obj = new GameObject(panel, s.name,
                worldX, worldY,
                s.width, s.height,
                s.hitBoxWidth, s.hitBoxHeight,
                s.hitBoxOffsetX, s.hitBoxOffsetY,
                s.solid);

        HarvestableComponent harvest = HarvestRegistry.componentFor(s.name);
        if (harvest != null) obj.components().add(harvest);
        return obj;
    }

    public static ObjectSpec spec(String name) {
        return ObjectCatalog.get(name);
    }

    private ObjectFactory() {}
}
