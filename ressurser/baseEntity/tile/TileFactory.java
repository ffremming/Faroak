package ressurser.baseEntity.tile;

import java.util.ArrayList;
import java.util.HashMap;

import ressurser.baseEntity.Sprite;
import ressurser.main.GamePanel;

public class TileFactory {

    GamePanel panel;

    public final int NONE = 1;
    public final int WILDERNESS = 1;
    public final int CAVE = 2;
    public final int DUNGEON = 3;

    private final boolean COLLISION = true;
    private final boolean NON_COLLISION = false;

    private final boolean ANIMATED = true;
    private final boolean NON_ANIMATED = false;

    

    HashMap<Integer,String> tileNames = new HashMap<>();
    HashMap<Integer,Boolean> tileCollision = new HashMap<>();
    HashMap<Integer,Integer> tileZone = new HashMap<>();
    HashMap<Integer,Boolean> tileAnimated = new HashMap<>();
    HashMap<Integer,Integer> tileHeight = new HashMap<>();
    HashMap<Integer,TileSprite> tileSprites = new HashMap<>();


    //file hashMap
    HashMap<Integer,ArrayList<String>> tileFiles = new HashMap<>();


    public TileFactory(GamePanel panel){
        this.panel = panel;
        
        setUpTileHashMap();
        setUpTileHashMapCollision();
        setUpTileHashMapZones();
        setUpTileHashMapAnimated();
        setUpTileHashMapHeight();
    }

    // when setting up a tile:
    // 0. read the right data
    // 1. create the tile object.
    //2. create the tile sprite
    // add the sprite to the object
    //3. resturn the object

    public Sprite getTileSprite(Tile tile){

        TileSprite newSprite;

        if (tile.zone == WILDERNESS){
            // needs an wilderness dictionary.



            //if all neightbors is from the same zone:
            if (tile.north.zone == WILDERNESS &&tile.west.zone == WILDERNESS&&tile.south.zone == WILDERNESS&&tile.east.zone == WILDERNESS){
                
                

                
            } 
        }

    }
    

    public Tile getNewTile(int keyNumber,int worldCol,int worldRow){
        String tileName = tileNames.get(keyNumber);

        Tile tile = new Tile(panel,tileName,worldRow*panel.tileSize,worldCol*panel.tileSize);
        tile.setCollision(tileCollision.get(keyNumber));
        tile.setAnimated(tileAnimated.get(keyNumber));
        tile.setZone(tileZone.get(keyNumber));

        Sprite tileSprite = new Sprite(tile);
    }


    /**
     * creates hashmap that puts number value for name and the tile toghether.
     */
    private void setUpTileHashMap(){
        tileNames.put(0,"ocean_water");
        tileNames.put(1,"lake_water");
        tileNames.put(2,"plains_grass");
        tileNames.put(3,"dark_grass");
        tileNames.put(4,"mud");
        tileNames.put(5,"rocky_mountain");
        tileNames.put(6,"moss");
        tileNames.put(7,"savanna");
        tileNames.put(8,"ocean_sand");

      
    }

    private void setUpTileHashMapCollision(){
        tileCollision.put(0,COLLISION);
        tileCollision.put(1,COLLISION);
        tileCollision.put(2,NON_COLLISION);
        tileCollision.put(3,NON_COLLISION);
        tileCollision.put(4,NON_COLLISION);
        tileCollision.put(5,NON_COLLISION);
        tileCollision.put(6,NON_COLLISION);
        tileCollision.put(7,NON_COLLISION);
        tileCollision.put(8,NON_COLLISION);
    }

    private void setUpTileHashMapZones(){
        tileZone.put(0,WILDERNESS);
        tileZone.put(1,WILDERNESS);
        tileZone.put(2,WILDERNESS);
        tileZone.put(3,WILDERNESS);
        tileZone.put(4,WILDERNESS);
        tileZone.put(5,WILDERNESS);
        tileZone.put(6,WILDERNESS);
        tileZone.put(7,WILDERNESS);
        tileZone.put(8,WILDERNESS);
    }

    private void setUpTileHashMapAnimated(){
        tileAnimated.put(0,ANIMATED);
        tileAnimated.put(1,ANIMATED);
        tileAnimated.put(2,NON_ANIMATED);
        tileAnimated.put(3,NON_ANIMATED);
        tileAnimated.put(4,NON_ANIMATED);
        tileAnimated.put(5,NON_ANIMATED);
        tileAnimated.put(6,NON_ANIMATED);
        tileAnimated.put(7,NON_ANIMATED);
        tileAnimated.put(8,NON_ANIMATED);
    }

    private void setUpTileHashMapHeight(){
        tileHeight.put(0,-1);
        tileHeight.put(1,-1);
        tileHeight.put(2,95);
        tileHeight.put(3,100);
        tileHeight.put(4,10);
        tileHeight.put(5,0);
        tileHeight.put(6,110);
        tileHeight.put(7,90);
        tileHeight.put(8,5);
    }

    
    private void setUpTileHashMapSprites(){
        tileSprites.put(0,new TileSprite(tileNames.get(0)+".png",panel));
        tileSprites.put(1,new TileSprite(tileNames.get(0)+".png",panel));
        tileSprites.put(2,new TileSprite(tileNames.get(0)+".png",panel));
        tileSprites.put(3,new TileSprite(tileNames.get(0)+".png",panel));
        tileSprites.put(4,new TileSprite(tileNames.get(0)+".png",panel));
        tileSprites.put(5,new TileSprite(tileNames.get(0)+".png",panel));
        tileSprites.put(6,new TileSprite(tileNames.get(0)+".png",panel));
        tileSprites.put(7,new TileSprite(tileNames.get(0)+".png",panel));
        tileSprites.put(8,new TileSprite(tileNames.get(0)+".png",panel));
    }

    
       
    
}
