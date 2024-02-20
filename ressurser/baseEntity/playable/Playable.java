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

    private Vector velocity = new Vector();
    private Vector direction = new Vector(1,1); 
    private ArrayList<Vector> path = new ArrayList<>();

    
    private int directionIndex = 0;

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
        updateAnimation();
        
    }

    private void updateAnimation() {


        int oldX = (int)direction.getX();
        int oldY = (int)direction.getY();

       
        
        //chechks for direction cahnges, might not be necacarry
        if (oldX == direction.getX() && oldY == direction.getY() && !(oldX== 0 && oldY== 0)){
            animationIndex +=2;
            if (animationIndex >= 60){
                animationIndex = 1;
            }

            
        } else {
            animationIndex = 0;
        }
        if (direction.getX()>0){
                //moves right
                directionIndex = 1;
            
        } else if (direction.getX()<0){
                //moves left
                directionIndex = 3;
        } else if(direction.getY()<0){
            //moves down
            directionIndex = 0;

        } else if(direction.getY()>0){
            //moves up
            directionIndex = 2;
            
        } else {
            //no movement
            //no need to change
        }

        panel.camera.addbackendPrintData("direction: "+String.valueOf(direction));
       
        
       
    }


    

    /**not used.. */
    public void move() {
        


        if (path.size()== 0){
            direction.set(Vector.normalize(velocity.getX()),Vector.normalize(velocity.getY()));
            int movementX = (int)velocity.transferX(2);
            int movementY = (int)velocity.transferY(2);
        

            if (!(isCollided(getHitboxInfront()))){

                worldX += movementX;
                worldY += movementY;
                getHitBox().updateCoords();
            }
        }
        else{
            direction.set(Vector.normalize(path.get(0).getX()),Vector.normalize(path.get(0).getY()));
            int movementX = (int)path.get(0).transferX(2);
            int movementY = (int)path.get(0).transferY(2);
            System.out.println(path.get(0));
        
            if (path.get(0).hasNoVelocity()){
                path.remove(0);
            }

            if (!(isCollided(getHitboxInfront()))){

                worldX += movementX;
                worldY += movementY;
                getHitBox().updateCoords();
            }
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
        int index = (directionIndex)*3;
        if (index <0){index = 0;}
        

        int index2 = 0;

        if (animationIndex == 0){
            
        } else if (animationIndex<30){
            index2 = 1;
        } else {
            index2 = 2;
        }
        

        return index +index2;// + animationIndex/20;


        
    }

    public Vector getVelocity(){
        return velocity;
    }


    public void setPath(ArrayList<Vector> newPath) {
        path = newPath;
    }
    public void addPath(ArrayList<Vector> newPath){
        path.addAll(newPath);
    }

    
}
