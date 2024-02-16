package ressurser.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.awt.BasicStroke;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.primitiveEntity;
import ressurser.baseEntity.playable.Playable;
import ressurser.baseEntity.sprite.Sprite;
import ressurser.baseEntity.tile.Tile;
import ressurser.chunkSystem.Chunk;
import ressurser.main.GamePanel;

public class Camera extends primitiveEntity{

    private BaseEntity followed;
    

    public boolean drawingTimer = false;

    //for drawing right FPS in panel:
    public int FPS = 60;
    public long splitTime = 1000000000/FPS;
    public long nextDrawTime = System.nanoTime()+splitTime;
    ArrayList<String> backEndData = new ArrayList<>();

    public boolean testData = true;

    public Camera(GamePanel panel, String name, int worldX, int worldY, short width, short height) {
        super(panel, name, worldX, worldY, (short) panel.screenWidth,(short)(panel.screenHeight));
        //TODO Auto-generated constructor stub
        //follow(panel.spiller);
        follow(panel.player);
       
    }
    public Camera(GamePanel panel,String name){
        super(panel,name);
        //follow(panel.spiller);
        
        worldX = -50;
        worldY = -50;
        width = (short)panel.screenWidth;
        height = (short)panel.screenHeight;
        hitBox = new HitBox(this);
    }




    /**
     * follows entitiy until given other orders
     */
    public void follow(BaseEntity entity){
        //TODO
        this.followed = entity;

    }
    
    public void draw(Graphics g){
        long startDraw = System.nanoTime();
        
        
        centerAtPosition(followed.getPoint());
        
        
        Graphics2D g2 = (Graphics2D)g;
        g2.setFont(new Font("Arial", Font.PLAIN, 7));
        
        
        
       
        ArrayList<BaseEntity> withinCam = panel.chunkSystem.workingMemory.getvisibleEntities();
        
       //panel.chunkSystem.workingMemory.writeInfo();
        //this only works if chunkysstem updates entites frequently
       
        //draw tiles first - not yet implemented
        for (BaseEntity baseE :withinCam){
            if (baseE instanceof Tile){
            
                drawRelative(g2,baseE);
                if (testData){
                    drawHitBox(g2,baseE);
                    drawCoords(g2,baseE);
                } 
            }  
            
        }

        //draw entities later..
        for (BaseEntity baseE :withinCam){
            if (baseE instanceof Playable){
                drawRelative(g2,baseE);
                if (testData){
                   
                    drawHitBox(g2,baseE);
                    drawCoords(g2,baseE);
                }   
            } 
        }

        //drawing chunks
        if (testData){
            drawChunks(g2);
        }
        long endDraw = System.nanoTime();
        
        addbackendPrintData("drawtime: "+String.valueOf(endDraw-startDraw));
        drawBackEndData(g2);
        
        panel.g.dispose();
        
        
    }

    public void drawRelative(Graphics2D g2,BaseEntity entity){
        //TODO write functions in baseEntity and hitbox that makes it so you dont need to "draw relative" - could do it inside those functions.
       

        int x = entity.getWorldX()-worldX;
        int y = entity.getWorldY()-worldY;

        g2.setColor(Color.WHITE);
        g2.drawString(entity.getName(),x,y+15);
        //g2.drawImage(entity.getImage(),x,y,64,64,null);     //can remove width and height.
        ArrayList<BufferedImage> imagesCopy = new ArrayList<>(entity.getImages());
        for (BufferedImage img:imagesCopy){
            g2.drawImage(img,x,y,entity.getWidth(),entity.getHeight(),null);  
        }
        
        
    }
    private void drawChunks(Graphics2D g2){
        float lineWidth = 2;
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);
        ArrayList<Chunk> chunkCopy = new ArrayList<>(panel.chunkSystem.workingMemory.getChunks());
        for (Chunk chunk:chunkCopy){
            int x = chunk.getWorldX()-worldX;
            int y = chunk.getWorldY()-worldY;
            g2.setColor(Color.white);
            g2.drawRect(x,y,(int)chunk.getWidth(),(int)chunk.getHeight());     //can remove width and height.
        }
    }

    private void drawHitBox(Graphics2D g2,BaseEntity entity){
        int x = entity.getHitBox().getWorldX()-worldX;
        int y = entity.getHitBox().getWorldY()-worldY;

        int width = (int)entity.getHitBox().getWidth();
        int height = (int)entity.getHitBox().getHeight();
        g2.setColor(Color.green);
        float lineWidth = 0.5f;
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);
       
        
         g2.drawRect(x,y,width,height);
         if (entity instanceof Playable){
            Rectangle rect = ((Playable) entity).getHitboxInfront();
            g2.drawRect(rect.x-worldX,rect.y-worldY,rect.width,rect.height);
         }
    }

    private void drawCoords(Graphics2D g2,BaseEntity entity){
        int x = entity.getWorldX()-worldX;
        int y = entity.getWorldY()-worldY+30;

        int width = (int)entity.getWidth();
        int height = (int)entity.getHeight();
       
        g2.setColor(Color.red);
        
         g2.drawString("x: "+entity.getWorldX()+",y: "+entity.getWorldY(),x,y);
    }

    private void drawBackEndData(Graphics g2){
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        int y = 20;
        ArrayList<String> printables = new ArrayList<>(backEndData);
        for (String printData:printables){
           
            g2.drawString(printData,20,y);
            y += 25;
        }
        
        backEndData.clear();
        backEndData.clear();
    }


    /** 
     * only react if entity is not null
    */
    private void center(BaseEntity entity){
        if (entity != null){
            int entityX = entity.getWorldX();
            int entityY = entity.getWorldY();

            //get the startValues for x and y
            worldX = entityX-width/2;
            worldY = entityY+height/2;
        }   
        
    }
    //can add loads of other abilities

    public void moveX(int value){
        worldX+= value;
    }

    public void moveY(int value){
        worldY+= value;

    }

    public void addbackendPrintData(String newString){backEndData.add(newString);}
}
