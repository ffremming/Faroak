package ressurser.chunkSystem;

import ressurser.main.GamePanel;
import java.util.HashMap;

public class World extends WorkingMemory {
    ChunkSystemManager csm = new ChunkSystemManager();
    /** the object that is used to load the world entities, has chunk generation from procedural gen */
    public World(GamePanel panel) {
        super(panel);
        chunkSystem.generate = true;
        //TODO Auto-generated constructor stub

       

    }

    /**changes active chunkSystem, saves previous */
    public void setChunkSystemID(String ID){
        csm.save(chunkSystem);
        chunkSystem = csm.getChunkSystem(ID);
    }


    
}
