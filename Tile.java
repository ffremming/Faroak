

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;




public class Tile {
    public BufferedImage image; 
    public boolean collision = false;
    public boolean animated;
    public BufferedImage bilde2;
    public Boolean water = false;
    public Boolean grass = false;
    public String type = "null";

    public boolean plantable = false;


    //not yet implemented - written 12/09/23
    private Tile southBorder;
    private Tile northBorder;
    private Tile eastBorder;
    private Tile westBorder;



    
    public Tile(String type){
        this.type = type;
    }

    public Tile(boolean type){
        this.animated = type;
    }

    

    //not yet implemented, just idea.
   //idea to draw from the tile, but not a good idea.
   //drawing shouyld be done by tilemanager / drawingmanager.

   //drawing needs to be done from a place where the player coordinates is known, as well as the tile type sprite, the tile border values

   //a function can be made to get a tile as parameter, then draw from method.



    public void draw(){

    }
}
