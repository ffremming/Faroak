package ressurser.chunkSystem;

import ressurser.main.GamePanel;

public class World extends WorkingMemory {

    /** the object that is used to load the world entities, has chunk generation from procedural gen */
    public World(GamePanel panel) {
        super(panel);
        chunkSystem.generate = true;
        //TODO Auto-generated constructor stub
    }
    
}
