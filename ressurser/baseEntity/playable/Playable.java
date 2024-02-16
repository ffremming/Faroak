package ressurser.baseEntity.playable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import ressurser.baseEntity.Entity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.Vector;
import ressurser.main.GamePanel;

public class Playable extends Entity {

    Vector velocity = new Vector();
    Vector direction = new Vector(1,1);
    ArrayList<BufferedImage> images = new ArrayList<>();

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
       
        setImages();
    }


    public HitBox getHitboxInfront(){

        int movementX = (int)direction.getX();
        int movementY = (int)direction.getY();

        HitBox usingHB = getHitBox();
        HitBox inFront = new HitBox(usingHB.x+movementX,usingHB.y+movementY,usingHB.width,usingHB.height);

        return inFront;
      
    }

    @Override
    public void update(){
        
        move();
    }

    public Vector getVelocity(){
        return velocity;
    }

    /**not used.. */
    public void move() {
        direction.set(Vector.normalize(velocity.getX()),Vector.normalize(velocity.getY()));
        int movementX = (int)velocity.transferX(2);
        int movementY = (int)velocity.transferY(2);
       

        if (!(isCollided(getHitboxInfront()))){

            worldX += movementX;
            worldY += movementY;
            getHitBox().updateCoords();
        }
        
    }

    /**moves hitbox and checks for collisions, moves hitbox back */
    private boolean isCollided(HitBox hitBox) {

        //TODO THIS MUST BE TESTED THOUROGLY
       
        boolean collision = panel.chunkSystem.workingMemory.solidCollision(hitBox);
        panel.camera.addbackendPrintData("collision: "+String.valueOf(collision));
        return collision;
    }

    public void addVelocity(Vector newVector){
        velocity.add(newVector);
        
    }

    public void setVelocity(Vector newVector){
        velocity = (newVector);
    }

    private void setImages(){
        images = panel.imageContainer.setPlayableImages(getName());
    }

    /**returns the one image that is supposed to be returned */
    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> arr = new ArrayList<>();
        if (images.size()>0){
            arr.add(images.get(getCorrespondingSpriteIndex()));
        }
        return arr;
    }

    private int getCorrespondingSpriteIndex() {//TODO
        return 0;
    }

    
}
