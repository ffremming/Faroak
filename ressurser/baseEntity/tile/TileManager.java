package ressurser.baseEntity.tile;

import java.util.HashMap;

import ressurser.baseEntity.sprite.TileSprite;
import ressurser.main.GamePanel;

public class TileManager {
    
    GamePanel panel;

    HashMap<String,TileSprite> tileSprites = new HashMap<>();
    public HashMap<String,Integer> tileHeight = new HashMap<>();

    /**helper for tiles. can keep inportant information about tiles etc... */
    public TileManager(GamePanel panel){
        this.panel = panel;

        
    }

    private void setup(){
        tileHeight.put("plains",100);
        tileHeight.put("mud",20);
        tileHeight.put("moss",80);
        tileHeight.put("desert",10);
        tileHeight.put("ocean",0);
        tileHeight.put("dark_green",101);
        tileHeight.put("savanna",50);
    }

    public TileSprite getTileSprite(String name){

        if (tileSprites.containsKey(name)){
            return tileSprites.get(name);
        }
        else{
            return createTileSprite(name);
        }

    }

    private TileSprite createTileSprite(String name) {
        TileSprite newTileSprite = new TileSprite(name,panel.imageContainer);
        tileSprites.put(name,newTileSprite);
        return newTileSprite;
    }

   
       
}
