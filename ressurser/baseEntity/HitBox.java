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
        super((int)(baseEntity.worldX)+relativeXPlusValue,(int)(baseEntity.worldY)+relativeYPlusValue,hitBoxWidth,hitBoxHeight);
        this.baseEntity = baseEntity;

        //relative values - the ditance from the objects coords to the hitboxs coords.
        this.relativeXValue = relativeXPlusValue;
        this.relativeYValue = relativeYPlusValue;
    }


    // not done.... 
    public HitBox(BaseEntity entity){
        super((int)(entity.worldX),(int)(entity.worldY),entity.width,entity.height);
    }

    /**
     * based on a hitbox without a connected entity.
     */
    public HitBox(double worldX,double worldY,int width, int height){
        super((int)worldX,(int)worldY,width,height);
    }


    /**
     * based on a connected rectangle.
     */
    public HitBox(Rectangle rect){
        //make rectangle
        super(rect);
    }




    /**return true if this hitbox either contains.
     * also returns false if given hitBox is the same as this hitbox
    */
    public boolean collision(HitBox hitbox){
        
        updateCoords();
        if (hitbox == this){return false;}
        return intersects(hitbox)||contains( hitbox);
    }

    public boolean collision(Point p){
        
        updateCoords();
        
        return contains( p);
    }

    public void updateCoords(){
        if (baseEntity != null){
            x = ((int)baseEntity.getWorldX())+relativeXValue;
            y = (int)baseEntity.getWorldY()+relativeYValue;
            
        }
    }

    


    public void move(int xMove,int yMove){
        x += xMove;
        y+= yMove;
    }

     /**updates the x coords in case they are wrong */
    public int getWorldX(){
        updateCoords();
        return x;
        
    }
    /**updates the y coords in case they are wrong */
    public int getWorldY(){
        updateCoords();
        return y;
    }




    public BaseEntity getEntity() {
        return baseEntity;
    }

    public void draw(Graphics2D g2 ,Spiller player){
        g2.drawRect((int)(getWorldX()-(player.worldX)+player.screenX),(int)(getWorldY()-(player.worldY)+player.screenY),width,height);
     
    }

    public Point getCenter(){
        return new Point((x+width/2),(y+height/2));
    }

    public HitBox getAlteredHitBox(double resizeValue,int right,int up){
        HitBox hitBox = new HitBox(getWorldX()+right,getWorldY()-up,(int)(width*resizeValue),(int)(height*resizeValue));
        return hitBox;

    }

    public HitBox getAlteredHitBox(int right,int up,int left,int down){
        HitBox hitBox = new HitBox(getWorldX()-left,getWorldY()-up,(int)(width+left+right),(int)(height+up+down));
        return hitBox;
    }

    

    
}
