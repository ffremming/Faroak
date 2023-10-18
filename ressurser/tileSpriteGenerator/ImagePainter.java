package ressurser.tileSpriteGenerator;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.util.HashMap;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.Color;

import static java.time.temporal.ChronoUnit.MILLIS;


public class ImagePainter {
    //backgrount tiles:
    BufferedImage grassBg;
    BufferedImage sandBg;
    BufferedImage waterBg;
    BufferedImage darkforest;
    BufferedImage savanna;
    BufferedImage snow;
    BufferedImage moss;


     HashMap <String,BufferedImage> background = new HashMap<String,BufferedImage>();
     HashMap <String,BufferedImage> borders = new HashMap<String,BufferedImage>();
     HashMap <String,BufferedImage> cornerBorder = new HashMap<String,BufferedImage>();
    HashMap <String,Integer> height = new HashMap<String,Integer>();

    public HashMap <String,BufferedImage> tileSprites = new HashMap<String,BufferedImage>();
    
    
    public ImagePainter(){
        System.out.println("started image generation");
        LocalTime lt1 = LocalTime.now();
        setup();
        //generateMultipleImages();
        //getHashmap();
        LocalTime lt2 = LocalTime.now();
        System.out.println("completed image generation: time used: "+MILLIS.between(lt1,lt2)+" ms");
        
    }
    

   private void setup(){
    try {
    BufferedImage grassB = ImageIO.read(new File("ressurser/tileSpriteGenerator/gbackground.png"));
    BufferedImage mudB = ImageIO.read(new File("ressurser/tileSpriteGenerator/mbackground.png"));
    BufferedImage MossB = ImageIO.read(new File("ressurser/tileSpriteGenerator/Mobackground.png"));
    BufferedImage sandB = ImageIO.read(new File("ressurser/tileSpriteGenerator/sbackground.png"));
    BufferedImage waterB = ImageIO.read(new File("ressurser/tileSpriteGenerator/wbackground.png"));
    BufferedImage dGrassB = ImageIO.read(new File("ressurser/tileSpriteGenerator/dG.png"));
    BufferedImage SaB = ImageIO.read(new File("ressurser/tileSpriteGenerator/Sa.png"));

    BufferedImage grassBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/tileBGR.png"));
    BufferedImage mudBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/borderm.png"));
    BufferedImage mossBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/borderMo.png"));
    BufferedImage sandBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/newSandBorder.png"));
    BufferedImage dGrassBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/dGBorder.png"));
    BufferedImage SaBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/SaB.png"));


    BufferedImage grassCBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/cBorderG.png"));
    BufferedImage mudCBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/cBorderM.png"));
    BufferedImage mossCBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/cBorderMo.png"));
    BufferedImage sandCBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/sandBorderCW.png"));
    BufferedImage dGrassCBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/dGCorner.png"));
    BufferedImage SaCBord = ImageIO.read(new File("ressurser/tileSpriteGenerator/SaC.png"));

    background.put("g",grassB);
    background.put("m",mudB);
    background.put("Mo",MossB);
    background.put("s",sandB);
    background.put("w",waterB);
    background.put("dG",dGrassB);
    background.put("Sa",SaB);

    height.put("g",100);
    height.put("m",20);
    height.put("Mo",80);
    height.put("s",10);
    height.put("w",0);
    height.put("dG",101);
    height.put("Sa",50);
   

    borders.put("g",grassBord);
    borders.put("m",mudBord);
    borders.put("Mo",mossBord);
    borders.put("s",sandBord);
    borders.put("dG",dGrassBord);
    borders.put("Sa",SaBord);

    
    cornerBorder.put("g",grassCBord);
    cornerBorder.put("m",mudCBord);
    cornerBorder.put("Mo",mossCBord);
    cornerBorder.put("s",sandCBord);
    cornerBorder.put("dG",dGrassCBord);
    cornerBorder.put("Sa",SaCBord);
    
   } catch (Exception e) {
    e.printStackTrace();
   }
}

