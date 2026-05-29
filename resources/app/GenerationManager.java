package resources.app;

import resources.presentation.animation.Animations;
import resources.presentation.image.ImageContainer;
import resources.presentation.image.ImageResources;
import resources.presentation.camera.Camera;
import resources.presentation.ui.Container;
import resources.presentation.ui.Button;
import resources.presentation.ui.UserInterface;
import resources.input.Keys;
import resources.input.Mouse;
import resources.input.InputHandlingSystem;
import resources.generation.cave.CaveBootstrap;
import resources.generation.dimension.Dimension;
import resources.generation.dimension.DimensionRegistry;
import resources.generation.interior.Interior;
import resources.generation.interior.InteriorBootstrap;
import resources.generation.interior.InteriorManager;
import resources.generation.interior.InteriorRegistry;
import resources.generation.noise.ProceduralGen;
import resources.domain.farming.FarmingRegistry;
import resources.domain.object.Portal;
import resources.domain.player.Npc;
import resources.domain.ai.IdleStrollBehavior;
import resources.domain.spawn.BoatSpawner;
import resources.world.DimensionService;
import resources.world.MapHandler;
import resources.world.ChunkSystem;
import resources.world.WorkingMemory;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.entity.component.LightSourceComponent;
import resources.domain.tile.Tile;
import resources.domain.tile.TileManager;
import resources.domain.object.GameObject;
import resources.domain.player.Playable;
import resources.domain.player.Moveable;
import resources.domain.inventory.ItemManager;
import resources.environment.EnvironmentManager;
import resources.generation.factory.EntityFactory;
import resources.generation.plant.PlantCatalog;
import resources.generation.factory.ObjectFactory;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import java.awt.Color;
import java.awt.Point;

import resources.geometry.HitBox;
import resources.domain.player.Moveable;
import resources.domain.player.Playable;
import resources.domain.inventory.ItemManager;
import resources.domain.tile.TileManager;
import resources.world.ChunkSystem;
import resources.world.WorkingMemory;
import resources.presentation.camera.Camera;

public class GenerationManager {
    
    GamePanel panel;

    public GenerationManager(GamePanel panel){
        this.panel = panel;
    }


    public void generateMap(){
        resources.presentation.ui.LoadingScreen.setStatus("Allocating world memory");
        panel.world = new WorkingMemory(panel);
        resources.presentation.ui.LoadingScreen.setStatus("Loading images");
        panel.imageContainer = new ImageContainer();
        // Plant pack is loaded from the hand-authored spritesheet (plants_green.png).
        // bootstrap() is a deliberate no-op — the old procedural pipeline is NOT
        // wired in and must never generate images at runtime.
        PlantCatalog.bootstrap();
        resources.presentation.ui.LoadingScreen.setStatus("Building animations");
        Animations.bootstrap(panel.animations(), panel.imageContainer);
        resources.presentation.ui.LoadingScreen.setStatus("Loading items");
        panel.itemM = new ItemManager(panel);

        resources.presentation.ui.LoadingScreen.setStatus("Preparing maps");
        panel.mapH = new MapHandler(panel);

        resources.presentation.ui.LoadingScreen.setStatus("Preparing tiles");
        panel.tileM = new TileManager(panel);

        resources.presentation.ui.LoadingScreen.setStatus("Registering dimensions");
        ProceduralGen gen = new ProceduralGen();
        DimensionRegistry.instance().register(DimensionRegistry.OVERWORLD,
            new Dimension(DimensionRegistry.OVERWORLD,
                new resources.generation.factory.EntityFactory(panel, gen),
                true, 0.0));
        DimensionRegistry.instance().register(DimensionRegistry.CAVE,
            CaveBootstrap.register(panel));
        DimensionRegistry.instance().register(DimensionRegistry.INTERIOR,
            InteriorBootstrap.register(panel));

        DimensionService dimensions = new DimensionService(panel, panel.world);
        dimensions.subscribe(panel.events());
        panel.setDimensions(dimensions);

        // Wire harvest profiles for mature crops (idempotent).
        FarmingRegistry.init();

        resources.presentation.ui.LoadingScreen.setStatus("Generating terrain");
        panel.world.initial();
        panel.world.update(new Point(0,0));

    }

