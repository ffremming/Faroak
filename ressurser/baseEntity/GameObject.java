package ressurser.baseEntity;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import ressurser.main.GamePanel;

public class GameObject extends Entity{

    public GameObject(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short i, short j,boolean solid) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, i, j);
        this.solid = solid;
        getImage();
    }

    /**used for drawing */
    @Override 
    public ArrayList<BufferedImage> getImages(){
        ArrayList<BufferedImage> arr = new ArrayList<>();
        arr.add(images.get(animationIndex));
        return arr;
    }

    @Override
    /**used for getting the images from image container */
    public BufferedImage getImage(){
        
        BufferedImage image = null;
        
        try {

            images = panel.imageContainer.getObjectImages(name);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return image;
    }
    

    
}
