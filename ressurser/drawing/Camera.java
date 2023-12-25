package ressurser.drawing;

import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;
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
    public Camera(GamePanel panel,String name){
        super(panel,name);
        follow(panel.spiller);
        this.hitBox = new HitBox(this);
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
        
        
        //System.out.println(panel.chunkSystem.getEntitiesInBound(this.getHitBox()));
       
        //this only works if chunkysstem updates entites frequently
        for (BaseEntity baseE :panel.chunkSystem.getEntitiesInBound(this.getHitBox())){
            drawRelative(baseE);
        }
        panel.g.dispose();
    }

    public void drawRelative(BaseEntity entity){
        

        //center
        center(entity);
    
        panel.g.drawImage(entity.getImage(),worldX,worldY,entity.getWidth(),entity.getHeight(),null);     //can remove width and height.
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
