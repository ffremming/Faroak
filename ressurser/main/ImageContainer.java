package ressurser.main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

import javax.imageio.ImageIO;

import ressurser.baseEntity.sprite.TileSprite;
import ressurser.chunkSystem.terrainGeneration.Biome;

public class ImageContainer {

    public HashMap<String,BufferedImage> images = new HashMap<>();
    public HashMap<String,ArrayList<BufferedImage>> objectImages = new HashMap<>();
    public HashMap<String,BufferedImage> itemImages = new HashMap<>();

    public ImageContainer(){
        setupBaseImages();
    }

    public BufferedImage getTileImage(String name){
        if (images.containsKey(name)){
            return (images.get(name));
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

            BufferedImage grass = scaleImage( ImageIO.read(new File("ressurser/images/tile/plains.png")),64,64);
            BufferedImage mud = scaleImage(ImageIO.read(new File("ressurser/images/tile/mud.png")),64,64);
            BufferedImage moss = scaleImage(ImageIO.read(new File("ressurser/images/tile/moss.png")),64,64);
            BufferedImage sand = scaleImage(ImageIO.read(new File("ressurser/images/tile/sand.png")),64,64);
            BufferedImage water = scaleImage(ImageIO.read(new File("ressurser/images/tile/ocean.png")),64,64);
            BufferedImage dark_grass = scaleImage(ImageIO.read(new File("ressurser/images/tile/dark_grass.png")),64,64);
            BufferedImage savanna = scaleImage(ImageIO.read(new File("ressurser/images/tile/savanna.png")),64,64);


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
        try {
            
            image = scaleImage(ImageIO.read(new File("ressurser/images/tile/"+name+".png")),32,32);
            
        }catch (IOException e) {
            // TODO Auto-generated catch block
            
            
            //create the right image from file
            try {
               
                
                int degrees = getNumberAtEnd(name)*90-90;
                String rawName = removeNumberAtEnd(name);
                
                

                if (rawName.charAt(rawName.length() - 1) == 'C') {
                    if (doesPNGFileExist("tile/"+removeNumberAtEnd(name)+"0")){
                        image = scaleImage(getRotated(getTileImage(removeNumberAtEnd(name)+"0"),degrees),32,32);
                    }
                    
                } else if (rawName.charAt(rawName.length() - 1) == 'B') {
                    if (doesPNGFileExist("tile/"+removeNumberAtEnd(name)+"1")){
                    image = scaleImage(getRotated(getTileImage(removeNumberAtEnd(name)+"1"),degrees),32,32);
                    }
                }
                
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                System.out.println("tried to rotate image, something went wrong:");
            }
        }
        if (name.startsWith("grass")){
            System.out.println(name +",,,"+image);
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

    public ArrayList<BufferedImage> setPlayableImages(String name) {
        ArrayList<BufferedImage> images = new ArrayList<>();
        try {
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/up1.png")));
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/up2.png")));
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/up3.png")));

        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/right1.png")));
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/right2.png")));
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/right3.png")));

        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/down1.png")));
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/down2.png")));
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/down3.png")));

        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/left1.png")));
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/left2.png")));
        images.add(ImageIO.read(new File("ressurser/images/playable/"+name+"/left3.png")));
        
    } catch (IOException e) {
        // TODO Auto-generated catch block
        System.out.println(name + " could not load the sprites of playable");
        e.printStackTrace();
    }
    return images;
       
    }



    public ArrayList<BufferedImage> getObjectImages(String name){
        if (objectImages.containsKey(name)){
            return objectImages.get(name);
        }
        else{
            return getObjectImagesFromFile(name);
        }
    }


    public ArrayList<BufferedImage> getObjectImagesFromFile(String name) {
        ArrayList<BufferedImage> images = new ArrayList<>();

        File folder = new File("ressurser/images/objects/"+name); // Change this to your folder path
        
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            BufferedImage image = ImageIO.read(file);
                            if (image != null) {
                                images.add(image);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }



        }
        objectImages.put(name,images);
        return images;
    }


   
        public static BufferedImage scaleImage(BufferedImage originalImage, int scaledWidth, int scaledHeight) {
            // Create a new buffered image with the desired size and type
            BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
    
            // Create a graphics context from the scaled image
            Graphics2D g2d = scaledImage.createGraphics();
    
            // Set rendering hints for pixelated scaling
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    
            // Create an AffineTransform for scaling
            AffineTransform transform = AffineTransform.getScaleInstance(
                    (double) scaledWidth / originalImage.getWidth(),
                    (double) scaledHeight / originalImage.getHeight()
            );
    
            // Create an AffineTransformOp and apply the transformation
            AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            op.filter(originalImage, scaledImage);
    
            // Dispose of the graphics context
            g2d.dispose();
    
            return scaledImage;
        }

    public BufferedImage getItemImage(String name){
        if (itemImages.containsKey(name)){
            return (images.get(name));
        }
        else{
            return retrieveItemImage(name);
        }

        
    
    }
    private BufferedImage retrieveItemImage(String name) {
        BufferedImage image = null;
        try {
            
            image = scaleImage(ImageIO.read(new File("ressurser/images/items/"+name+".png")),32,32);
            
        }catch (IOException e) {
            
        }
        itemImages.put(name,image);
        return image;
    }   
}
