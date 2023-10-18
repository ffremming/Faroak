package ressurser.entityFactory;

import ressurser.baseEntity.tile.Tile;
import ressurser.main.GamePanel;

public class EntityFactory {
    
    GamePanel panel;

    /**
     * entityFactory is an object that is responsible for producing entities.
     * it shall get instructions based on strings, and return a entity.
     */
    public EntityFactory(GamePanel panel){
        this.panel = panel;
    }

    public Tile ProduceTile(String recipe){
        Tile tile;


        return null;
    }
}
