package ressurser.baseEntity.playable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import ressurser.baseEntity.Entity;
import ressurser.baseEntity.Vector;
import ressurser.main.GamePanel;

public class Playable extends Entity {

    Vector velocity = new Vector();
    ArrayList<BufferedImage> images = new ArrayList<>();

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
       
        setImages();
    }

    public void move() {
        worldX += velocity.transferX(5);
        worldX += velocity.transferY(5);
    }

    public void setVelocity(Vector newVector){
        velocity.add(newVector);
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

    private int getCorrespondingSpriteIndex() {//TODO
        return 0;
    }

    
}
