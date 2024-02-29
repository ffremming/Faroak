package ressurser.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Entity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.primitiveEntity;
import ressurser.baseEntity.playable.Moveable;

import ressurser.chunkSystem.Chunk;
import ressurser.main.GamePanel;
import ressurser.main.GUIMenu.ItemContainer;

public class Camera extends primitiveEntity{

    private BaseEntity followed;
    

    public boolean drawingTimer = false;

    //for drawing right FPS in panel:
    public int FPS = 60;
    public long splitTime = 1000000000/FPS;
    public long nextDrawTime = System.nanoTime()+splitTime;
    ArrayList<String> backEndData = new ArrayList<>();

    public boolean testData = false;

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
        
        if (followed == null){
            centerAtPosition(new Point(0,0));
        }else{
            centerAtPosition(followed.getPoint());
        }
        
        
        GraphicsConfiguration gc = ((Graphics2D) g).getDeviceConfiguration();
        int width = panel.getWidth(); // Assuming getWidth() returns the width of your drawing area
        int height = panel.getHeight(); // Assuming getHeight() returns the height of your drawing area
        BufferedImage image = gc.createCompatibleImage(width, height);
        BufferedImage image2 = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        
        // Draw on the compatible image
        Graphics2D g2 = image2.createGraphics();

        
        //Graphics2D g2 = (Graphics2D)g;
        g2.setFont(new Font("Arial", Font.PLAIN, 7));
        
       
        ArrayList<BaseEntity> withinCam = panel.chunkSystem.workingMemory.getvisibleEntities();
        
        addbackendPrintData("amount entities visible: "+String.valueOf(withinCam.size()));
        //panel.chunkSystem.workingMemory.writeInfo();
        //this only works if chunkysstem updates entites frequently
       
        //draw tiles first - not yet implemented
        for (BaseEntity baseE :panel.chunkSystem.workingMemory.getVisibleTiles()){
            drawRelative(g2,baseE);
            if (testData){
                drawHitBox(g2,baseE);
                drawCoords(g2,baseE);
            } 
        }

        //draw entities later..
        for (BaseEntity baseE :withinCam){
            if (baseE instanceof Entity){
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

        
        drawObjectData(g2);
        long endDraw = System.nanoTime();
        drawHighlightetHitbox(g2,panel.chunkSystem.workingMemory.hoveredEntity);
        addbackendPrintData("drawtime ms: "+String.valueOf((endDraw-startDraw)/1000000));
        addbackendPrintData("remaining cap: "+String.valueOf((1000000000-(((endDraw-startDraw)*60)))));
        addbackendPrintData("UI: "+panel.container.width +","+ panel.container.height);
        drawBackEndData(g2);

        //panel.UI.draw(g2);
        panel.container.draw(g2);
        
        
        // BufferedImage optimized  = toCompatibleImage(image2); LESS EFFECTIVE
        g.drawImage(image2, 0, 0, null);
        g2.dispose();
    }

    public void drawRelative(Graphics2D g2,BaseEntity entity){
        //TODO write functions in baseEntity and hitbox that makes it so you dont need to "draw relative" - could do it inside those functions.
       

        
        if (getHitBox().intersects(entity.getWorldX(),entity.getWorldY(),entity.getWidth(),+entity.getHeight()) ){

            int x = (int)(entity.getWorldX()-worldX);
            int y = (int)(entity.getWorldY()-worldY);

            if (testData){
                g2.setColor(Color.WHITE);
                g2.drawString(entity.getName(),x,y+15);
            }
            
            //g2.drawImage(entity.getImage(),x,y,64,64,null);     //can remove width and height.
            ArrayList<BufferedImage> imagesCopy = new ArrayList<>(entity.getImages());
            for (BufferedImage img:imagesCopy){
                g2.drawImage(img,x,y,entity.getWidth(),entity.getHeight(),null);  
                
            }
        }

        
    }

    private void drawChunks(Graphics2D g2){
        float lineWidth = 2;
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);
        ArrayList<Chunk> chunkCopy = new ArrayList<>(panel.chunkSystem.workingMemory.getChunks());
        for (Chunk chunk:chunkCopy){
            int x = (int)(chunk.getWorldX()-worldX);
            int y = (int)(chunk.getWorldY()-worldY);
            g2.setColor(Color.white);
            g2.drawRect(x,y,(int)chunk.getWidth(),(int)chunk.getHeight());     //can remove width and height.
        }
    }

