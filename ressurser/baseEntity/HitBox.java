package ressurser.baseEntity;


import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import ressurser.entity.spiller.Spiller;



public class HitBox extends Rectangle{

    //has height and width
    
    BaseEntity baseEntity = null;
   
    int relativeXValue,relativeYValue = 0;

    //hitbox could be based of not an x and y value, but the ditance from the entityies x and y.

    final int STANDARD1X1 = 0;
    final int CHARACHTER = 1;
    final int STANDARD2X2 = 2;

    //might need some weird non rectangular.





    /**
     * based on a connected entity.
     * does not want this solution, try to phase out.
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
        super(entity.worldX,entity.worldY,entity.width,entity.height);
    }

    /**
     * based on a hitbox without a connected entity.
     */
    public HitBox(int worldX,int worldY,int width, int height){
        super(worldX,worldY,width,height);
    }


    /**
     * based on a connected rectangle.
     */
    public HitBox(Rectangle rect){
        //make rectangle
        super(rect);
    }




    public boolean collision(HitBox hitbox){
        
        updateCoords();

        return contains( hitbox)||intersects(hitbox);
    }

    void updateCoords(){
        x = getWorldX();
        y = getWorldY();
    }

    


    public void move(int xMove,int yMove){
        x += xMove;
        y+= yMove;
    }

     /**updates the x coords in case they are wrong */
    public int getWorldX(){
        if (baseEntity!= null){
            return baseEntity.worldX+relativeXValue;
        } else {
            return x;
        }
        
    }
    /**updates the y coords in case they are wrong */
    public int getWorldY(){
        if (baseEntity!= null){
            return baseEntity.worldY+relativeYValue;
        } else {
            return y;
        }
    }




    public BaseEntity getEntity() {
        return baseEntity;
    }

    public void draw(Graphics2D g2 ,Spiller player){
        g2.drawRect(getWorldX()-(player.worldX)+player.screenX,getWorldY()-(player.worldY)+player.screenY,width,height);
     
    }

    public Point getCenter(){
        return new Point((x+width/2),(y+height/2));
    }
    
    
}
