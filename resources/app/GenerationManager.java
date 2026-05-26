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
import resources.generation.interior.InteriorBootstrap;
import resources.generation.noise.ProceduralGen;
import resources.domain.farming.FarmingRegistry;
import resources.domain.object.Portal;
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
        panel.world = new WorkingMemory(panel);
        panel.imageContainer = new ImageContainer();
        Animations.bootstrap(panel.animations(), panel.imageContainer);
        panel.itemM = new ItemManager(panel);

        panel.mapH = new MapHandler(panel);

        panel.tileM = new TileManager(panel);

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
        Portal toInterior = new Portal(panel, "door",
            p.x + 128, p.y + 128,
            DimensionRegistry.OVERWORLD, DimensionRegistry.INTERIOR, new Point(0, 0));
        toInterior.addComponent(new resources.domain.entity.component.LabelComponent("House"));
        panel.world.placeEntity(toInterior);
        // Scatter a handful of boats on the nearest patch of water.
        BoatSpawner.spawnBoatsNear(panel, p, 3);
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
                if ("ocean".equals(n) || "river".equals(n)) return false;
            }
        }
        return true;
    }
}