    private void drawHitBox(Graphics2D g2,BaseEntity entity){
        int x = (int)(entity.getHitBox().getWorldX()-worldX);
        int y = (int)(entity.getHitBox().getWorldY()-worldY);

        int width = (int)entity.getHitBox().getWidth();
        int height = (int)entity.getHitBox().getHeight();
        g2.setColor(Color.green);
        float lineWidth = 0.5f;
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);
       
        
         g2.drawRect(x,y,width,height);
         if (entity instanceof Moveable){
            Rectangle rect = ((Moveable) entity).getHitboxInfront();
            g2.drawRect((int)(rect.x-worldX),(int)(rect.y-worldY),rect.width,rect.height);
         }
    }

    private void drawHighlightetHitbox(Graphics2D g2,BaseEntity entity){
        int x = (int)(entity.getHitBox().getWorldX()-worldX);
        int y = (int)(entity.getHitBox().getWorldY()-worldY);

        int width = (int)entity.getHitBox().getWidth();
        int height = (int)entity.getHitBox().getHeight();
        g2.setColor(Color.white);
        float lineWidth = 2f;
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);
       
        
         g2.drawRect(x,y,width,height);
         if (entity instanceof Moveable){
            Rectangle rect = ((Moveable) entity).getHitboxInfront();
            g2.drawRect((int)(rect.x-worldX),(int)(rect.y-worldY),rect.width,rect.height);
         }
    }

    private void drawCoords(Graphics2D g2,BaseEntity entity){
        int x = (int)(entity.getWorldX()-worldX);
        int y = (int)(entity.getWorldY()-worldY+30);

        int width = (int)entity.getWidth();
        int height = (int)entity.getHeight();
       
        g2.setColor(Color.red);
        
         g2.drawString("x: "+entity.getWorldX()+",y: "+entity.getWorldY(),x,y);
    }

    private void drawBackEndData(Graphics g2){
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        int y = 20;
        ArrayList<String> printables = new ArrayList<>(backEndData);
        for (String printData:printables){
           
            g2.drawString(printData,20,y);
            y += 25;
        }
        backEndData.clear();
    }

    private void drawObjectData(Graphics g2){
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.PLAIN, 16));
        int y = 20;
        ArrayList<String> printables = new ArrayList<>();

        
        for (String streng:(panel.chunkSystem.workingMemory.hoveredEntity.toString().split("\n"))){
            printables.add(streng);
        }

        for (String printData:printables){
           
            g2.drawString(printData,panel.screenWidth-150,y);
            y += 25;
        }
    }


    /** 
     * only react if entity is not null
    */
    private void center(BaseEntity entity){
        if (entity != null){
            int entityX = (int)entity.getWorldX();
            int entityY = (int)entity.getWorldY();

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

    /**works very poorly.. */
    public BufferedImage createTransparentRectangle(int width, int height,ArrayList<BaseEntity> visible) {
        // Create a BufferedImage with ARGB type for transparency support
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Get the graphics context from the image
        Graphics2D g2d = image.createGraphics();
        
        // Fill the entire image with black color
        g2d.setColor(new Color(0,0,0,100));
        g2d.fillRect(0, 0, width, height);
        
        // Clear the transparent area by setting alpha to 0
        g2d.setColor(new Color(0, 0, 0, 0)); // Transparent black color


        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskGraphics = mask.createGraphics();
        maskGraphics.setColor(new Color(0, 0, 0, 0)); // Transparent color

      
        


        for (BaseEntity baseE: visible){
            if (baseE.lightSource){
                for (int x = (int)(baseE.getWorldX()-worldX-300);x<baseE.getWorldX()-worldX+300;x++){
                    for (int y = (int)(baseE.getWorldY()-worldY-300);y<baseE.getWorldY()-worldY+300;y++){
                        if (x>=0 && x< width && y>= 0 && y<height){
                            if (image.getRGB(x,y)!=0x00000000 ){
                                image.setRGB(x, y, 0x00000000);
                            }
                            
                            
                        }
                        
                    }
                }
            }
        }
        
       
       
        
        // Dispose of the graphics context
        g2d.dispose();
        
        
        return image;
    }


    public void addbackendPrintData(String newString){backEndData.add(newString);}


    private BufferedImage toCompatibleImage(BufferedImage image)
{
    // obtain the current system graphical settings
    GraphicsConfiguration gfxConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().
        getDefaultConfiguration();

    /*
     * if image is already compatible and optimized for current system 
     * settings, simply return it
     */
    if (image.getColorModel().equals(gfxConfig.getColorModel()))
        return image;

    // image is not optimized, so create a new image that is
    BufferedImage newImage = gfxConfig.createCompatibleImage(
            image.getWidth(), image.getHeight(), image.getTransparency());

    // get the graphics context of the new image to draw the old image on
    Graphics2D g2d = newImage.createGraphics();

    // actually draw the image and dispose of context no longer needed
    g2d.drawImage(image, 0, 0, null);
    g2d.dispose();

    // return the new optimized image
    return newImage; 
}
}
