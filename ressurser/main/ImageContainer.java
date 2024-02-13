package ressurser.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ressurser.baseEntity.sprite.TileSprite;
import ressurser.chunkSystem.terrainGeneration.Biome;

public class ImageContainer {

    public HashMap<String,BufferedImage> images = new HashMap<>();

    public ImageContainer(){
        setupBaseImages();
    }

    public BufferedImage getImage(String name){
        if (images.containsKey(name)){
            return images.get(name);
        }
        else{
            return retrieveTileSpriteImage(name);
        }
    }

    public boolean containsImage(String name){
        return (images.containsKey(name));
    }

    private void loadBufferedImage(String name){

    }

    private void setupBaseImages(){
        try {

            BufferedImage grass = ImageIO.read(new File("ressurser/images/plains.png"));
            BufferedImage mud = ImageIO.read(new File("ressurser/images/mud.png"));
            BufferedImage moss = ImageIO.read(new File("ressurser/images/moss.png"));
            BufferedImage sand = ImageIO.read(new File("ressurser/images/sand.png"));
            BufferedImage water = ImageIO.read(new File("ressurser/images/ocean.png"));
            BufferedImage dark_grass = ImageIO.read(new File("ressurser/images/dark_grass.png"));
            BufferedImage savanna = ImageIO.read(new File("ressurser/images/savanna.png"));


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
            images.put("rain_forest",savanna);

            
            System.out.println(images.get("plains"));

    

        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("problem with base load of images");
            //e.printStackTrace();

        }
    }

    /**takes inn the name of file : 
     * for tile - background // background + B + (0-3) // background + C + (0-3)
     */
    private BufferedImage retrieveTileSpriteImage(String name){
        BufferedImage image = null;
        System.out.println(name);
        try {

            image = ImageIO.read(new File("ressurser/images/"+name+".png"));
            

        }catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("problem with load of images - "+name);
            System.out.println((removeNumberAtEnd(name)+"1")+","+(getNumberAtEnd(name)*90-90) );
            //create the right image from file
            try {
               
                
                int degrees = getNumberAtEnd(name)*90-90;
                String rawName = removeNumberAtEnd(name);
                System.out.println("raw  "+rawName);
                System.out.println(degrees);
                System.out.println("name "+degrees + ", end");
                

                if (rawName.charAt(rawName.length() - 1) == 'C') {
                    if (doesPNGFileExist(removeNumberAtEnd(name)+"0")){
                        image = getRotated(getImage(removeNumberAtEnd(name)+"0"),degrees);
                    }
                    
                } else if (rawName.charAt(rawName.length() - 1) == 'B') {
                    if (doesPNGFileExist(removeNumberAtEnd(name)+"1")){
                    image = getRotated(getImage(removeNumberAtEnd(name)+"1"),degrees);
                    }
                }

                
                
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                System.out.println("tried to rotate image, something went wrong:");
                
            }
        }

        images.put(name,image);
        return image;
    }

    public static boolean doesPNGFileExist( String fileName) {
       
        try {
            BufferedImage image = ImageIO.read(new File("ressurser/images/"+fileName+".png"));
            return true;
        } catch (IOException e) {
           return false;
        }
        
    }

    private static String removeNumberAtEnd(String input){
        // Use regular expression to match and remove the number at the end
        return input.replaceAll("\\d+$", "");
    }
    public static int getNumberAtEnd(String input) {
        // Check if the input string is not empty
        if (input != null && !input.isEmpty()) {
            // Use regular expression to find the number at the end
            String numberString = input.replaceAll(".*[^\\d](\\d+)$", "$1");

            // Convert the extracted number string to an integer
            try {
                return Integer.parseInt(numberString);
            } catch (NumberFormatException e) {
                // Handle the case where the number cannot be parsed
                System.err.println("Error parsing the number: " + e.getMessage());
            }
        }

        // Return a default value (e.g., 0) if no valid number is found
        return -1;
    }

    public void setTileImages(TileSprite tileSprite) {
        
        //main background
        tileSprite.setMain(retrieveTileSpriteImage(tileSprite.getName()));

        //corners
        tileSprite.corner0 = retrieveTileSpriteImage(tileSprite.getName() +"C" +"0");
        tileSprite.corner0 = retrieveTileSpriteImage(tileSprite.getName() +"C" +"1");
        tileSprite.corner0 = retrieveTileSpriteImage(tileSprite.getName() +"C" +"2");
        tileSprite.corner0 = retrieveTileSpriteImage(tileSprite.getName() +"C" +"3");

        //borders
        tileSprite.border0 = retrieveTileSpriteImage(tileSprite.getName() +"B" +"0");
        tileSprite.border0 = retrieveTileSpriteImage(tileSprite.getName() +"B" +"1");
        tileSprite.border0 = retrieveTileSpriteImage(tileSprite.getName() +"B" +"2");
        tileSprite.border0 = retrieveTileSpriteImage(tileSprite.getName() +"B" +"3");

        
    }

    

    private BufferedImage getRotated(BufferedImage image,int angle){
        return ImageResources.rotateImage(image,angle);
    }
}
