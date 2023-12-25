package ressurser.objects;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ressurser.baseEntity.HitBox;
import ressurser.entity.spiller.Spiller;
import ressurser.main.GamePanel;

public class SuperObject {

    public BufferedImage image,image1;
    public boolean collision = true;
    public boolean animated = false;
    public boolean interactable = true;
    protected GamePanel panel;
    Spiller spiller;
    ObjectManager objM;
    public boolean alwaysInteract = false;
    public String name;

    public boolean hammerBreakable = false;
    public boolean shovelBreakable = false;
    public boolean axeBreakable = false;
    public boolean hoeBreakable = false;
    public boolean pickaxeBreakable = false;

    public boolean yieldable = false;
    public boolean materialYield = false;
    public boolean resourceYield = false;

    public String yieldType = "null";
    protected int smashCounter = 0;
    public String type = "null";
    public int durability = 0;

    int collisionHeight = 1;
    int collisionWidth = 1;
    int pixlePlusYvalue = 0;
    int pixlePlusXvalue = 0;

    public int worldX = 0;
    public int worldY = 0;

    String directionCollision = "null";

    SuperObject [] connectedCells;

    String mapString;

    public HitBox hitBox;

    int width = 1*64;
    int height = 1*64;


   

    public SuperObject(GamePanel gp, ObjectManager objM,int worldX,int worldY,String name,String type){
        this.panel = gp;
        this.spiller = panel.spiller;
        this.objM = objM;
        this.name = name;
        this.worldX = worldX;
        this.worldY = worldY;
        this.type = type;

        //this.hitBox = new HitBox(this,width ,height,0,0);

       
       


        connectedCells= new SuperObject [30];

        
        
    }

    public SuperObject(GamePanel gp, ObjectManager objM,int worldX,int worldY,String name,String type,int width, int height){
        this.panel = gp;
        this.spiller = panel.spiller;
        this.objM = objM;
        this.name = name;
        this.worldX = worldX;
        this.worldY = worldY;
        this.type = type;
        this.collisionWidth = width;
        this.collisionHeight = height;

        //this.hitBox = new HitBox(this,width*64,height*64,0,0);

        connectedCells= new SuperObject [30];

        if (collisionWidth == 2){
          
           //SuperObject o = objM.factory.createGameObject(worldX+32,worldY,"dead","ghost",0,0,1,1);
           GhostObj o = new GhostObj(panel,objM,worldX+32,worldY,"ghost","ghost",this);
           connectedCells[0] = o;
           o.interactable = true;
           objM.map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][(worldY)/32][(worldX+32)/32] = o;
        }
        
        
    }


    protected void readImages(){
        
        image1 = objM.objectSprites.get(name);
        
        
        if (image1 == null){
            
            try {
                image1 = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+name+".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        
            
        
        //image1 = ImageIO.read(getClass().getResourceAsStream("objectSprites/superObjects/"+name+".png"));
        image = image1;
    }

    public void interact(){
    }

    public void continueInteraction() {
    }

    

    public String getType(){
        return type;
    }
    public void changeImage(int i){

    }

    public void smash() {
    }

    public void enterInteract() {
        
        panel.gameState = panel.DIALOGSTATE;
        panel.textString = "looks like a "+name;
        panel.textBox = true;
    }

    public void enterContinueInteraction() {
        panel.textBox = false;
        panel.gameState = panel.PLAYSTATE ;
    }

    public void setConnectedCells(int width,int height){
        if (width == 2){
          
            //SuperObject o = objM.factory.createGameObject(worldX+32,worldY,"dead","ghost",0,0,1,1);
            GhostObj o = new GhostObj(panel,objM,worldX+32,worldY,"ghost","ghost",this);
            connectedCells[0] = o;

            if (worldY/32 < panel.mapH.mapHeight&& (worldX+32)/32 < panel.mapH.mapWidth&& worldY/32 >= 0 &&(worldX+32)/32>= 0){
                objM.map[panel.mapH.activeMapType][panel.mapH.activeMapNumber][(worldY)/32][(worldX+32)/32] = o;
            }
            
         }
    }

    /**
     * removes a superobject based on their x and y
     * does not remove ghostobjs.
     */

    public void remove(){
        objM.map [panel.mapH.activeMapType][panel.mapH.activeMapNumber][worldY/32][worldX/32] = null;
    }

}

