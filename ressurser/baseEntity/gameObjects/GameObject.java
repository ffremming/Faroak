package ressurser.baseEntity.gameObjects;

import ressurser.baseEntity.BaseEntity;
import ressurser.main.GamePanel;

public abstract class GameObject extends BaseEntity{


    boolean edible = false;
    boolean placeable = false;
    
    final int NONFRAGILE = 0;
    final int TOOLFRAGILE = 1;
    final int FRAGILE = 2;

    int fragile = 0;

    boolean changeAbleValues = false;
    
    

    

    public GameObject(GamePanel panel,String name, int worldX, int worldY, short width, short height, short hitBoxWidth,short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel,name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
       
        type = SUPEROBJECT;

        //generate correct sprites sprites - new SpriteHandler for every Object.
        spriteHandler = new SpriteHandlerObjects(this);
        
    }


     
    enum type {
        SEED,DECOR,TOOL,ARTIFACT,EDIBLE,RESOURCE
    }
    
    
}

