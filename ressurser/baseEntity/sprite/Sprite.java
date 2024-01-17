package ressurser.baseEntity.sprite;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import ressurser.main.GamePanel;
import ressurser.main.ImageContainer;


public abstract class Sprite {

    boolean animated;
    String name;
    
    ArrayList<ArrayList<BufferedImage>> imageVersions = new ArrayList<>();
    BufferedImage main;

    GamePanel panel;

    int animationNumber = 0;
    int versionNumber = 0;
    int maxAnimations = 3;
    int maxVersions = 3;

    ImageContainer imageStorage;

    //the sprite will contain only the sprite for one object at one state. the sprite class can howeever contain variatons of the sprite, like animations.

    public Sprite(String name,ImageContainer imageStorage){
        this.name = name;
        loadImages();
        this.imageStorage = imageStorage;
    }
    public void setMain(BufferedImage image){
        main = image;
    }
    public String getName(){
        return name;
    }

    abstract void loadImages();
       
   

    public BufferedImage getImage(){
        return main;
    }

    /**
     * updates the number of animation. this will decide what sprite will be shown if the object is animated.
     */
    private void updateAnimation(){
        animationNumber++;
        
       
    }
    
    /**
     * updates the number of versions. this will decide what sprite will be shown if the object has multiple versions.
     */
    public void updateVersion(){
        versionNumber++;
       
    }

    
}
