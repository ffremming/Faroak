package ressurser.baseEntity.tile;

import java.util.HashMap;
import java.util.NoSuchElementException;

import ressurser.baseEntity.sprite.TileSprite;
import ressurser.main.GamePanel;

public class TileManager {
    
    GamePanel panel;

    HashMap<String,TileSprite> tileSprites = new HashMap<>();
    public HashMap<String,Integer> tileHeight = new HashMap<>();

    /**helper for tiles. can keep inportant information about tiles etc... */
    public TileManager(GamePanel panel){
        this.panel = panel;
        setup();

        
    }
    /**
     * thows exception if the element does not have a corresponding name. 
     */
    private int getHeight(Tile tile){
        


        if (tileHeight.containsKey(tile.getName())){
            return tileHeight.get(tile.getName());
        } else{
            throw new NoSuchElementException(tile.getName());
            
        }
    }

    /**returns true if tile2 is higher than tile */
    public boolean isHigher(Tile tile, Tile tile2){

        if (tile.compareTo(tile2) && !tile.getName().equals("ocean")){
            int hundreds1 = (tile.altitude) / 50;
            int hundreds2 = (tile2.altitude) / 50;
            
            // Check if the hundreds digits are different
            return hundreds1 < hundreds2;
        } else {
            return (getHeight(tile2) >getHeight(tile));
        }

        

        //
    }

    private void setup(){
        tileHeight.put("plains",100);
        tileHeight.put("mud",20);
        tileHeight.put("swamp",80);
        tileHeight.put("desert",10);
        tileHeight.put("beach",5);
        tileHeight.put("ocean",0);
        tileHeight.put("forest",101);
        tileHeight.put("savanna",50);
        tileHeight.put("seasonal forest",50);
    }

    
    

    

   
       
}