    /**
     * Spawn world entities that aren't part of per-tile generation: a portal
     * pair to the cave/interior, and a handful of boats on nearby water.
     * Called after the player exists so we can target spawn-relative positions.
     */
    public void seedWorldEntities() {
        if (panel.player == null) return;
        Point p = new Point((int) panel.player.getWorldX(), (int) panel.player.getWorldY());
        Portal toCave = new Portal(panel, "cave_portal",
            p.x + 128, p.y,
            DimensionRegistry.OVERWORLD, DimensionRegistry.CAVE, new Point(0, 0));
        toCave.addComponent(new resources.domain.entity.component.LabelComponent("Cave"));
        panel.world.placeEntity(toCave);
        // Arrive one tile north of the starter house door so the player
        // spawns on a floor cell, not inside the door tile itself.
        Point doorPx = interiorDoorPixel(InteriorRegistry.STARTER_HOUSE, 0, 0);
        Point arrival = new Point(doorPx.x, doorPx.y - panel.tileSize);
        Point overworldDoor = new Point(p.x + 128, p.y + 128);
        Portal toInterior = new Portal(panel, "door",
            overworldDoor.x, overworldDoor.y,
            DimensionRegistry.OVERWORLD, DimensionRegistry.INTERIOR, arrival);
        toInterior.addComponent(new resources.domain.entity.component.LabelComponent("House"));
        panel.world.placeEntity(toInterior);
        // Doors inside any interior should return the player to the overworld
        // portal location, not (0,0).
        InteriorManager interiors = InteriorBootstrap.lastBuilt();
        if (interiors != null) interiors.setOverworldArrival(overworldDoor);
        // Scatter a handful of boats on the nearest patch of water.
        BoatSpawner.spawnBoatsNear(panel, p, 3);
        // A small welcoming committee: a few NPCs strolling near spawn.
        spawnStartingNpcs(p);
        // Demo: place every procedural plant in a tidy grid near spawn so the
        // catalog is visible at a glance. Skips the normal solid-collision
        // check so two adjacent trees can sit on neighbouring tiles.
        spawnPlantDemoGrid(p);
    }

    /**
     * Drop one of each {@link PlantCatalog} slug in a grid a few tiles north
     * of spawn so the full pack is visible at a glance. Spacing is generous
     * so the largest trees (oak_mega, willow_large) don't overlap their
     * neighbours.
     */
    private void spawnPlantDemoGrid(Point spawn) {
        int ts = panel.tileSize;
        int cols = 5;
        int spacingX = (int) (ts * 2.4);
        int spacingY = (int) (ts * 3.2);
        int originX = spawn.x - (cols - 1) * spacingX / 2;
        int originY = spawn.y - ts * 6;

        java.util.List<String> all = PlantCatalog.slugs();
        for (int i = 0; i < all.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int x = originX + col * spacingX;
            int y = originY + row * spacingY;
            GameObject plant = ObjectFactory.create(panel, all.get(i), x, y);
            panel.world.placeEntity(plant);
        }
    }

    /**
     * Place a handful of NPCs around the player's spawn point. Each candidate
     * tile is checked against the same land/solid rules used for the player so
     * NPCs don't pop into water or rocks. Skips a candidate if its patch is
     * blocked; doesn't search further to avoid pushing NPCs out of sight.
     */
    private void spawnStartingNpcs(Point spawn) {
        int ts = panel.tileSize;
        Point[] offsets = {
            new Point( ts * 3,  0),
            new Point(-ts * 3,  0),
            new Point( 0,       ts * 3),
            new Point( ts * 2, -ts * 3),
            new Point(-ts * 2,  ts * 2)
        };
        long seed = System.nanoTime();
        for (int i = 0; i < offsets.length; i++) {
            int x = spawn.x + offsets[i].x;
            int y = spawn.y + offsets[i].y;
            if (!isLandPatch(x, y, ts)) continue;
            Npc npc = new Npc(panel, "npc", x, y, 20,
                new IdleStrollBehavior(seed ^ ((long) x << 16) ^ y));
            panel.world.placeEntity(npc);
        }
    }

