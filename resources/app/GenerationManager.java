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
import resources.generation.dimension.Dimension;
import resources.generation.dimension.DimensionRegistry;
import resources.generation.noise.ProceduralGen;
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
            new Dimension(DimensionRegistry.CAVE,
                new resources.generation.factory.EntityFactory(panel, gen),
                false, 0.0));

        DimensionService dimensions = new DimensionService(panel, panel.world);
        dimensions.subscribe(panel.events());
        panel.setDimensions(dimensions);

        panel.world.initial();
        panel.world.update(new Point(0,0));

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
        System.out.println(p);
        panel.player = (new Playable(panel, "red",p.x,p.y,(short)48,(short)96,(short)36,(short)32,(short)6,(short)64));
        panel.world.placeEntity(panel.player);
        panel.player.components().add(new LightSourceComponent(200, 1.0f, new Color(255, 220, 160)));
    }


    private Point getStartingPoint(){
        for (int x = -panel.tileSize*10;x<=panel.tileSize*10;x+=panel.tileSize){
            for (int y = -panel.tileSize*10;y<=panel.tileSize*10;y+=panel.tileSize){
                if (!(panel.world.solidCollision(new HitBox(x,y,panel.tileSize*2,panel.tileSize*2)))){
                    return new Point(x,y);
                }
            }
        }
        return null;
    }
}
