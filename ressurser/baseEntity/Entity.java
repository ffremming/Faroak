package ressurser.baseEntity;

import ressurser.main.GamePanel;

public class Entity extends BaseEntity{

    HitBox interactionHitBox;

    public Entity(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
        //TODO Auto-generated constructor stub


       
    }

    /**
     * is called from the entity class. when an entity interacts with another entity.
     */
    public void interact(BaseEntity entity){
        entity.interact(this);
    }


    
}