    public  void generateMultipleImages(){
        for (String key:background.keySet()){
            for (String upper:background.keySet()){
                for (String lower:background.keySet()){
                    for (String right:background.keySet()){
                        for (String left:background.keySet()){
                    
                            try {
                                getImage(key,upper,lower,right,left);
                               
                                
                                //BufferedImage image = ImageIO.read(new File("/tileSprites/"+key+"/"+key+upper+lower+right+left+".png"));
                                //tileSprites.put(background+upper+lower+right+left,image);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        
        ImagePainter paint = new ImagePainter();
       

       
    }


    public void getHashmap(){
        for (String key:background.keySet()){
            for (String upper:background.keySet()){
                for (String lower:background.keySet()){
                    for (String right:background.keySet()){
                        for (String left:background.keySet()){
                    
                            try {
                                
                                BufferedImage image = ImageIO.read((getClass().getResource("../tileSprites/"+key+"/"+key+upper+lower+right+left+".png")));
                                tileSprites.put(key+upper+lower+right+left,image);

                            } catch (Exception e) {
                                System.out.println(((getClass().getResource("../tileSprites/"+key+"/"+key+upper+lower+right+left+".png"))));
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public void getSpecificHashmap(String backGround,String upper,String lower,String right,String left){

        if (!background.containsKey(backGround)){return;}

        try{
            BufferedImage image = ImageIO.read((getClass().getResource("../tileSprites/"+backGround+"/"+backGround+upper+lower+right+left+".png")));
            tileSprites.put(backGround+upper+lower+right+left,image);
        } catch (Exception e){
            System.out.println("feil!"+backGround+upper+lower+right+left);
        }
        
    }




    public  void getImage(String backGround,String upper,String lower,String right,String left) throws IOException{

        

        BufferedImage combinedImage = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);

        // Draw input images onto final image
        Graphics2D g = (Graphics2D)combinedImage.getGraphics();

        g.drawImage(background.get(backGround),0,0,null);

        //g.drawImage(rotateImage(image2,90),0, 0, null); //
       
        drawBorders(g,backGround,upper,lower,right,left);
        drawCorners(g,backGround,upper,lower,right,left);


        File directory = new File(System.getProperty("user.dir") +"/ressurser/tileSprites/"+backGround+"/"+backGround+upper+lower+right+left+".png");
               
               
        try{
            ImageIO.write(combinedImage, "png",directory); 
        }
        catch (IIOException ioe){
            System.out.println(directory);
            }       
    }

    private void drawBorders(Graphics2D g,String backGround,String upper,String lower,String right ,String left){
        BufferedImage image = null;
        int degree = 0;


    
        for (int i = 0;i<4;i++){
            if (i == 0){
                if (right!= backGround){
                    if (height.get(backGround)<height.get(right)){
                        image = borders.get(right);
                    } 
                    
                }
                
            }
            
            else if (i == 1){
                if (lower!= backGround){
                    if (height.get(backGround)<height.get(lower)){
                    image = borders.get(lower);
                    }
                }
            }

            else if (i == 2){
                if (left!= backGround){
                    if (height.get(backGround)<height.get(left)){
                    image = borders.get(left);
                    }
                    
                }
            }

            else if (i == 3){
                if (upper!= backGround){
                    if (height.get(backGround)<height.get(upper)){
                        image = borders.get(upper);
                    }
                    }
                }
            


            if (image!= null){
                g.drawImage(rotateImage(image,degree),0,0,null);
                
                image = null;
            }
            degree += 90;

        }
    }


    private void drawCorners(Graphics2D g,String backGround,String upper,String lower,String right ,String left){
        BufferedImage cornerImage = null;
        int degree = 0;


        for (int j = 0;j<4;j++){
            if (j == 0){cornerImage = null;
                if (right!= backGround){
                    if (height.get(backGround)<height.get(right)){
                        if (right.equals(upper)){
                            cornerImage = cornerBorder.get(right);
                        }
                    }
                }  
            }
            
            else if (j == 1){cornerImage = null;
                if (lower!= backGround){
                    if (height.get(backGround)<height.get(lower)){
                        if (lower.equals(right)){
                            cornerImage = cornerBorder.get(lower);
                        }
                    }
                }
            }

            else if (j == 2){cornerImage = null;
                if (left!= backGround){
                    if (height.get(backGround)<height.get(left)){
                        if (left.equals(lower)){
                            cornerImage = cornerBorder.get(left);
                        }
                    }
                }
            }

            else if (j == 3){cornerImage = null;
                if (upper!= backGround){
                    if (height.get(backGround)<height.get(upper)){
                        if (upper.equals(left)){
                            cornerImage = cornerBorder.get(upper);
                        } 
                    }
                }
            }


            if (cornerBorder!= null){
                g.drawImage(rotateImage(cornerImage,degree),0,0,null);
                cornerBorder = null;
            }
            degree += 90;
            }

    
    }


    
    private  BufferedImage rotateImage(BufferedImage buffImage, double angle) {
        double radian = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(radian));
        double cos = Math.abs(Math.cos(radian));
    
        int width = 32;
        int height = 32;
    
        int nWidth = (int) Math.floor((double) width * cos + (double) height * sin);
        int nHeight = (int) Math.floor((double) height * cos + (double) width * sin);
    
        BufferedImage rotatedImage = new BufferedImage(
                nWidth, nHeight, BufferedImage.TYPE_INT_ARGB);
    
        Graphics2D graphics = rotatedImage.createGraphics();
    
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    
        graphics.translate((nWidth - width) / 2, (nHeight - height) / 2);
        // rotation around the center point
        graphics.rotate(radian, (double) (width / 2), (double) (height / 2));
        graphics.drawImage(buffImage, 0, 0, null);
        graphics.dispose();
    
        return rotatedImage;
    }

}
