package ressurser.baseEntity;

import java.util.HashMap;

import ressurser.baseEntity.tile.Tile;
import ressurser.main.GamePanel;

public class BaseEntityCollection {

    GamePanel panel;


    //collection of objects that is static!! these can not be changed. 

    //i think only static items will work here. f.eks. seeds osv. tiles does not work. 
    HashMap <String,Tile> tileMap = new HashMap<>();
    



    public BaseEntityCollection(GamePanel panel){
        this.panel = panel;
    }




}
