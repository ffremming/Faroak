package ressurser.drawing;

import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Sprite;
import ressurser.baseEntity.primitiveEntity;
import ressurser.main.GamePanel;

public class Camera extends primitiveEntity{

    private BaseEntity followed;

    public Camera(GamePanel panel, String name, int worldX, int worldY, short width, short height) {
        super(panel, name, worldX, worldY, (short) panel.screenWidth,(short)(panel.screenHeight));
        //TODO Auto-generated constructor stub
        follow(panel.spiller);
    }



    /**
     * follows entitiy until given other orders
     */
    public void follow(BaseEntity entity){
        //TODO
        this.followed = entity;
    }

    public void draw(){
        //dont need to update all that often. - this could be updated in chunkSystem
        panel.chunkSystem.getEntitiesInBound(this.getHitBox());
        
        ArrayList<BaseEntity> entityList = panel.chunkSystem.semiStaticEntitiesRendered;
        //this only works if chunkysstem updates entites frequently
        for (BaseEntity baseE :panel.chunkSystem.semiStaticEntitiesRendered){
            drawRelative(baseE);
        }
        panel.g.dispose();

    }

    public void drawRelative(BaseEntity entity){
        //g2.drawImage()

        //if followed:   -  could change to own coordinated
        int tempScreenX = (entity.worldX)-worldX;
        int tempScreenY = (entity.worldY)-worldY;
    
        panel.g.drawImage(entity.getImage(),tempScreenX,tempScreenY,sprite.width,sprite.height,null);     //can remove width and height.
    }



    private void center(BaseEntity entity){
        int entityX = entity.getWorldX();
        int entityY = entity.getWorldY();

        //get the startValues for x and y
        worldX = entityX-width/2;
        worldY = entityY+height/2;
    }

    //can add loads of other abilities

}
