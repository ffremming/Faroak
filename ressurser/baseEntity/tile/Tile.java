package ressurser.baseEntity.tile;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Sprite;
import ressurser.baseEntity.gameObjects.GameObject;
import ressurser.main.GamePanel;

public class Tile extends BaseEntity{
    
    final int NORTH = 0;
    final int SOUTH = 2;
    final int WEST = 3;
    final int EAST = 1;



    Tile [] neigbors = new Tile [4];
    Tile north;
    Tile south;
    Tile east;
    Tile west;

    int zone;

    Sprite sprite;

    //midlertidig
    public BufferedImage image;



    public Tile(GamePanel panel,String name, int worldX, int worldY) {
        super(panel, name, worldX, worldY, (short)panel.tileSize,(short)panel.tileSize, (short)panel.tileSize, (short)panel.tileSize, (short)0, (short)0);
        
        
        //TODO Auto-generated String name,constructor stub

        //generate sprites
        //spriteHandler = new SpriteHandlerTiles(this);


        
    }

    

    // not sure if i need this.
    public void addSprite(Sprite sprite){
        this.sprite = sprite;
    }


    //idea that every tile contain gameobject, but not implementet. should not be done yet.
    //well, could work for static objects, but not so good with non-statics. would not do this.
    public void addGameObject(GameObject go){
        
    }

    /**@return null - if direction is given is wrong OR there is no TILE */
    private Tile getTile(int direction){
        if (direction == NORTH){
            return panel.chunkSystem.getTile(new Point(worldX+width/2,worldY-width/2));
        }
        else if (direction == EAST){
            return panel.chunkSystem.getTile(new Point(worldX+width*3/2,worldY+width/2));
        }
        else if (direction == SOUTH){
            return panel.chunkSystem.getTile(new Point(worldX+width/2,worldY+width*3/2));
        }
        else if (direction == WEST){
            return panel.chunkSystem.getTile(new Point(worldX-width/2,worldY+width/2));
        }

        return null;
    }




    /**
     * not yet implemented, implement if this will be used.
     */
    public void setNeighBors(){
        addNorthNeighBor(getTile(NORTH));
        addSouthNeighBor(getTile(SOUTH));
        addWestNeighBor(getTile(WEST));
        addEastNeighBor(getTile(EAST));
    }


    private void addNorthNeighBor(Tile tile){
        north = tile;
    }
    private void addSouthNeighBor(Tile tile){
        south = tile;
    }
    private void addWestNeighBor(Tile tile){
        west = tile;
    }
    private void addEastNeighBor(Tile tile){
        east = tile;
    }

    

    public void setAnimated(Boolean boolean1) {
        animated = boolean1;
    }

    public void setZone(Integer thisZone) {
        zone = thisZone;
    }



    
}
