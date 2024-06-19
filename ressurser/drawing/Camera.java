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
import java.util.List;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Entity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.primitiveEntity;
import ressurser.baseEntity.playable.Moveable;
import ressurser.baseEntity.tile.Tile;
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

    BufferedImage baseImage = null;
    int frameCounter = 0;
    double baseX = 0;
    double baseY = 0;


    public int setWidth;

    public Camera(GamePanel panel, String name, int worldX, int worldY, short width, short height) {
        super(panel, name, worldX, worldY, (short) 50,(short)(50));
        //TODO Auto-generated constructor stub
        //follow(panel.spiller);
        
        follow(panel.player);
        
        //center(followed);
    }

    public Camera(GamePanel panel,String name){
        super(panel,name);
        //follow(panel.spiller);
        
        worldX = 0;
        worldY = 0;
        width = (short)panel.screenWidth;
        height = (short)panel.screenHeight;
        hitBox = new HitBox(this);
    }

    public void toggleTestData(){
        testData = !testData;
    }

    /**
     * follows entitiy until given other orders
     */
    public void follow(BaseEntity entity){
        //TODO
        this.followed = entity;
    }

    //draw background image without moveable entities
    private void setBaseImage(Graphics g){
        //TODO
        //baseImage = image2;
        baseX = worldX;
        baseY = worldY;
    }
    private Graphics2D getGraphics(BufferedImage image){
        Graphics2D g2 = image.createGraphics();
        return g2;
    }

    /**returns the image that is to be drawn on - blank canvas */
    private BufferedImage getImage(Graphics g){
        int width = panel.getWidth(); // Assuming getWidth() returns the width of your drawing area
        int height = panel.getHeight(); // Assuming getHeight() returns the height of your drawing area
       
        return new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
    }

    private void drawGraphics( Graphics2D g2){
        ArrayList<BaseEntity> visibleEntities = panel.world.getVisibleEntities(this);
        ArrayList<BaseEntity> visibleTiles = (panel.world.getVisibleTiles(this));

        drawEntities(visibleTiles,g2);
        drawEntities(visibleEntities,g2);
        
        addbackendPrintData("amount entities visible: "+String.valueOf(visibleEntities.size()));
        addbackendPrintData("amount tiles visible: "+String.valueOf(visibleTiles.size()));

        g2.drawImage(baseImage,(int)(baseX-worldX),(int)(baseY-worldY),width,height,null); 
    }

    /**
     * @param visibleEntities any list of any baseEntities - have to be sorted/managed beforehand
     * - draws all entities on the given graphics, also draws testData if wanted
     */
    private void drawEntities(ArrayList<BaseEntity> visibleEntities,Graphics2D g2){
        for (BaseEntity baseE :visibleEntities){
            drawRelative(g2,baseE);
            if (testData){
                drawHitBox(g2,baseE);
                drawCoords(g2,baseE);
            }
        }
    }

    void drawMoveablentities(Graphics2D g2){
        ArrayList<BaseEntity> withinCam = panel.world.getvisibleEntities();
        for (BaseEntity baseE :withinCam){

            if (baseE instanceof Moveable){
                drawRelative(g2,baseE);
                if (testData){
                    drawHitBox(g2,baseE);
                    drawCoords(g2,baseE);
                } 
            }
        }
    }
    
    public void setupImage(Graphics g){
        frameCounter ++;
        if (frameCounter>=20){
            setBaseImage(g);
            frameCounter = 0;
        } else { setBaseImage(g);}//changed to fix
        g.setFont(new Font("Arial", Font.PLAIN, 7));
       
        if (followed == null){
            centerAtPosition(new Point(0,0));
        }else{
            centerAtPosition(followed.getPoint());
        }
    }

    private BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(0, 0, rect.width, rect.height);
        return dest; 
     }

    public void draw(Graphics g){
        hitBox.updateCoords();
        center(followed);
        //for timing
        long startTime = System.nanoTime();
        
        BufferedImage image = getImage(g);
        Graphics2D g2 = getGraphics(image);
        drawGraphics(g2);
        
        
        drawUserInterface(g2);
        long endTime = System.nanoTime();
        drawData(g2,endTime,startTime);
        
       
        
        
        
        // BufferedImage optimized  = toCompatibleImage(image2); LESS EFFECTIVE
        g.drawImage(image, 0, 0, null);
        g2.dispose();
    }

    /**calls the userInterface to draw on given graphics */
    private void drawUserInterface(Graphics2D g2){
        panel.userInterface.draw(g2);
    }

    /**combination of drawing of  */
    private void drawData(Graphics2D g2,long endTime,long startTime){
        if (testData){
            drawChunks(g2);
        }
        writeHoveredInformation(g2);
        addDrawTimeData(startTime,endTime);
        drawHoveredEntityOutline(g2);
        drawBackEndData(g2);
    }

    private void addDrawTimeData(long startTime, long endTime) {
        addbackendPrintData("drawtime ms: "+String.valueOf((endTime-startTime)/1000000));
        addbackendPrintData("remaining cap: "+String.valueOf(1000000000-(((endTime-startTime)*60))));
    }
    public void drawRelative(Graphics2D g2,BaseEntity entity){
        
       
            int x = (int)(entity.getWorldX()-worldX);
            int y = (int)(entity.getWorldY()-worldY);

            if (testData){
                g2.setColor(Color.WHITE);
                g2.drawString(entity.getName(),x,y+15);
            }

            int shadowX = (int)(entity.getHitBox().x-worldX)-5;
            int shadowY = (int)(entity.getHitBox().y-worldY)+15;
           
            
            //g2.drawImage(entity.getImage(),x,y,64,64,null);     //can remove width and height.
            //shadow system is not good enoght by far..
            if (!(entity instanceof Tile)){
                g2.setColor(new Color(100,100,150,60));
                g2.fillOval(shadowX,shadowY,entity.getHitBox().width+10,entity.getHitBox().height);
            }

            ArrayList<BufferedImage> imageSetToBeDrawn;

            
            imageSetToBeDrawn = (entity.getImages());
           
            
            for (BufferedImage img:imageSetToBeDrawn){
                g2.drawImage(img,x,y,entity.getWidth(),entity.getHeight(),null);  
            }
    }

    private void drawChunks(Graphics2D g2){
        float lineWidth = 2;
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);
        ArrayList<Chunk> chunkCopy = new ArrayList<>(panel.world.getChunks());
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
        g2.setColor(Color.white);
        float lineWidth = 0.5f;
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2.setStroke(stroke);
       
        g2.drawRect(x,y,width,height);
        //g2.drawString("HB: x=" + entity.getHitBox().x + ",HB y=" + entity.getHitBox().y + ", HBwidth=" + width + ", HBheight=" + height, x, y - 10);
        if (entity instanceof Moveable){
            Rectangle rect = ((Moveable) entity).getHitboxInfront();
            g2.drawRect((int)(rect.x-worldX),(int)(rect.y-worldY),rect.width,rect.height);
            g2.drawString("Infront: x=" + rect.x + ", y=" + rect.y + ", width=" + rect.width + ", height=" + rect.height, (int)(rect.x-worldX), (int)(rect.y-worldY) - 10);

            HitBox interactionHB = ((Moveable) entity).getInteractionHitBox();
            g2.drawRect((int)(interactionHB.x-worldX),(int)(interactionHB.y-worldY),interactionHB.width,interactionHB.height);
            g2.drawString("Interaction: x=" + interactionHB.x + ", y=" + interactionHB.y + ", width=" + interactionHB.width + ", height=" + interactionHB.height, (int)(interactionHB.x-worldX), (int)(interactionHB.y-worldY) - 10);
        }
        
        
        
    }


    /**draws white outline around entity */
    private void drawEntityOutline(Graphics2D g2,BaseEntity entity){
        if (entity!= null){

            HitBox thisHB = entity.getHitBox();
            int x = (int) (entity.getWorldX()-worldX);
            int y= (int) (entity.getWorldY()-worldY);

            //TODO follow up on the getImages() below
            g2.drawImage(panel.imageContainer.getOutline(entity.getImages().get(0)),x,y,(int)entity.getWidth(),(int)entity.getHeight(),null);
        }
    }

    /** draws the outline of hoveredEntity, if there is one*/
    private void drawHoveredEntityOutline(Graphics2D g2){
        
        drawEntityOutline(g2,panel.world.getHoveredEntity());
        
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

    /**writes infroamtion about the hovered entity, draws this on image */
    private void writeHoveredInformation(Graphics g2){
        if (panel.world.getHoveredEntity() != null){
            g2.setColor(Color.white);
            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            int y = 20;
            ArrayList<String> printables = new ArrayList<>();
    
            
            for (String streng:(panel.world.getHoveredEntity().toString().split("\n"))){
                printables.add(streng);
            }
    
            for (String printData:printables){
               
                g2.drawString(printData,panel.screenWidth-150,y);
                y += 25;
            }
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
            worldY = entityY-height/2;
        }else{
            throw new NullPointerException("followed is null in Camera");
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
    public void setWidth(int i) {
        width = (short)i;
    }
    public void setHeight(int i) {
      height = (short) i;
    }


    public BufferedImage createDarkImage(List<BaseEntity> entities) {
        // Determine the dimensions of the image based on the maximum WorldX and WorldY
        
        
        
        // Create a BufferedImage with the determined dimensions
        BufferedImage darkImage = new BufferedImage(width ,height, BufferedImage.TYPE_INT_ARGB);
        
        // Loop through the entities and set lighter spots based on their lightLvl
        for (BaseEntity entity : entities) {
            int lightLvl = entity.getLightLvl(); // Assuming getLightLvl() returns the light level of the entity
            if (getHitBox().intersects(entity.getHitBox())){
                if (lightLvl > 0) {
                    int x = (int)(entity.getWorldX()-worldX);
                    int y = (int)(entity.getWorldY()-worldY);
    
                    int rgb = (255 << 24) | (lightLvl << 16) | (lightLvl << 8) | lightLvl; // Lighter spot color
                    if (x>=0 && x< width && y>= 0 && y<height)
                    darkImage.setRGB(x, y, rgb);
                }
            }
            }   
        return darkImage;
    }
}
