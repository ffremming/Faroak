package ressurser.entity;

import java.awt.image.BufferedImage;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;
import ressurser.entity.spiller.Spiller;
import ressurser.main.GamePanel;



public class Entity extends BaseEntity{
    
    public int speed = 6;
    protected int xMovement;
    public int yMovement;

    //image sprites:
    public BufferedImage opp1,opp2,opp3,ned1,ned2,ned3,venstre1,venstre2,venstre3,hoyre1,hoyre2,hoyre3;
    public BufferedImage aktivtbilde;

    //direction - should cahnge name.
    public String retning = "opp";
    public boolean move;
    

    protected int spriteCounter = 0;
    protected int frameCounter = 0;
    
    protected int moveValue = 0;
    public HitBox hitBox;

    //not yet implemented:
    protected int activeDirection;
    protected int UP = 1;
    protected int DOWN = 3;
    protected int RIGHT = 2;
    protected int LEFT = 4;

    int [] DirectionalVector = new int [2];

    

    public Entity(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
            }
    /**
     * updates sprite counter.
     * coudl make it so entityHandler updates all
     */
    //should maybe be changed.

    public void updateSpriteCounter() {      //skal kjøres hver gang spiller er i aktivitet
        if (move){
            frameCounter++;
        
        if (frameCounter % 6 == 0){        //aktiveres hver 16
            spriteCounter ++;
            frameCounter = 0;
            spriteCounter = spriteCounter % 2;
        }
        } else {
            frameCounter = 0;
            spriteCounter = 0;
        }
        
    }

    

    protected void getSprite(){
        
        if (retning == "ned"){
            if (move){
                if (spriteCounter== 1){
                    aktivtbilde = ned3;
                } else {
                    aktivtbilde = ned2;
                }
            } else {
                aktivtbilde = ned1;
            }
        } else if (retning == "opp"){
            if (move){
                if (spriteCounter== 1){
                    aktivtbilde = opp3;
                } else {
                    aktivtbilde = opp2;
                }
            } else {
                aktivtbilde = opp1;
            }
        } else if (retning == "hoyre"){
            if (move){
                if (spriteCounter== 1){
                    aktivtbilde = hoyre3;
                } else {
                    aktivtbilde = hoyre2;
                }
            } else {
                aktivtbilde = hoyre3;
            }
        } else if (retning == "venstre"){
            if (move){
                if (spriteCounter== 1){
                    aktivtbilde = venstre3;
                } else {
                    aktivtbilde = venstre2;
                }
            } else {
                aktivtbilde = venstre3;
            }
        }
    }


    /**
     * moves entity. should be run as long ass the entitys PMove is true;
     * maybe implement collisionchecker here
     * makes sure entity only moves 32 pix.
     */
    public void move(){
       
        updateSpriteCounter();
        getSprite();
            moveEntity(retning);
            moveValue++;

            if (moveValue== 32/speed) {
                move = false;
                moveValue = 0;
            }
    }

    private void moveEntity(String dir){
        if (move){ 
            if (dir.equals("opp")){ retning = "opp";

                worldX-=speed;

            } else if (dir.equals("ned")){ retning = "ned";
                worldY+=speed;

            } else if (dir.equals("hoyre")){ retning = "hoyre";
                worldX+=speed;

            } else if (dir.equals("venstre")){ retning = "venstre";
                worldX-=speed;
            }
        } 
    }
    public void updateAction(){
        
        //System.out.println("x:"+xMovement);
       
        updateSpriteCounter();
        getSprite();
        
       
    }

    

    

   


    public void resetYMovement() {
        yMovement = 0;
    }
    public void resetXMovement() {
        xMovement = 0;
    }

    private boolean hasMovementLeft(){
       return (DirectionalVector[1] != 0 || DirectionalVector[0] != 0);
       
    }


    public boolean collision(HitBox hb){
        return hitBox.collision(hb);
    }
}
