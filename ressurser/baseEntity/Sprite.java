package ressurser.baseEntity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import ressurser.main.GamePanel;
import ressurser.sprites.SpriteLoader;

public class Sprite {
    
    BaseEntity baseEntity;


    public int width;


    public int height;


    public int worldX;


    public int worldY;


    int relativeXValue;


    int relativeYValue;

    public BufferedImage activeImage;

    

    boolean animated;

    int animationNumber = 0;
    int versionNumber=0;
    
    ArrayList<ArrayList<BufferedImage>> images = new ArrayList<>();

    GamePanel panel;



    //the sprite will contain only the sprite for one object at one state. the sprite class can howeever contain variatons of the sprite, like animations.

    public Sprite(BaseEntity baseEntity){

        this.baseEntity = baseEntity;
        width = baseEntity.width;
        height = baseEntity.height;

        

    }

    public Sprite(String entityName){
        loadImages(entityName);

        updateImageVersion();

    }

    private void loadImages(String objectName){
        images = SpriteLoader.loadImages(objectName);
    }


    /**
     * this does not work yet.
     */
    public void draw(Graphics2D g2){
        baseEntity.panel.drawingM.drawRelative(this,g2);
    }


    public BufferedImage getImage(){
        return activeImage;
    }

    /**
     * updates the number of animation. this will decide what sprite will be shown if the object is animated.
     */
    public void updateAnimation(){
        animationNumber++;
        updateImageVersion();
       
    }

    /**
     * updates the number of versions. this will decide what sprite will be shown if the object has multiple versions.
     */
    public void updateVersion(){
        versionNumber++;
        updateImageVersion();
    }

    /**
     * emthod shold change field activeImage to the correct image. should be called when a update occurs.
     */
    public void updateImageVersion(){
        activeImage = images.get(versionNumber).get(animationNumber);
    }
}
