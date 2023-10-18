package ressurser.entity.spiller;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.IOException;

import javax.imageio.ImageIO;

import ressurser.baseEntity.HitBox;
import ressurser.entity.Entity;
import ressurser.main.GamePanel;
import ressurser.meny.items.Item;

public class Spiller extends Entity{
    
   
    

   
    
    
    public  int screenX;
    public  int screenY;
    
    
    public boolean interact = false;
    public boolean bike,boat,axe,shovel,hoe,bucket,hammer;
   
    public String activeMaterial = "null";
    public String activeTool = "null";
    public Item activeItem = null;
    public boolean cheat = false;

    public int venteVerdi = 0;



    


    public Spiller(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
            
        hentBilde();
        aktivtbilde = ned1;
        boat = true;

        //hitBox = new HitBox(this,40,40,width-40/2,height-40);

        
    }

    

    public void hentBilde(){
        try{
        opp1 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerStandingBack1.png"));
        opp2 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerBackNew2.png"));
        opp3 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerBackNew3.png"));


        ned1 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerDown.png"));
        ned2 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerDown2.png"));
        ned3 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerDown3.png"));

        venstre1 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerLeftStanding1.png"));
        venstre2 = ImageIO.read(getClass().getResourceAsStream("playerSprites/revidertPlayerLeft2.png"));
        venstre3 = ImageIO.read(getClass().getResourceAsStream("playerSprites/revidertPlayerLeft3.png"));

        hoyre1 = ImageIO.read(getClass().getResourceAsStream("playerSprites/standingRight1.png"));
        hoyre2 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerRight2.png"));
        hoyre3 = ImageIO.read(getClass().getResourceAsStream("playerSprites/playerRight3.png"));

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    

    public void draw(Graphics2D g2){

        //getSprite();
        
        //g2.drawString(Integer.toString(hitBox.x),100,100);
        g2.drawImage(aktivtbilde,screenX,screenY-height/2,this.width,this.height,null);
    }


    /**
     * check if object in front of player
     * also checks if player should move or turn
     * might need to be changed
     */
    private void getAction(String ret){  //legg inn ret
        
        coorectCoordinates();
       
        
        if (retning != ret){venteVerdi = 4;retning = ret;}
        if (!panel.collisionC.playerCollision()){
                if (retning == ret && venteVerdi <= 0){
                    move = true; retning = ret;
                } else if (retning == ret){
                    venteVerdi --;
                } else {
                    //retning = ret;venteVerdi = 16/fart;
                }
        } 
    }


    //change all pMove til move

    /*
     
    
    

    public void updateAction(){
        if (!move &&panel.objM.getObjInteractable()){
            if (panel.objM.getObject().alwaysInteract){
                panel.objM.getObject().interact();
            }
        }
        //should be changed to input class
         if ((panel.input.upPressed||panel.input.downPressed||panel.input.leftPressed||panel.input.rightPressed||panel.input.enterPressed||panel.input.mPressed) && !(move)){
             inputGetAction();       //endrer retning, gjør move true //
            
         }
        
        if (move) {
            move();
        }
    }
     */

    



    //has to be changed
    //should not need to check all four, shold be a value thas is changed by input class
    private void inputGetAction(){  //spillerklassen
        
        if (panel.input.upPressed){
            getAction("opp");
           
        }
        else if (panel.input.downPressed){
            getAction("ned");
        }
        else if (panel.input.leftPressed ){
            getAction("venstre");
        }
        else if (panel.input.rightPressed){
            getAction("hoyre");
        }
    }
    
    public void equipBike(){
        //fart = 20;
        cheat  = true;
        bike = true;
    }

    public void unEquipBike(){
        //fart = 2;
        bike = false;
        
    }

    public void equipBoat(){
        if (panel.tileM.getTile(worldX,worldY,retning).water){
            move = true;
            boat = true;
        }
    }

    public void unEquipBoat(){
        if (!panel.tileM.getTile(worldX,worldY,retning).water){
            move = true;
            boat = false;
        }
    }

    public void unEquipAxe() {
        axe = false;
        activeTool = "null";
    }

    public void equipAxe() {
        activeTool = "axe";
        axe = true;
    }

    public void unEquipShovel() {
        shovel = false;
        activeTool = "null";
    }

    public void equipShovel() {
        shovel = true;
        activeTool = "shovel";
    }

    public void unEquipHoe() {
        hoe = false;
        activeTool = "null";
    }

    public void equipHoe() {
        hoe = true;
        activeTool = "hoe";
    }
    
    public void unEquipHammer() {
        hammer = false;
        activeTool = "null";
    }

    public void equipHammer() {
        hammer = true;
        activeTool = "hammer";
    }


    private void coorectCoordinates(){
        
        if (worldX%panel.tileSize!=0 && !move){
           
            worldX = panel.objM.getNearest32(worldX);
            
        }
        if (worldY%panel.tileSize!=0){
           
            worldY = panel.objM.getNearest32(worldY);
            
        }
        
    }
    public void changeCoordinates(int newX,int newY){
        worldX = newX;
        worldY = newY;
        move = true;
    }

    @Override
    public void updateAction(){
        
        //System.out.println("x:"+xMovement);

        
       
        updateSpriteCounter();
        getSprite();
        movePlayer();
        

       
    }

    private boolean inputPressed(){
        System.out.println(panel);
        return panel.input.upPressed||panel.input.downPressed||panel.input.leftPressed||panel.input.rightPressed;
    }

    private void movePlayer(){
       
        if (inputPressed() ){
            //!collision()
            System.out.println(panel.input.upPressed+","+panel.input.downPressed+","+panel.input.leftPressed+","+panel.input.rightPressed);
            System.out.println(retning);
            calculateMovementAndMove();
        } else {
            move = false;
        }

    }
    
    private void calculateMovementAndMove(){
        int xMove = 0;
        int yMove = 0;

        if (panel.input.upPressed){
            yMove -= speed;
        }
        if (panel.input.downPressed){
            yMove += speed;
        }
        if (panel.input.leftPressed){
            xMove -= speed;
        }
        if (panel.input.rightPressed){
            xMove += speed;
        }

        //if two or more forces
        if (xMove != 0 && yMove != 0){
            int result = speed * speed; // Calculate 5 squared
            int x = (int) Math.sqrt(result / 2);

            xMove /= speed;
            xMove *= x;

            yMove /= speed;
            yMove *= x;

            if (xMove >0)
                {retning = "hoyre";
            }
            else {
                retning = "venstre";
            }



        } else { 
            //if horizontal movement
            if (xMove != 0){
                if (xMove<0){
                    retning = "venstre";
                } else {retning = "hoyre";}

            //if vertical movement
            } else {
                if (yMove<0){
                    retning = "opp";
                } else {
                    retning = "ned";
                }
            }
        }

        worldX += xMove;
        worldY += yMove;

       // hitBox.move(xMove,yMove);

        if (xMove != 0 ||yMove != 0){
            move = true;
        }

    }
    
    public boolean collision(){
        return panel.collisionC.collisionAllEntities(this);
    }


}
    


