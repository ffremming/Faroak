package resources.world;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.tile.Tile;
import resources.domain.tile.CliffTile;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.generation.factory.EntityFactory;
import resources.domain.object.GameObject;
import resources.domain.player.Moveable;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.presentation.camera.Camera;

import java.util.HashMap;

/** this class will contain all chunksystems and have the neccesassary methods to add, remove, write to file etc.*/ 
public class ChunkSystemManager {
    
    HashMap<String,ChunkSystem> chunkSystems = new HashMap<>();

    public void addChunkSystem(ChunkSystem chunkSystem) throws Exception{
        String id = chunkSystem.getID();
        if (!chunkSystems.keySet().contains(id)){
            chunkSystems.put(id,chunkSystem);
        } else {
            throw new Exception("ID needs to be unique");
        }
    }

    public boolean contains(String ID){
        return chunkSystems.keySet().contains(ID);
    }

    public void removeChunkSystem(String ID){
        chunkSystems.remove(ID);
    }

    //when chunkSystem no longer is in use - save it to file, then 



    public ChunkSystem getChunkSystem(String ID){return chunkSystems.get(ID); }

    public void save(ChunkSystem chunkSystem) {
        if (chunkSystem == null){return;}
        
        //initiate writing to file

        //write every chunk that has been generated 
    }


}
