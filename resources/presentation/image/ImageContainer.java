package resources.presentation.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

import javax.imageio.ImageIO;

import resources.domain.entity.Entity;

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
        BufferedImage loaded = retrieveTileSpriteImage(name);
        if (loaded != null){
            return loaded;
        }
        BufferedImage fallback = makeFallbackTile(name);
        images.put(name, fallback);
        return fallback;
    }

    /** Solid-colour 64x64 tile keyed by name so different biomes get distinct shades when their sprite is missing. */
    private BufferedImage makeFallbackTile(String name){
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(fallbackColor(name));
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        return img;
    }

    private Color fallbackColor(String name){
        if (name == null) return new Color(0x80, 0x80, 0x80);
        String n = name.toLowerCase();
        if (n.startsWith("ocean") || n.startsWith("water") || n.startsWith("river")) return new Color(0x2E, 0x6B, 0xB5);
        if (n.startsWith("beach") || n.startsWith("sand"))                            return new Color(0xE6, 0xD2, 0x9C);
        if (n.startsWith("desert"))                                                   return new Color(0xD9, 0xB8, 0x76);
        if (n.startsWith("snow") || n.startsWith("icy") || n.startsWith("ice"))       return new Color(0xDC, 0xE6, 0xEE);
        if (n.startsWith("mountain") || n.startsWith("cliff") || n.startsWith("rock"))return new Color(0x80, 0x80, 0x80);
        if (n.startsWith("savanna"))                                                  return new Color(0xCD, 0xBE, 0x70);
        if (n.startsWith("swamp") || n.startsWith("moss") || n.startsWith("mud"))     return new Color(0x4E, 0x4E, 0x35);
        if (n.startsWith("forest") || n.contains("forest"))                           return new Color(0x40, 0x80, 0x00);
        return new Color(0xA0, 0xC0, 0x80);
    }

    public boolean containsImage(String name){
        return (images.containsKey(name));
    }

    private void loadBufferedImage(String name){

    }



    private void setupBaseImages(){
        // Each mapping resolves independently so one missing sprite doesn't blank out the rest.
        // Order matters: aliases below may reuse the canonical image loaded above.
        loadTile("plains",          "plains.png");
        loadTile("swamp",           "mud.png");
        loadTile("seasonal forest", "moss.png");
        loadTile("ocean",           "oceanT.png");
        loadTile("savanna",         "savanna.png");
        loadTile("desert",          "desert.png");
        loadTile("forest",          "forest.png");
        loadTile("beach",           "beach.png");
        loadTile("mountain",        "rockCliff0.png");

        aliasTile("snowy Tundra", "plains");
        aliasTile("snowy taiga",  "forest");
        aliasTile("rain_forest",  "forest");
        aliasTile("rain forest",  "forest");
        aliasTile("riverbank",    "beach");
    }

    /** Load a single tile under {@code key}, falling back to a colour swatch if the file is missing. */
    private void loadTile(String key, String fileName){
        File file = new File("resources/images/tile/" + fileName);
        if (file.exists()) {
            try {
                images.put(key, scaleImage(ImageIO.read(file), 64, 64));
                return;
            } catch (IOException e) {
                System.out.println("failed to read tile " + fileName + ": " + e.getMessage());
            }
        } else {
            System.out.println("tile sprite missing: " + fileName + " (using fallback for '" + key + "')");
        }
        images.put(key, makeFallbackTile(key));
    }

    /** Point a biome name at an already-loaded tile image. */
    private void aliasTile(String alias, String existingKey){
        BufferedImage img = images.get(existingKey);
        if (img != null) {
            images.put(alias, img);
        } else {
            images.put(alias, makeFallbackTile(alias));
        }
    }

    /**takes inn the name of file : 
     * for tile - background // background + B + (0-3) // background + C + (0-3)
     */
    private BufferedImage retrieveTileSpriteImage(String name){
        BufferedImage image = null;
        try {
            
            image = scaleImage(ImageIO.read(new File("resources/images/tile/"+name+".png")),32,32);
            
        }catch (IOException e) {
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
                System.out.println("tried to rotate image, something went wrong:");
            }
        }
        if (name.startsWith("grass")){
           
        }
       
        images.put(name,image);
        return image;
    }

    public static boolean doesPNGFileExist( String fileName) {
       
        try {
            BufferedImage image = ImageIO.read(new File("resources/images/"+fileName+".png"));
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



    

    private BufferedImage getRotated(BufferedImage image,int angle){
        return ImageResources.rotateImage(image,angle);
    }

    public ArrayList<BufferedImage> setPlayableImages(String name) {
        ArrayList<BufferedImage> images = new ArrayList<>();
        try {
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/up1.png")));
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/up2.png")));
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/up3.png")));

        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/right1.png")));
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/right2.png")));
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/right3.png")));

        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/down1.png")));
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/down2.png")));
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/down3.png")));

        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/left1.png")));
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/left2.png")));
        images.add(ImageIO.read(new File("resources/images/playable/"+name+"/left3.png")));
        
    } catch (IOException e) {
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
            if (name.contains(",")){
                return getObjectPreview(name);
            }

            return getObjectImagesFromFile(name);
        }
    }

    private ArrayList<BufferedImage> getObjectPreview(String name) {
        String original = name.split(",")[0];
        ArrayList<BufferedImage> originalImages = getObjectImages(original);
        ArrayList<BufferedImage> previewImages = new ArrayList<>();
        for (BufferedImage img:originalImages){
            previewImages.add(reduceTransparency(img));
        }
        return previewImages;
    }


    public ArrayList<BufferedImage> getObjectImagesFromFile(String name) {
        ArrayList<BufferedImage> images = new ArrayList<>();

        File folder = new File("resources/images/objects/"+name); // Change this to your folder path
        
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
            return (itemImages.get(name));
        }
        else{
            return retrieveItemImage(name);
        }

        
    
    }
    private BufferedImage retrieveItemImage(String name) {
        BufferedImage image = null;
       
        try {
            File file = new File("resources/images/items/"+name+".png");
            if(file.exists()){
                image = ImageIO.read(file);
                System.out.println("#loaded item image");
            } else {
                System.out.println("File does not exist: " + file.getAbsolutePath());
                ArrayList<BufferedImage> potentionImage = getObjectImages(name);
                if (potentionImage.size()>0){image = potentionImage.get(0);}
                

            }
        } catch (IOException e) {
            System.out.println("Error while reading the file: " + e.getMessage());
        }
        if (image == null){
            System.out.println("itemImage is null, something went wrong.");
        }
       
        itemImages.put(name,image);
        return image;
    }   



    /** returns outline image */
    public BufferedImage getOutline(BufferedImage originalImage)throws NullPointerException { 
        
        // Create a new buffered image for the outline
        BufferedImage outlineImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // Create a graphics context for the outline image and fill it with white
        Graphics2D g = outlineImage.createGraphics();
        g.setColor(Color.WHITE);
        //g.fillRect(0, 0, outlineImage.getWidth(), outlineImage.getHeight());
        // Iterate over each pixel in the original image
        for (int x = 0; x < originalImage.getWidth(); x++) {
            for (int y = 0; y < originalImage.getHeight(); y++) {
                // Get the color of the pixel in the original image
                int pixel = originalImage.getRGB(x, y);
                Color color = new Color(pixel, true);

                // If the pixel is not transparent or less than 30% transparent, check its surrounding pixels
                if (color.getAlpha() > 76 ) { // 76 is approximately 30% of 255
                    // Check the surrounding pixels
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int nx = x + dx;
                            int ny = y + dy;

                            // If the surrounding pixel is within the image bounds and is transparent or less than 30% transparent, set it to white
                            if (nx >= 0 && nx < originalImage.getWidth() && ny >= 0 && ny < originalImage.getHeight()) {
                                int neighborPixel = originalImage.getRGB(nx, ny);
                                Color neighborColor = new Color(neighborPixel, true);

                                if (neighborColor.getAlpha() <= 76) { // 76 is approximately 30% of 255
                                    outlineImage.setRGB(nx, ny, Color.WHITE.getRGB());
                                }
                            } else {
                                if (color.getAlpha() > 76) { // 76 is approximately 30% of 255
                                    outlineImage.setRGB(x, y, Color.WHITE.getRGB());
                                }
                            }
                        }
                    }
                }
            }
        }

        return outlineImage;
    }


    public BufferedImage reduceTransparency(BufferedImage image) {

        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();

        
        for (int i = 0; i < result.getWidth(); i++) {
            for (int j = 0; j < result.getHeight(); j++) {
                int argb = result.getRGB(i, j);
                int alpha = (argb >> 24) & 0xFF;
               
                int reducedAlpha = (int) (alpha * 0.3); // reduce to 10%
                
                int rgb = argb & 0x00ffffff; // get the rgb values
                int newArgb = (reducedAlpha << 24) | rgb; // combine reduced alpha and rgb
               

                result.setRGB(i, j, newArgb);
            }
        }

        return result;
    }

    public boolean checkIntersection(Entity entity1, Entity entity2) {
        // BufferedImage entity1Outline = getOutline(entity1.images.get(0));
        // BufferedImage entity2Outline = getOutline(entity2.images.get(0));
    
        BufferedImage entity1Outline = entity1.images.get(0);
        BufferedImage entity2Outline = entity2.images.get(0);
    
        int entity1Width = entity1.getWidth();
        int entity1Height = entity1.getHeight();
        int entity1ImageWidth = entity1Outline.getWidth();
        int entity1ImageHeight = entity1Outline.getHeight();
    
        int entity2Width = entity2.getWidth();
        int entity2Height = entity2.getHeight();
        int entity2ImageWidth = entity2Outline.getWidth();
        int entity2ImageHeight = entity2Outline.getHeight();
    
        double entity1ConversionRateX = (double) entity1ImageWidth / entity1Width;
        double entity1ConversionRateY = (double) entity1ImageHeight / entity1Height;
    
        double entity2ConversionRateX = (double) entity2ImageWidth / entity2Width;
        double entity2ConversionRateY = (double) entity2ImageHeight / entity2Height;
    
        int amountCounter = 0;
        int neededPixelsIntersection = (entity1Width*entity1Height)/(entity1ImageWidth*entity1ImageHeight)*150;

        for (int x = 0; x < entity1ImageWidth; x++) {
            for (int y = 0; y < entity1ImageHeight; y++) {
                int alpha1 = (entity1Outline.getRGB(x, y) >> 24) & 0xFF;
                if (alpha1 > 128) { // if alpha value is high
    
                    // Coordinates of pixels
                    int worldX = (int) (x / entity1ConversionRateX + entity1.getWorldX());
                    int worldY = (int) (y / entity1ConversionRateY + entity1.getWorldY());
    
                    // Pixels of 2nd image
                    int pixelX = (int) ((worldX - entity2.getWorldX()) * entity2ConversionRateX);
                    int pixelY = (int) ((worldY - entity2.getWorldY()) * entity2ConversionRateY);
                    
                    try {
                        if (pixelX >= 0 && pixelX < entity2ImageWidth && pixelY >= 0 && pixelY < entity2ImageHeight) {
                            if (((entity2Outline.getRGB(pixelX, pixelY) >> 24) & 0xFF) > 128) {
                                amountCounter++;
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // Nothing
                    }
                }
            }
        }
        if (amountCounter >neededPixelsIntersection){return true;}
        return false;
    }


public static BufferedImage rotateImage(BufferedImage image, double degrees) {
    // Calculate the angle in radians
    double radians = Math.toRadians(degrees);
    
    // Get the width and height of the original image
    int w = image.getWidth();
    int h = image.getHeight();
    
    // Calculate the new width and height of the rotated image
    int newW = (int) Math.round(w * Math.abs(Math.cos(radians)) + h * Math.abs(Math.sin(radians)));
    int newH = (int) Math.round(h * Math.abs(Math.cos(radians)) + w * Math.abs(Math.sin(radians)));
    
    // Create a new buffered image with the new dimensions and a transparent background
    BufferedImage rotatedImage = new BufferedImage(newW, newH, image.getType());
    Graphics2D g2d = rotatedImage.createGraphics();
    
    // Set up the rotation transformation
    AffineTransform transform = new AffineTransform();
    transform.translate(newW / 2, newH / 2);
    transform.rotate(radians);
    transform.translate(-w / 2, -h / 2);
    
    // Draw the original image on the transformed graphics context
    g2d.setTransform(transform);
    g2d.drawImage(image, 0, 0, null);
    g2d.dispose();
    
    return rotatedImage;
}

    

}