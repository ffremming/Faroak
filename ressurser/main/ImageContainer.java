package ressurser.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ressurser.chunkSystem.terrainGeneration.Biome;

public class ImageContainer {

    HashMap<String,BufferedImage> images = new HashMap<>();

    public ImageContainer(){
        setupBaseImages();
    }

    public BufferedImage getImage(String name){
        if (images.containsKey(name)){
            return images.get(name);
        }
        else{
            //TODO
        }
        return null;
    }

    private void loadBufferedImage(String name){

    }

    private void setupBaseImages(){
        try {

            BufferedImage grass = ImageIO.read(new File("ressurser/tileSpriteGenerator/gbackground.png"));
            BufferedImage mud = ImageIO.read(new File("ressurser/tileSpriteGenerator/mbackground.png"));
            BufferedImage moss = ImageIO.read(new File("ressurser/tileSpriteGenerator/Mobackground.png"));
            BufferedImage sand = ImageIO.read(new File("ressurser/tileSpriteGenerator/sbackground.png"));
            BufferedImage water = ImageIO.read(new File("ressurser/tileSpriteGenerator/wbackground.png"));
            BufferedImage dark_grass = ImageIO.read(new File("ressurser/tileSpriteGenerator/dG.png"));
            BufferedImage savanna = ImageIO.read(new File("ressurser/tileSpriteGenerator/Sa.png"));


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
