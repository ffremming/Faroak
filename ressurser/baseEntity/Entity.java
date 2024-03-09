package ressurser.baseEntity;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import ressurser.main.GamePanel;
import java.awt.Rectangle;

public class Entity extends BaseEntity{

    

    
    protected int animationIndex = 0;

    public Entity(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
        //TODO Auto-generated constructor stub
    }

    public Rectangle getRectangle() {
        return new Rectangle((int)worldX,(int) worldY, width, height);
       
    }
}
