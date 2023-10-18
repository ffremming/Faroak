package ressurser.sprites;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Sprite;
import ressurser.main.GamePanel;

public class SpriteLoader {

    GamePanel panel;
    HashMap<String,Sprite> spriteContainer = new HashMap<>();
    HashMap<String,Sprite> tileSprites = new HashMap<>();
    HashMap<String,Sprite> entitySprites = new HashMap<>();
    

    public SpriteLoader(GamePanel panel){
        this.panel = panel;
    }

    public static void main(String[] args) {
        
            System.out.println(loadImages("grass"));
        
           
           
        
    }
    

    public static ArrayList<ArrayList<BufferedImage>> loadImages(String name) {
        
        ArrayList<ArrayList<BufferedImage>> allVersions = new ArrayList<ArrayList<BufferedImage>>();
    
        // Get the directory for the specified name
        File directory = new File("ressurser/sprites/baseEntitySprites/" + name);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist: " + directory.getPath());
        }
    
        // Iterate over all version folders
        File[] versionFolders = directory.listFiles();
        for (File versionFolder : versionFolders) {
            if (!versionFolder.isDirectory()) {
            continue;
            }
    
            // Create a new arraylist for the current version
            ArrayList<BufferedImage> versionImages = new ArrayList<BufferedImage>();
    
            // Iterate over all animation files in the current version folder
            File[] animationFiles = versionFolder.listFiles();
            for (File animationFile : animationFiles) {
            if (!animationFile.isFile() || !animationFile.getName().endsWith(".png")) {
                continue;
            }
    
            // Load the image and add it to the version's arraylist
            BufferedImage image = null;
            try {
                image = ImageIO.read(animationFile);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            versionImages.add(image);
            }
    
            // Add the version's arraylist to the main arraylist
            allVersions.add(versionImages);
        }
    
        return allVersions;
    }



       
        
    

    /**
     * Either loads a new sprite that is not prevoiusly loaded,or gets a sprite that is stored in a hashmap.
     * 
     * @return the image thas is associated with the path.
     * 
     */
    public BufferedImage getSprite(String path){
        if (!spriteContainer.keySet().contains(path)){
             return insertImage(loadSprites(path),path);
        } 
        return null;
        //return spriteContainer.get(path);
        
    }


    /**
     * insert the image from parameter to the hashmap. 
     * the key is the path.
     * 
     * @return Bufferedimage that is inserted
     */
    private BufferedImage insertImage (BufferedImage image,String path){
       
        return image;
    }

    /**
     * loads the image assocaited with the path.
     * 
     * @return the image thas is associated with the path.
     * 
     */
    private BufferedImage loadSprites(String path){
        BufferedImage image = null;

        try {
            image= ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(path +"path can not be loaded");
        }
        return image;
    }

    public void loadTileSprite(String tile) {
       // String path = "/baseEntitySprites/TileSprites/"+baseEntity.getNameID();
        
    }

    public BufferedImage getTileImage(String basePath) {

        return null;
    }

    
}
