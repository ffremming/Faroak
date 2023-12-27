package ressurser.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ressurser.chunkSystem.terrainGeneration.Biome;

public class ImageContainer {

    public HashMap<String,BufferedImage> images = new HashMap<>();

    public ImageContainer(){
        setupBaseImages();
    }

    public BufferedImage getImage(String name) throws Exception{
        if (images.containsKey(name)){
            return images.get(name);
        }
        else{
            throw new Exception("no such image");
        }
       
    }

    private void loadBufferedImage(String name){

    }

    private void setupBaseImages(){
        try {

            BufferedImage grass = ImageIO.read(new File("../images/grass.png"));
            BufferedImage mud = ImageIO.read(new File("../images/mud.png"));
            BufferedImage moss = ImageIO.read(new File("../images/moss.png"));
            BufferedImage sand = ImageIO.read(new File("../images/sand.png"));
            BufferedImage water = ImageIO.read(new File("../images/ocean.png"));
            BufferedImage dark_grass = ImageIO.read(new File("../images/dark_grass.png"));
            BufferedImage savanna = ImageIO.read(new File("../images/savanna.png"));


            images.put("plains",grass);
            images.put("swamp",mud);
            images.put("seasonal forest",moss);
            images.put("desert",sand);
            images.put("ocean",water);
            images.put("forest",dark_grass);
            images.put("savanna",savanna);

            images.put("snowy Tundra",dark_grass);
            images.put("snowy taiga",dark_grass);
            images.put("beach",sand);
            images.put("rain forest",savanna);

            


    

        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("problem with base load of images");
            e.printStackTrace();

        }
        
       
    }
}
