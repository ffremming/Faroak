package ressurser.baseEntity.tile;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.SpriteHandler;

public class SpriteHandlerTiles extends SpriteHandler{

    public SpriteHandlerTiles(BaseEntity baseEntity) {
        super(baseEntity);
        //TODO Auto-generated constructor stub
    }
    public void loadSprites(){
        
        baseEntity.panel.spriteLoader.loadTileSprite(baseEntity.getName());
        
        
    }
}
