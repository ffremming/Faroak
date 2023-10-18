package ressurser.entity.NPC;

import java.io.IOException;

import javax.imageio.ImageIO;

import ressurser.entity.Entity;
import ressurser.main.CollisionChecker;
import ressurser.main.GamePanel;
import ressurser.objects.ObjectManager;

import java.awt.Graphics2D;
public class NPC extends Entity{
    int npcMoveValue= 0;
    boolean npcMove = false;
    int moveCount = 0;
    protected int bredde = 32;
    protected int hoyde = 64;
    double speed;
    ObjectManager objM;
    CollisionChecker collisionC;
    String streng1,streng2,streng3;
    public int antStreng = 0;
    String [] dialoge;
    public Thread gameThread;
    int currentString = 0;
    boolean option = false;
    String questionString;
    

    public NPC(GamePanel panel,String name,int worldX,int worldY,short width,short height,short hbWidth, short hbHeight,short relativeXValue,short relativeYValue){
        super(panel, name, worldY, worldY, width, height, hbWidth, hbHeight, relativeXValue, relativeYValue);
        speed = 2;
        this.worldX = worldX;
        this.worldY = worldY;
        getImage();
        objM = panel.objM;
        collisionC = panel.collisionC;
        
        dialoge = new String[antStreng];
        
        
        
        
    }

    public void updateNPC(){
        
       
        if (npcMove){
            NPCBeveg();
        } else {
       
        if (panel.animatedSpriteCounter%8 == 0){npcMoveValue++;
            if (npcMoveValue >= 1){
                if (Math.random() < 0.35){
                    if (Math.random() < 0.50){
                        if (!(collisionC.checkOutOfBounds(worldX, worldY,retning)||collisionC.NPCCollidedPLayer(worldX,worldY,retning))){
                            if (!(objM.getObjCollisionNPC(worldX,worldY,retning)||(panel.tileM.getTile(worldX,worldY,retning)).collision)){
                                npcMove = true;
                            }
                        }
                        
                        
                        
                    } else {
                        byttRetning();
                    }
                }
            }
        }
        }
    }

    public void NPCBeveg(){
        
        if (npcMove){
            

            if (retning.equals("opp")){ 
                worldY-=speed;
                

            } else if (retning.equals("ned")){ 
                worldY+=speed;

            } else if (retning.equals("hoyre")){ 
                worldX+=speed;

            } else if (retning.equals("venstre")){
                worldX-=speed;
            }

            moveCount++;
            if (moveCount >= 16){
                npcMove = false;
                moveCount = 0;
            }
        }
    }


    public void getImage(){
        try{
        String folderName = "NPCSprites";
        //ned1 = ImageIO.read(getClass().getResourceAsStream(folderName+"/playerDown.png"));
        //ned2 = ImageIO.read(getClass().getResourceAsStream(folderName+"/playerDown2.png"));
        //ned3 = ImageIO.read(getClass().getResourceAsStream(folderName+"/playerDown3.png"));

        opp1 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCStandingBack1.png"));
        opp2 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCWalkingBack1.png"));
        opp3 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCWalkingBack2.png"));

        venstre1 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCStandingLeft1.png"));
        venstre2 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCWalkingLeft1.png"));
        venstre3 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCWalkingLeft2.png"));

        hoyre1 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCStandingRight1.png"));
        hoyre2 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCWalkingRight1.png"));
        hoyre3 = ImageIO.read(getClass().getResourceAsStream(folderName+"/NPCWalkingRight2.png"));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void byttRetning(){
        if (Math.random() < 0.25 ){
            retning = "hoyre";
        } else if (Math.random() < 0.5){
            retning = "opp";
        } else if (Math.random() < 0.75 ){
            retning = "ned";
        } else {retning = "venstre";}
    }

    public void draw(Graphics2D g2){
        getSprite();
        //g2.drawImage(aktivtbilde,worldX,worldY-panel.tileSize,bredde,hoyde,null);
         
            //finner posisjon på skjermen som tilesene skal tegnes. 
        int screenX = (worldX)-(panel.spiller.worldX)+panel.spiller.screenX;
        int screenY = (worldY )-(panel.spiller.worldY)+panel.spiller.screenY;

            //tegner bare tiles som blir brukt:
            if ((screenY + panel.tileSize > 0 && screenX+panel.tileSize> 0)&&((screenY - panel.tileSize < panel.skjermHoyde)&&(screenX - panel.tileSize < panel.skjermBredde))){
                
                //if ( tegn == 0){tile[map[worldRow][worldCol]].image = hentBildeVann(worldRow,worldCol);}
                g2.drawImage(aktivtbilde,screenX,screenY-32 ,bredde,hoyde,null);
               
            
            }
    }

    public void getSprite(){

        switch (retning){
        case "ned":if (npcMove){ if (panel.animatedSpriteCounter == 1) {aktivtbilde = ned3;}else{aktivtbilde = ned2;}} else {aktivtbilde =  ned1;}break;
        
        case "opp":if (npcMove){ if (panel.animatedSpriteCounter == 1) {aktivtbilde = opp3;}else{aktivtbilde = opp2;}} else {aktivtbilde =  opp1;}break;

        case "venstre":if (npcMove){ if (panel.animatedSpriteCounter == 1) {aktivtbilde = venstre3;}else{aktivtbilde = venstre2;}} else {aktivtbilde =  venstre1;}break;

        case "hoyre":if (npcMove){ if (panel.animatedSpriteCounter == 1) {aktivtbilde = hoyre3;}else{aktivtbilde = hoyre2;}} else {aktivtbilde =  hoyre1;}break;
        }
    }

    public void interact(){
        if (antStreng > 0){
            setDialoge();
            panel.UI.dialogueString = dialoge[currentString];
            
        
        } else if (option){
            setDialoge();
            question();
        }
    }

    public void continueInteraction(){
        
        if (currentString+1 < antStreng ){
            currentString++;
            panel.UI.dialogueString = dialoge[currentString];

        } else if (option){
                
                question();
        } else {
            //avslutter hvis ikke option eller flere stringer å si
            finishInteraction();
        }
    }
    
    public void question(){
        setQuestion();
        panel.UI.dialogueString = questionString;
    }

    public void answer(boolean svar){
        if (svar){
            //gjør det du skal hvis yes
        
        } else {
            //gjør det du skal hvis no
        }
        finishInteraction();
    }

    
    private void finishInteraction(){
        panel.gameState= panel.PLAYSTATE ;panel.dialoge = false;currentString = 0;
        panel.gameOption = false;
    }

    private void setDialoge(){
        panel.gameState= panel.DIALOGSTATE;panel.dialoge = true;currentString = 0;
    }

    private void setQuestion(){
        panel.gameOption = true;
    }
}
