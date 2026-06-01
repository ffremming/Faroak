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
import resources.domain.object.StoneWall;
import resources.domain.object.PlaceAble;
import resources.domain.object.Torch;
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
        // Register the sliced misc sprite-sheet objects (items + placeables).
        // Must run before seedItemIcons() so their icons can be seeded below.
        resources.domain.object.ObjectCatalog.registerAll(panel);
        resources.domain.farming.FarmingRegistry.init();
        resources.domain.farming.FarmingRegistry.registerTileSprites(panel.imageContainer);
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
        // Fence: its sprites live in the shared fences/ variant pack under
        // "fence_v<variant>" names, so the plain "fence" item key resolves to
        // the "?" placeholder by default. Reuse the world-object icon — the
        // template fence has no neighbours, so its variant resolves to the
        // isolated post (BfenceStandard), which reads well as an inventory icon.
        BaseEntity fenceRep = physicalRepresentations.get("fence");
        if (fenceRep != null) {
            java.util.ArrayList<java.awt.image.BufferedImage> imgs = fenceRep.getImages();
            if (!imgs.isEmpty()) {
                panel.imageContainer.itemImages.put("fence", imgs.get(0));
            }
        }
        // Stone wall: same trick as fence — its art lives in the shared
        // stone_walls/ variant pack under "stone_wall_v<variant>" names, so the
        // plain "stone_wall" item key would otherwise resolve to the "?"
        // placeholder. The template wall has no neighbours, so its variant is
        // the isolated block, which reads well as an inventory icon.
        BaseEntity stoneWallRep = physicalRepresentations.get("stone_wall");
        if (stoneWallRep != null) {
            java.util.ArrayList<java.awt.image.BufferedImage> imgs = stoneWallRep.getImages();
            if (!imgs.isEmpty()) {
                panel.imageContainer.itemImages.put("stone_wall", imgs.get(0));
            }
        }

        // Sliced misc sprite-sheet objects ship no dedicated items/<name>.png
        // icon — reuse their world-object sprite as the inventory/catalog icon,
        // same trick as chest/fence above.
        java.util.List<resources.domain.object.ObjectCatalog.Entry> catalog =
            new java.util.ArrayList<>();
        catalog.addAll(resources.domain.object.ObjectCatalog.ENTRIES);
        catalog.addAll(resources.domain.object.ObjectCatalog.EXTRA);
        for (resources.domain.object.ObjectCatalog.Entry e : catalog) {
            if (panel.imageContainer.itemImages.containsKey(e.name)) continue;
            java.util.ArrayList<java.awt.image.BufferedImage> imgs =
                panel.imageContainer.getObjectImages(e.name);
            if (imgs != null && !imgs.isEmpty() && imgs.get(0) != null) {
                panel.imageContainer.itemImages.put(e.name, imgs.get(0));
            }
        }
    }



    private void setupPR(){
        physicalRepresentations.put("hammer",new PlaceAble(panel, "demoHouse", 0, 0, 5*64, 5*64, 5*64, 64, 0, 4*64, true));
        physicalRepresentations.put("demoHouse",new PlaceAble(panel, "demoHouse", 0, 0, 3*64, 2*64, 64, 64, 0, 64, true));
        physicalRepresentations.put("block",new PlaceAble(panel, "block", 0, 0, 64, 64, 64, 64, 0, 64, true));

        // New placeables (phase 4)
        physicalRepresentations.put("fence",    new Fence(panel, 0, 0));
        physicalRepresentations.put("stone_wall", new StoneWall(panel, 0, 0));
        physicalRepresentations.put("torch",    new Torch(panel, 0, 0));
        physicalRepresentations.put("barrel",   new Barrel(panel, 0, 0));
        physicalRepresentations.put("chest",    new Chest(panel, 0, 0));
        physicalRepresentations.put("crafting_table", new CraftingTable(panel, 0, 0));
        // "farmland" is no longer a placeable object: tilling now mutates the
        // tile layer in place (see PlacementRegistry "hoe"/"farmland" → TILL_TILE).

        // Seeds resolve to a placeable Farmland-target hint — the actual planting
        // happens via FarmingService when the player clicks on existing farmland.
        // Keep them placeable as a fallback so the inventory UI shows previews.
        physicalRepresentations.put("seeds_wheat",  new PlaceAble(panel, "crop_wheat_stage0",  0, 0, 64, 64, 48, 48, 8, 8, false));
        physicalRepresentations.put("seeds_carrot", new PlaceAble(panel, "crop_carrot_stage0", 0, 0, 64, 64, 48, 48, 8, 8, false));
        for (String c : new String[]{"emberwheat","frostbloom","glowcap","manaberry",
                                     "ironvine","sungourd","bloodroot","stardrop"}) {
            physicalRepresentations.put("seeds_" + c,
                new PlaceAble(panel, "crop_" + c + "_stage0", 0, 0, 64, 64, 48, 48, 8, 8, false));
        }

        // Boat: a real Boat instance so left-click placement spawns a working
        // (rideable) vessel. The template never enters the world, so no AI;
        // the placed boat is also AI-free until the player dismounts it.
        physicalRepresentations.put("boat", new Boat(panel, 0, 0, false));
    }

    public BaseEntity getPhysicalRepresentation(String name) {
        return physicalRepresentations.get(name);
    }


}
