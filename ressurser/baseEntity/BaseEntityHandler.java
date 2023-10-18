package ressurser.baseEntity;

import ressurser.chunkSystem.ChunkSystem;
import ressurser.main.GamePanel;

public class BaseEntityHandler {

    GamePanel panel;
    ChunkSystem chunkSystem;


    public BaseEntityHandler(GamePanel panel){
        this.panel = panel;
        chunkSystem = new ChunkSystem(panel);
    }
    



}
