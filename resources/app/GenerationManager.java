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
import resources.world.MapHandler;
import resources.world.ChunkSystem;
import resources.world.WorkingMemory;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
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
