package ressurser.baseEntity.sprite;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ressurser.baseEntity.tile.Tile;
import ressurser.baseEntity.tile.TileManager;
import ressurser.drawing.Camera;
import ressurser.main.ImageContainer;

/**should only be one sprite for each kind of tile. It is a shared storage device */
public class TileSprite extends Sprite{

    public BufferedImage corner0;
    public BufferedImage border0;
    
    BufferedImage corner1;
    BufferedImage border1;

    BufferedImage corner2;
    BufferedImage border2;

    BufferedImage corner3;
    BufferedImage border3;

    TileManager tileManager;

    public TileSprite(String name,ImageContainer imageStorage,TileManager tileManager) {
        super(name,imageStorage);
        //loads all images to the sprite.
        loadImages();

        this.tileManager = tileManager;
        
    }

    @Override
    /**overrides load of images
     * this should load all borders
     */ 
    void loadImages(){
        imageStorage.setTileImages(this);
    }

    @Override
    void draw(Graphics2D g2,Tile tile,Camera camera) {
       
        
            int x = getWorldX()-camera.worldX;
            int y = getWorldY()-camera.worldY;
        
            g2.setColor(Color.WHITE);
           
            g2.drawImage(sprite.getImage(),x,y,64,64,null);
            for (int i = 0;i<tile.getNeighbors().length;i++){

                //if the neigbor is higher up than the selected tile:
                if (tileManager.tileHeight.get(tile.getNeighbors()[i].getName()) > tileManager.tileHeight.get(tile.getName())){
                    
                    tile.getNeighbors()[i].sprite.border
                }
            }
            g2.drawString(getName(),x,y+15);
    
            //UNCOMPLETED. NEEDS A BETTER METHOD FOR DRAWING USING THE SPRITE SHIT
        
    }

}