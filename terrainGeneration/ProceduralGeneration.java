package terrainGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import ressurser.baseEntity.tile.Tile;
import ressurser.main.GamePanel;
import ressurser.worldGeneration.OpenSimplex2S;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ProceduralGeneration {
    
    static double avg = 0;
    static int amount = 0;
    static double sumValue = 0;

     int seed;
     int moistSeed;
     int tempSeed;
     int TILESIZE = 32;
     int continentalSeed;
     int peaksSeed;
    GamePanel panel;


    // koefficent
    double heightKoefficent = 0.5;
    double temperatureKoefficent = 1;
    double moistKoefficent = 1;


    //frequenzy:

    double standardFrequency = 0.005/24;
    final double RIVERFREQUENZY =  1/24;
    double moistFrequency = 0.03 /24;
    double heatFrequency = 0.02/24;
    double continentalFreq = 0.001/24;
    double peaksFreq = 0.01/24;

    //rects of biomes
    ArrayList<Biome> biomeRects = new ArrayList<>();
    HashMap <Biome,Integer> colorMap = new HashMap<>();

    //boolean modes
    boolean hightColor = false;

    //colors
    private int green = 0xA0C080;
	private int darkGreen = 0x408000;
	private int sand = 0xffe7c3;
	private int moss = 0x4E4E35;
	private int water = 0x0000ff;;
	private int mountain =0x808080 ;
	private int ice = 0xBFEFFF;
	private int savannaC = 0xCDBE70;
	private int sForest = 0x629632;
	private int rainFr = 0x78AB46;
	private int black =  0x000000;

    //lvls
    double oceanLvl = 0;
    double beachLvl = 0.03;


    public ProceduralGeneration(){
        //Setting a random seed
        newSeed();

        

        

        //setup
        setupBiomes();
        System.out.println(getBiome(100*32,500*32));

    }

    /**
     * method takes in the startvalue of x and Y, as well as chunkSize.
     * 
     * @param startXValue is the real worldX of the tile.aka 32 instead of 1
     * @param chunkSize is the size of the chunk, example 16
     * 
     * @return 2D Array of tiles in the correct order.
     */
    public Tile [][] getArrayOfTiles(int startXValue,int startYvalue, int chunkSize){

        Tile [][] tileArray = new Tile [chunkSize][chunkSize];

        for (int x = 0;x <chunkSize;x++){
            
            for (int y = 0;y<chunkSize;y++){
                Tile tile = getTile(startXValue + (x*TILESIZE),startYvalue + (y*TILESIZE));

                //dobbel sjekk 2d ARRAY
                tileArray[y][x] = tile;
            }
        }
        return tileArray;

    }

    /**
     * @param xValue is the real worldX of the tile.aka 32 instead of 1  - this value can be negative!
     * uses procedural generation to get the right Tile.. 
     */
    private void getTile(int xValue,int yValue){
        // .... procedural generation here...

        
            
    }

    /**
     * iterates thought
     * @param worldX takes in real coord - converts it into tileCoord in function
     * @param iteration defines how many iterations the agorithms run. more is more detailed and sharp edges, more noise in edges. less is smoother.
     * 
     */
    private double getNoiseValue(long seed,double frequency,int worldX,int worldY,int iterations){

        int x = worldX/32;
        int y = worldY/32;

        double value = 0;

        //multiplier decreases by half every iteration
        double multiplier = 1;
        //size is how large the value is compared to original.
        double size = 0;
        
        for (int i = 0;i<iterations;i++){
            value += OpenSimplex2S.noise2_ImproveX(seed, x * frequency, y * frequency)*multiplier;
            frequency*=2;
            size += multiplier;
            multiplier/=2;
        }
        value = value/size;

        sumValue+= value;
        amount++;
        //System.out.println(sumValue/amount);

        
		return value;
	}

    /**
     * @param worldX is real coordinates
     * 
     * function produces moist and heat noice, and returns the corresponding biome.
     * define frequency and seed inside.
     */
    private Biome getBiome(int worldX,int worldY){
        //producing the moist noicevalue
        double moistValue = getMoistValue(worldX,worldY);

        //producing the heat noicevalue
        double heatValue = getTemperatureValue(worldX,worldY);
        

        

        
        return getBiomeFromList(heatValue,moistValue);

    }

    /**
     * searches thorught all biomes and finds out where the point is.
     */
    private Biome getBiomeFromList(double heat,double moist){

        int heat2 = (int)(heat*50);
        int moist2 = (int)(moist*50);

        Point p = new Point(heat2,moist2);
        

        for (Biome biome:biomeRects){
            if (biome.contains(p)){
                return biome;
            }
        }

        //System.out.println(heat2+","+moist2);
        return null;
    }

    private void drawBiomes(){

        BufferedImage image =  new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);;
        
        for (int temperature = -50;temperature<50;temperature ++){
            for (int moist = -50;moist<50;moist ++){
                 if (getBiomeFromList((double)temperature/50,(double)moist /50) != null){
                        int rgb = colorMap.get(getBiomeFromList((double)temperature/50,(double)moist /50));
                        image.setRGB(temperature +50, moist +50, rgb);
                    
            }
        }
    }
     try {
        ImageIO.write(image, "png", new File("ressurser/worldGeneration/biomes.png"));
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
        
    }


    private void setupBiomes(){
    Biome snowyTundra = new Biome(-51,-51,-25,0,"Snowy Tundra");
    Biome plains = new Biome(-25,-51,20,0,"plains");
    Biome desert = new Biome(20,-51,51,-10,"desert");
    Biome snowyTaiga = new Biome(-51,0,-25,51,"snowy taiga");
    Biome seasonalForest = new Biome(-25,0,0,51,"seasonal forest");
    Biome savanna = new Biome(0,-10,51,0,"savanna");
    Biome forest = new Biome(0,0,51,25,"forest");
    Biome swamp = new Biome(0,25,25,51,"swamp");
    Biome rainForest = new Biome(20,25,51,51,"rain forest");

    //others:
    

    biomeRects.add(snowyTundra);
    biomeRects.add(plains);
    biomeRects.add(desert);
    biomeRects.add(snowyTaiga);
    biomeRects.add(seasonalForest);
    biomeRects.add(savanna);
    biomeRects.add(forest);
    biomeRects.add(swamp);
    biomeRects.add(rainForest);
   

    

    colorMap.put(snowyTundra,ice);
    colorMap.put(snowyTaiga,ice);
    colorMap.put(plains,green);
    colorMap.put(desert,sand);
    colorMap.put(snowyTundra,ice);
    colorMap.put(seasonalForest,sForest);
    colorMap.put(savanna,savannaC);
    colorMap.put(forest,darkGreen);
    colorMap.put(rainForest,rainFr);
    colorMap.put(swamp,moss);

    

    }


    /**
     * retrieves the image to file
     */
    private void paintMap(int startX,int startY,int width,int height){

        try{
            BufferedImage image = getImage(startX,startY,width,height);
            ImageIO.write(image, "png", new File("ressurser/worldGeneration/noise2.png"));
        }
            catch (IOException e){
            e.printStackTrace();
        }
	} 

    /**
     * generate and return the painted terrain-image
     */
    public BufferedImage getImage(int startX,int startY,int width,int height){
        
        int rgb;
        BufferedImage image = new BufferedImage(width-startX, height-startY, BufferedImage.TYPE_INT_RGB);
        for (int y = startY; y < height; y++)
        {
            for (int x = startX; x < width; x++){
                    
                    rgb = getColor(x*32,y*32);
                    image.setRGB(x-startX, y-startY, rgb);

            }
        }
		return image;
    }
        
    private int getColor(int worldX,int worldY){

        if (hightColor){
            return calculateHightColor(worldX,worldY);
        } else{
            return calculateBiomeColor(getBiome(worldX,worldY),worldX,worldY);
        }
        
        
    }

    /**
     * @param worldX and worldY - takes in coords, get the hightValue inside, then convertas the value to a color.
     */
    private int calculateHightColor(int worldX,int worldY){

        // Ensure the value is within the range [-1, 1]
        double value = getHeightValue(worldX,worldY);

        value = Math.max(-1.0, Math.min(1.0, value));
        
        // Map the value to the hue in the HSL color space
        float hue = (float) ((value + 1.0) / 2.0 * 360.0);
        
        // Create the corresponding color
        Color color = Color.getHSBColor(hue / 360.0f, 1.0f, 1.0f);
        
        // Convert the color to an RGB integer
        return color.getRGB();
    }

    private int calculateBiomeColor(Biome biome,int worldX,int worldY ){

        double height = getHeightValue(worldX,worldY);
        if (height < oceanLvl){
            return water;
        
        }else if (height< beachLvl){
            return sand;
        
        } else{
            try{
                int color = colorMap.get(biome);
                return color;
            } catch (NullPointerException e){
                System.out.println( "feil: "+","+getTemperatureValue(worldX,worldY));
            }
            return 0;
        }

       
    }

    private double getMoistValue(int worldX, int worldY){
        double moist = getNoiseValue(moistSeed,moistFrequency,worldX,worldY,5);

        double continentalness = getNoiseValue(continentalSeed,continentalFreq,worldX,worldY,3);

        double alteredMoist = getAlteredValue(moist,1,-continentalness);

        return alteredMoist;
    }


    private double getTemperatureValue(int worldX,int worldY){

        double temperature = getNoiseValue(tempSeed,heatFrequency,worldX,worldY,5);

        double height = getHeightValue(worldX,worldY);

        double alteredTemperature = getAlteredValue(temperature,temperatureKoefficent,-height);
        

        if (alteredTemperature< -1 ){
            alteredTemperature = -1;
        }
        else if (alteredTemperature > 1){
            alteredTemperature = 1;
        }
        return alteredTemperature;
    }

    private double getAlteredValue(double originalValue,double koefficent,double ChangeValue){
        double alteredValue = originalValue +(koefficent *(ChangeValue));
        return alteredValue;
    }

    /*
     * mainpualtes the hight value 
     * takes the original height value and adds continentalness*koefficent
     * should only return values between 1 and -1
     */
    private double getHeightValue(int worldX,int worldY){


        double height = getNoiseValue(seed,standardFrequency,worldX,worldY,4);

        double continentalness = getNoiseValue(continentalSeed,continentalFreq,worldX,worldY,3);
        
        double peakNoice = getNoiseValue(peaksSeed,peaksFreq,worldX,worldY,3);
        
        //not always a positive number, only takes the peaks.
        double peakValue = Math.pow(peakNoice,2)/3;


        double alteredHeight = getAlteredValue(height,heightKoefficent,continentalness) + peakValue;

        //testing:
        //System.out.println(height +",altered:"+alteredHeight + " -- "+(1+heightKoefficent)*continentalness + ","+continentalness);

        //we need continentalness, erosion and peeks

        if (alteredHeight< -1 ){
            alteredHeight = -1;
        }
        else if (alteredHeight > 1){
            alteredHeight = 1;
        }

        

        return alteredHeight;


    }

    private int getRandomSeed(){
        Random random = new Random();
        int seed = random.nextInt(0,9999);
        return seed;
    }

    /**
     *can be called by another object.
     *set new seed for all seed attributes
     */
    
    public void newSeed(){
        seed = getRandomSeed();
        tempSeed = getRandomSeed();
        moistSeed = getRandomSeed();
        continentalSeed = getRandomSeed();
        peaksSeed = getRandomSeed();
    }

    public static void main(String []args){
        ProceduralGeneration pg = new ProceduralGeneration();
        pg.paintMap(0,0,20,20);

        pg.drawBiomes();
    }

    public void setStandardFrequenzy(double frequency){
        standardFrequency = frequency;
    }

    public void setMoistFrequenzy(double frequency){
        moistFrequency = frequency;
    }

    public void setTempFrequenzy(double frequency){
        heatFrequency = frequency;
    }

    public void setK(double newValue){
        heightKoefficent = newValue;
    }

    public void setTemperatureK(double newKValue) {
        temperatureKoefficent = newKValue;
    }

    public void setContinentalFrequency(double newContinentalValue) {
        continentalFreq = newContinentalValue;
    }

     public void setPeaksFrequency(double newPeaksValue) {
        peaksFreq = newPeaksValue;
    }

    public void setHightColor() {
        if (hightColor){
            hightColor = false;
        } else {
            hightColor = true;
        }
    }

    public void setMoistK(double newKValue) {
        moistKoefficent = newKValue;
    }

    



    //needs a forcefucntion to force land and islands - this can be replaced by another function that defines goelogical phenomens.

    //want riverlands as well as normal plains with waterboides, and i want islands as well as non-islands

}
