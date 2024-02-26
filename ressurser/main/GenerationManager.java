package ressurser.main;
import java.awt.Point;

import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.playable.Playable;
import ressurser.baseEntity.tile.TileManager;
import ressurser.chunkSystem.ChunkSystem;
import ressurser.drawing.Camera;

public class GenerationManager {
    
    GamePanel panel;

    public GenerationManager(GamePanel panel){
        this.panel = panel;
    }


    public void generateMap(){
        panel.imageContainer = new ImageContainer();
        panel.chunkSystem = new ChunkSystem(panel);
        
        panel.mapH = new MapHandler(panel);
      
        panel.tileM = new TileManager(panel);
        panel.chunkSystem.workingMemory.initial();
        panel.chunkSystem.workingMemory.update(new Point(0,0));
    }


    public void initiate(){
        Point p = getStartingPoint();
        panel.player = (new Playable(panel, "red",p.x,p.y,(short)48,(short)96,(short)36,(short)32,(short)6,(short)64));
        panel.chunkSystem.addEntity(panel.player);
    }


    private Point getStartingPoint(){
        for (int x = -panel.tileSize*10;x<=panel.tileSize*10;x+=panel.tileSize){
            for (int y = -panel.tileSize*10;y<=panel.tileSize*10;y+=panel.tileSize){
                if (!(panel.chunkSystem.workingMemory.solidCollision(new HitBox(x,y,panel.tileSize*2,panel.tileSize*2)))){
                    return new Point(x,y);
                }
            }
        }
        return null;
    }
}
