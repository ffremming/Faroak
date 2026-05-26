package resources.domain.entity;

import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.app.GamePanel;
import java.awt.Rectangle;

public class Entity extends BaseEntity{

    

    
    protected int animationIndex = 0;

    public Entity(GamePanel panel, String name, int worldX, int worldY, int width, int height, int hitBoxWidth,
            int hitBoxHeight, int relativeXPLus, int relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
    }

    public Entity(GamePanel panel, String name, double worldX, double worldY, int width, int height, HitBox hitBox) {
        super(panel, name, worldX, worldY, width, height, hitBox);
    }

    public Rectangle getRectangle() {
        return new Rectangle((int)worldX,(int) worldY, width, height);
       
    }
}