    public void newSeed(){
        panel.world = new WorkingMemory(panel);
        panel.world.initial();
        panel.world.update(new Point(0,0));

        initiate();
        //panel.world.update(panel.player.getPoint());
    }


    public void initiate(){
        Point p = getStartingPoint();
        panel.player = (new Playable(panel, "red",p.x,p.y,(short)48,(short)96,(short)36,(short)32,(short)6,(short)64));
        panel.world.placeEntity(panel.player);
        panel.player.components().add(new LightSourceComponent(200, 1.0f, new Color(255, 220, 160)));
        placeStarterChest(p);
    }

    /**
     * Drop a {@link resources.domain.object.Chest} a few tiles to the right of
     * the player spawn. Scans outwards in a small ring so we don't end up
     * stuck in solid terrain on cramped spawns; if nothing fits within the
     * search radius we silently give up rather than crashing world-gen.
     */
    private void placeStarterChest(Point spawn) {
        int ts = panel.tileSize;
        int[][] offsets = {
            {  2,  0 }, { -2,  0 }, {  0,  2 }, {  0, -2 },
            {  2,  2 }, { -2,  2 }, {  2, -2 }, { -2, -2 },
            {  3,  0 }, { -3,  0 }, {  0,  3 }, {  0, -3 }
        };
        for (int[] off : offsets) {
            int cx = spawn.x + off[0] * ts;
            int cy = spawn.y + off[1] * ts;
            if (!isLandPatch(cx, cy, ts)) continue;
            resources.domain.object.Chest chest =
                new resources.domain.object.Chest(panel, cx, cy);
            if (panel.world.placeEntity(chest)) return;
        }
    }


    /**
     * Find a 2x2 land patch within ±10 tiles of the origin. Land here means
     * the tile is neither water (ocean/river) nor solid (no rocks/walls);
     * beach is allowed because it's walkable shore. Falls back to (0,0) if
     * the search radius doesn't find one — which only happens on seeds where
     * the player would spawn in deep ocean, in which case the player will
     * see they need to move; ocean spawn is still better than null/crash.
     */
    private Point getStartingPoint(){
        int ts = panel.tileSize;
        for (int x = -ts*10; x <= ts*10; x += ts){
            for (int y = -ts*10; y <= ts*10; y += ts){
                if (isLandPatch(x, y, ts)) return new Point(x, y);
            }
        }
        return new Point(0, 0);
    }

    /** World-pixel of the door tile for an interior anchored at (slotX, slotY). */
    private Point interiorDoorPixel(Interior interior, int slotX, int slotY) {
        int ts = panel.tileSize;
        int tx = slotX * InteriorManager.SLOT_TILES + interior.doorTx();
        int ty = slotY * InteriorManager.SLOT_TILES + interior.doorTy();
        return new Point(tx * ts, ty * ts);
    }

    private boolean isLandPatch(int x, int y, int ts){
        if (panel.world.solidCollision(new HitBox(x, y, ts*2, ts*2))) return false;
        // Sample every corner of the 2x2 footprint; all must be on dry land.
        int[] xs = { x, x + ts*2 - 1 };
        int[] ys = { y, y + ts*2 - 1 };
        for (int sx : xs) {
            for (int sy : ys) {
                resources.domain.tile.Tile t = panel.world.getTile(new java.awt.Point(sx, sy));
                if (t == null) return false;
                String n = t.getName();
                if ("ocean".equals(n) || "river".equals(n) || "shallowWater".equals(n)) return false;
            }
        }
        return true;
    }
}
