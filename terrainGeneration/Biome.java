package terrainGeneration;

import java.awt.Rectangle;
/**
 * temp = x
 * moist = y
 */
public class Biome extends Rectangle{
    
    private String type;
    double maxHeight;
    double minHeight;

    /**
     * @param startTemperatur first param, works as the x value
     * @param startMoist second param, works as the y value
     */
    public Biome(int startTemperatur,int startMoist,int endTemperatur,int endMoist,String type){
        super(startTemperatur,startMoist,endTemperatur-startTemperatur,endMoist-startMoist);

       this.type = type;
    }

    /*
     * based on attributes other than temp/moist
     */
    public Biome(double maxHeight,double minHeight,String type){
        this.type =  type;
        this.maxHeight = maxHeight;
        this.minHeight = minHeight;
    }

    @Override
    public String toString(){
        return type;
    }

    public String getType(){
        return type;
    }

    



    
    
}
