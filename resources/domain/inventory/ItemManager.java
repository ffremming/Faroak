package resources.domain.inventory;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;

import java.util.HashMap;

import resources.domain.entity.BaseEntity;
import resources.domain.object.Barrel;
import resources.domain.object.Boat;
import resources.domain.object.Chest;
import resources.domain.object.CraftingTable;
import resources.domain.object.Fence;
import resources.domain.object.PlaceAble;
import resources.domain.object.Torch;
import resources.domain.farming.Farmland;
import resources.app.GamePanel;
import resources.world.placement.PlacementRegistry;

public class ItemManager {

    GamePanel panel;

    private HashMap <String,Item> items = new HashMap<String,Item>();
    private HashMap <String,BaseEntity> physicalRepresentations = new HashMap<String,BaseEntity>();

    public ItemManager(GamePanel panel) {
        this.panel = panel;
        setupPR();
        PlacementRegistry.registerDefaults(panel);
        seedItemIcons();
    }

    /**
     * Pre-populate inventory thumbnails for items whose art lives outside the
     * standard resources/images/items/<name>.png convention. Currently only
     * the boat needs this — its sprites live under objects/ships/starterShip/,
     * so left to the default loader it would render as a colored placeholder.
     */
    private void seedItemIcons() {
        if (panel.imageContainer == null) return;
        BaseEntity rep = physicalRepresentations.get("boat");
        if (rep instanceof Boat) {
            Boat b = (Boat) rep;
            java.util.ArrayList<java.awt.image.BufferedImage> imgs = b.getImages();
            if (!imgs.isEmpty()) {
                panel.imageContainer.itemImages.put("boat", imgs.get(0));
            }
        }
        // Chest: no dedicated item sprite ships with the game — reuse the
        // world-object icon so the hotbar shows the chest art instead of
        // the placeholder.
        BaseEntity chestRep = physicalRepresentations.get("chest");
        if (chestRep != null) {
            java.util.ArrayList<java.awt.image.BufferedImage> imgs = chestRep.getImages();
            if (!imgs.isEmpty()) {
                panel.imageContainer.itemImages.put("chest", imgs.get(0));
            }
        }
    }



    private void setupPR(){
        physicalRepresentations.put("hammer",new PlaceAble(panel, "demoHouse", 0, 0, 5*64, 5*64, 5*64, 64, 0, 4*64, true));
        physicalRepresentations.put("demoHouse",new PlaceAble(panel, "demoHouse", 0, 0, 3*64, 2*64, 64, 64, 0, 64, true));
        physicalRepresentations.put("block",new PlaceAble(panel, "block", 0, 0, 64, 64, 64, 64, 0, 64, true));

        // New placeables (phase 4)
        physicalRepresentations.put("fence",    new Fence(panel, 0, 0));
        physicalRepresentations.put("torch",    new Torch(panel, 0, 0));
        physicalRepresentations.put("barrel",   new Barrel(panel, 0, 0));
        physicalRepresentations.put("chest",    new Chest(panel, 0, 0));
        physicalRepresentations.put("crafting_table", new CraftingTable(panel, 0, 0));
        physicalRepresentations.put("farmland", new Farmland(panel, 0, 0));

        // Seeds resolve to a placeable Farmland-target hint — the actual planting
        // happens via FarmingService when the player clicks on existing farmland.
        // Keep them placeable as a fallback so the inventory UI shows previews.
        physicalRepresentations.put("seeds_wheat",  new PlaceAble(panel, "crop_wheat_stage0",  0, 0, 64, 64, 48, 48, 8, 8, false));
        physicalRepresentations.put("seeds_carrot", new PlaceAble(panel, "crop_carrot_stage0", 0, 0, 64, 64, 48, 48, 8, 8, false));

        // Boat: a real Boat instance so left-click placement spawns a working
        // (rideable) vessel. The template never enters the world, so no AI;
        // the placed boat is also AI-free until the player dismounts it.
        physicalRepresentations.put("boat", new Boat(panel, 0, 0, false));
    }

    public BaseEntity getPhysicalRepresentation(String name) {
        return physicalRepresentations.get(name);
    }


}
