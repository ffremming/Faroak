package resources.domain.tile;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;
import resources.presentation.image.ImageContainer;

import java.util.HashMap;


import resources.app.GamePanel;

public class TileManager {
    
    GamePanel panel;
    public HashMap<String,Integer> tileHeight = new HashMap<>();

    /**helper for tiles. can keep important information about tiles etc... */
    public TileManager(GamePanel panel){
        this.panel = panel;
        setup();

        
    }
    /** Default rank for biomes that don't have an explicit height registered. */
    private static final int DEFAULT_HEIGHT = 100;

    private int getHeight(Tile tile){
        Integer h = tileHeight.get(tile.getName());
        return h != null ? h : DEFAULT_HEIGHT;
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



    /**returns true if tile2 is higher than tile */
    public boolean cliffDifference(Tile tile, Tile tile2){

        if (!tile.getName().equals("ocean")){
            int hundreds1 = (tile.altitude) / 50;
            int hundreds2 = (tile2.altitude) / 50;
            
            // Check if the hundreds digits are different
            return hundreds1 < hundreds2;
        }
        return false;
       
        
    }

    private void setup(){
        tileHeight.put("plains",100);
        tileHeight.put("grass",87);
        tileHeight.put("grass2",86);
        tileHeight.put("grass3",85);
        tileHeight.put("grass4",84);
        tileHeight.put("grass5",83);
        tileHeight.put("grass6",82);
        tileHeight.put("grass7",81);

        tileHeight.put("mud",20);
        tileHeight.put("swamp",70);
        tileHeight.put("desert",10);
        tileHeight.put("beach",7);
        tileHeight.put("wetBeach",5);
        tileHeight.put("ocean",0);
        tileHeight.put("forest",101);
        tileHeight.put("savanna",15);
        tileHeight.put("seasonal forest",50);

        // Biomes added by ProceduralGen / BiomeRegistry
        tileHeight.put("snowy taiga",  102);
        tileHeight.put("snowy Tundra", 95);
        tileHeight.put("rain forest",  103);
        tileHeight.put("rain_forest",  103);
        tileHeight.put("riverbank",    8);
        tileHeight.put("river",        0);
        tileHeight.put("mountain",     200);
    }

    
    

    

   
       
}
