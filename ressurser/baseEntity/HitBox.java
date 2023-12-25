package ressurser.baseEntity;


import java.awt.Graphics2D;
import java.awt.Rectangle;

import ressurser.entity.spiller.Spiller;



public class HitBox extends Rectangle{

    //has height and width
    
    BaseEntity baseEntity;
    int worldX,worldY;
    int relativeXValue,relativeYValue;

    //hitbox could be based of not an x and y value, but the ditance from the entityies x and y.

    final int STANDARD1X1 = 0;
    final int CHARACHTER = 1;
    final int STANDARD2X2 = 2;

    //might need some weird non rectangular.





    /**
     * based on a connected entity.
     */
    public HitBox(BaseEntity baseEntity,int hitBoxWidth,int hitBoxHeight,int relativeXPlusValue,int relativeYPlusValue){
        //make rectangle
        super(baseEntity.worldX+relativeXPlusValue,baseEntity.worldY+relativeYPlusValue,hitBoxWidth,hitBoxHeight);
        this.baseEntity = baseEntity;

        //relative values - the ditance from the objects coords to the hitboxs coords.
        this.relativeXValue = relativeXPlusValue;
        this.relativeYValue = relativeYPlusValue;
    }


    // not done.... 
    public HitBox(BaseEntity entity){
        super(entity.worldX,entity.worldY,entity.panel.tileSize,entity.panel.tileSize);
    }



    /**
     * based on a hitbox without a connected entity.
     */
    public HitBox(int hitBoxWidth,int hitBoxHeight,int worldX,int worldY){
        //make rectangle
        super(worldX,worldY,hitBoxWidth,hitBoxHeight);
    }



    /**
     * based on a connected rectangle.
     */
    public HitBox(Rectangle rect){
        //make rectangle
        super(rect);
    }




    public boolean collision(HitBox hitbox){
        x = getWorldX();
        y = getWorldY();
        return contains( hitbox)||intersects(hitbox);
    }

    


    public void move(int xMove,int yMove){
        x += xMove;
        y+= yMove;
    }

    public int getWorldX(){
        return baseEntity.worldX+relativeXValue;
    }

    public int getWorldY(){
        return baseEntity.worldY+relativeYValue;
    }




    public BaseEntity getEntity() {
        return baseEntity;
    }

    public void draw(Graphics2D g2 ,Spiller player){
        

        g2.drawRect(getWorldX()-(player.worldX)+player.screenX,getWorldY()-(player.worldY)+player.screenY,width,height);
     
    }
}
