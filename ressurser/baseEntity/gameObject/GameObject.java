package ressurser.baseEntity.gameObject;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import ressurser.baseEntity.Entity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.playable.Playable;
import ressurser.main.GamePanel;

public class GameObject extends Entity{

    

    public GameObject(GamePanel panel, String name, int worldX, int worldY, int width, int height, int hitBoxWidth,
            int hitBoxHeight, int i, int j,boolean solid) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, i, j);
        this.solid = solid;
        if (this.panel== null){System.out.println(name);}
        getImage();
    }

    public GameObject(GamePanel panel, String name, double worldX, double worldY, int width, int height, HitBox hitBox,
            boolean solid) {
                super(panel, name, worldX, worldY, width, height, hitBox);
                this.solid = solid;
                this.panel = panel;
                if (this.panel== null){System.out.println(name+","+2);}
                getImage();
    }

    /**used for drawing */
    @Override 
    public ArrayList<BufferedImage> getImages(){
        ArrayList<BufferedImage> arr = new ArrayList<>();

        if (panel.player != null){
            if (panel.player.getRectangle().intersects(getRectangle()) && images.size() > 0){
                if(panel.imageContainer.checkIntersection(this,panel.player )){
                   

                    //arr.add( panel.imageContainer.reduceTransparency(images.get(animationIndex)));
                    //return arr;
                }
               
            }
            
        }
        
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
  
            e.printStackTrace();
        }
        return image;
    }

    public void interact(Playable playable) {
       
    }


    public GameObject getPreviewObject(GamePanel panel2){
       
        return new GameObject(panel2,name+",preview",worldX,worldX,width,height,getHitBox(),solid);
    }
    
}
