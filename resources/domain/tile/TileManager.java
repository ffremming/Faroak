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
        tileHeight.put("tidalSand",4);
        tileHeight.put("shallowWater",3);
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

        // Procedural-tile variants. Each height sits strictly below the primary
        // of every biome that hosts the variant, so the primary always draws its
        // border onto the variant. Within a biome, variants get distinct adjacent
        // heights so adjacent patches also border each other.
        //
        // Grass family — must stay ≤ min(host biome heights). Savanna (15) and
        // seasonal forest (50) cap the most-shared grass tiles.
        tileHeight.put("dryGrassTile", 12); // PLAINS(100), SAVANNA(15), SEASONAL_FOREST(50)
        tileHeight.put("burnedGrass",  13); // SAVANNA(15), SEASONAL_FOREST(50)
        tileHeight.put("meadowGrass",  49); // PLAINS(100), SEASONAL_FOREST(50)
        tileHeight.put("lushGrass",    97); // PLAINS(100), FOREST(101), RAIN_FOREST(103)
        tileHeight.put("mossyGrass",   98); // FOREST(101), RAIN_FOREST(103)
        tileHeight.put("tallGrassTile", 96); // (not yet wired into a biome; reserved)

        // Mud family — SWAMP primary 70.
        tileHeight.put("wetMud",       66);
        tileHeight.put("peatMud",      67);
        tileHeight.put("swampMud",     68);
        tileHeight.put("crackedMud",   69); // reserved

        // Desert family — DESERT primary 10.
        tileHeight.put("desertGravel", 6);
        tileHeight.put("paleSand",     7);
        tileHeight.put("redSand",      8);
        tileHeight.put("dustyDesert",  9);

        // Riverbank family — RIVERBANK primary 8 (aliased to "beach").
        tileHeight.put("riverMud",     5);
        tileHeight.put("gravelPath",   6); // also OK in desert context (same height as desertGravel; never adjacent)

        // Mountain family — MOUNTAIN primary 200.
        tileHeight.put("cobbleRock",   195);
        tileHeight.put("crackedStone", 196);
        tileHeight.put("boulderField", 197);
        tileHeight.put("slateRock",    198);

        // Cave floor stones — painted as winner-take-all blobs by
        // CaveGenerator at FLOOR_ALTITUDE=100. Distinct heights in the same
        // altitude bucket so neighbours of different variants get a soft
        // height-ordered border via the B1/C0 overlay, while the higher
        // wall (altitude 600) still draws its cliff edge onto every floor.
        tileHeight.put("caveCobble",    101);
        tileHeight.put("caveSlab",      102);
        tileHeight.put("caveMossStone", 103);
    }

    
    

    

   
       
}
