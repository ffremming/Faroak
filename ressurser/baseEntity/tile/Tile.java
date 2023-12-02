package ressurser.baseEntity.tile;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Sprite;
import ressurser.baseEntity.gameObjects.GameObject;
import ressurser.main.GamePanel;

public class Tile extends BaseEntity{
    
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

    public BufferedImage getImage(){
        return image;
    }

    // not sure if i need this.
    public void addSprite(Sprite sprite){
        this.sprite = sprite;
    }


    //idea that every tile contain gameobject, but not implementet. should not be done yet.
    //well, could work for static objects, but not so good with non-statics. would not do this.
    public void addGameObject(GameObject go){
        
    }

    /**
     * not yet implemented, implement if this will be used.
     */
    private void setNeighBors(Tile north,Tile south,Tile east,Tile west){
        addNorthNeighBor(north);
        addSouthNeighBor(south);
        addWestNeighBor(west);
        addEastNeighBor(east);
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
