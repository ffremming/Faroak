package ressurser.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Entity;
import ressurser.baseEntity.tile.Tile;

public class SpriteFactory {
    private HashMap<String,ArrayList<BufferedImage>> spriteMap = new HashMap<>();
    public SpriteFactory(){

    
    }

    /**
     * returns the correct BufferedImage - prototype with little flexiblility
     */
    public BufferedImage getImage(BaseEntity entity){
        String name = entity.getName();
        BufferedImage sprite;

        Boolean animated = entity.getAnimated();

        //TILE SYSTEM FOR BORDERS IS NEEDED: PROBABLY A VISUAL SOLUTION WITH JUST APPENDING IS THE BEST.

        if (!spriteMap.containsKey(name)){
                loadImages(entity);
        }
        sprite = spriteMap.get(name).get(0);//First images in list //TODO change to iterating for animated.

        if (sprite == null){
                throw new NullPointerException("tried to access "+entity.getName()+ "s sprite, did return a null");
            }
        return sprite;

    }

    private void loadImages(BaseEntity entity){
        ArrayList<BufferedImage> allVersions = new ArrayList<BufferedImage>();
        String instance = "";

        if (entity instanceof Tile){
            instance = "TileSprites";
        } else if (entity instanceof Entity){
            instance = "EntitySprites";
        }

        // Get the directory for the specified name
        File directory = new File("ressurser/sprites/baseEntitySprites/"+instance +"/" + entity.getName());
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist: " + directory.getPath());
        }
    
        ArrayList<BufferedImage> allSprites = new ArrayList<BufferedImage>();

        // Iterate over all version folders
        File[] versionFolders = directory.listFiles();
        for (File versionFolder : versionFolders) {
            if (!versionFolder.isDirectory()) {
            continue;
            }
    
            // Load the image and add it to the version's arraylist
            BufferedImage image = null;
            try {
                image = ImageIO.read(versionFolder);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            allSprites.add(image);
            }
    
            // Add the version's arraylist to the main arraylist
            
        }
    
        return allVersions;
    }
}
