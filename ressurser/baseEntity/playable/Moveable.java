package ressurser.baseEntity.playable;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import ressurser.baseEntity.Entity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.Vector;
import ressurser.main.GamePanel;

public class Moveable extends Entity {

    private Vector velocity = new Vector();
    private Vector direction = new Vector(1,1); 
    protected ArrayList<Vector> path = new ArrayList<>();

    private double movementSpeed = 1;
    private int directionIndex = 0;
    private HitBox interactionHitBox;

    public Moveable(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
       
        setImages();
    }


    public HitBox getHitboxInfront(){

        int movementX = (int)(1*direction.getX());
        int movementY = (int)(1*direction.getY());

        HitBox usingHB = getHitBox();
        usingHB.updateCoords();
        HitBox inFront = new HitBox(usingHB.x+movementX,usingHB.y+movementY,usingHB.width,usingHB.height);

        return inFront;
      
    }

    public HitBox setInteractIonHitBox(){

        int xVal = 0;
        int yVal = 0;

        if (directionIndex == 0){
            yVal = -50;
            xVal-= ((64/2)-(getHitBox().width/2));
        }
        else if (directionIndex == 1){
            xVal = getHitBox().width/2;
            yVal-= ((64/2)-(getHitBox().height/2));
        }
        else if (directionIndex == 2){
            //yVal = (int)getHitBox().getHeight();
            xVal-= ((64/2)-(getHitBox().width/2));
            
        }
        else if (directionIndex == 3){
            yVal-= ((64/2)-(getHitBox().height/2));
            xVal = -(64-(getHitBox().width/2));
        }

        HitBox usingHB = getHitBox();
        usingHB.updateCoords();
        HitBox inFront = new HitBox(usingHB.x+xVal,usingHB.y+yVal,64,64);
        return inFront;
      
    }


    public HitBox getInteractionHitBox(){
        if (interactionHitBox == null){
            interactionHitBox = setInteractIonHitBox();
        }
        return interactionHitBox;
    }

    @Override
    public void update(){
       
        move();
        updateAnimation();
        
    }

    private void updateAnimation() {

        //chechks for direction cahnges, might not be necacarry

        if (!direction.hasNoVelocity()){
            animationIndex +=2;
        if (animationIndex >= 60){
            animationIndex = 1;
        }
        }
        //Could check for directional changes..
        
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
        //resets interactionHitbox
        interactionHitBox = null;


        panel.camera.addbackendPrintData("velocity: "+String.valueOf(velocity));
        double movementX;
        double movementY;
        


        if (path.size()== 0){
            
            //double [] xy = velocity.transferValues(5);
            Vector movement =velocity.transfer(movementSpeed*4);
            double [] xy = {movement.x,movement.y};
            movementX = xy[0];
            movementY = xy[1];
            direction.set(movementX,movementY);
           
        

            if (!(isCollided(getHitboxInfront()))){
                panel.camera.addbackendPrintData("velocity: "+movementX+","+movementY);
                panel.camera.addbackendPrintData("speed: "+(panel.camera.FPS*Math.sqrt(movementX*movementX+movementY*movementY)));
               
                worldX += movementX;
                worldY += movementY;
               
                getHitBox().updateCoords();
            }
        }

        else{
            panel.camera.addbackendPrintData("path: true");
            Vector movement = path.get(0).transfer(movementSpeed*2);
            double [] xy = {movement.x,movement.y};
            

            
            movementX = xy[0];
            movementY = xy[1];
            panel.camera.addbackendPrintData("path velocity: "+path.get(0));
            
            
            
           
            if (!(isCollided(getHitboxInfront()))){
                direction.set(movementX,movementY);
             
                panel.camera.addbackendPrintData("speed: "+(panel.camera.FPS*Math.sqrt(movementX*movementX+movementY*movementY)));
               
                worldX += movementX;
                worldY += movementY;
               
                getHitBox().updateCoords();
            }

            

        
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
        boolean collision = panel.world.solidCollision(hitBox);
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

    private int getCorrespondingSpriteIndex() {
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


    public void interact() {
        throw new UnsupportedOperationException("Unimplemented method 'interact'");
    }

    
}
