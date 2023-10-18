package ressurser.chunkSystem;

import java.awt.Rectangle;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.tile.Tile;

public class Chunk extends TreeNode{
    static int amount = 0;
    Tile [][] tileMap;
    ArrayList<BaseEntity> entities = new ArrayList<>();

    // is the chunk loaded. When the game is started the chunk will not be loaded. When chunk is rendered/loaded, boolean value is set true. 
    // this boolean needs to be stored in harddrive. If already loaded, do not need procedural generation of entites, because these is already loaded.
    boolean loaded = false;

    public Chunk(ChunkSystem chunkS,int startXValue, int startYValue, int width, int height, int level) {
        super(chunkS,startXValue, startYValue, width, height, level);

        tileMap = new Tile [CHUNKSIZE][CHUNKSIZE];
        //tileMap = new Tile [height][width];
    }


    @Override
    protected void addChildren(){
        //nothing
        amount ++;
        System.out.println(amount);
        //System.out.println(startXValue+","+ startYValue+";"+ width+","+ height);
       
        
    }


    

    public void addTile(Tile tile,int row,int col){
        tileMap[col] [row] = tile;
    }

    public ArrayList<BaseEntity> getEntities(){
        return entities;
    }


    public Tile [][] getTiles(){
        return tileMap;
    }

    public Tile getTile(int worldX,int worldY){
        return tileMap[worldY/chunkS.panel.tileSize] [worldX/chunkS.panel.tileSize];
    }


    public ArrayList<BaseEntity> getEntitiesInBound(HitBox hitBox){
        ArrayList<BaseEntity> entitiesInBound = new ArrayList<>();

        for (BaseEntity baseEntity : entitiesInBound){
            if (baseEntity.collision(hitBox)){
                entitiesInBound.add(baseEntity);
            }
        }

        return entitiesInBound;
    }

    public ArrayList<BaseEntity> getEntitiesInBound (Rectangle rect,ArrayList<BaseEntity> arrayList){
        for (BaseEntity entity:entities){
            if (rect.contains(entity.getHitBox()) || rect.intersects(entity.getHitBox())){
                arrayList.add(entity);
            }
        }
        return arrayList;
    }
    
    @Override
    protected void getAllChunks(Rectangle rect,ArrayList<Chunk> list){
        list.add(this);
        
        //System.out.println("add chunk"+startXValue+","+startYValue+","+width+","+height);
    }

    


    /*
     * great writing here. adds entities based on the instance.
     */
    @Override
    public void addEntity(BaseEntity entity){

        if (entity instanceof Tile){
            addTile((Tile) entity,entity.getRow(),entity.getCol());
        } else {
            entities.add(entity);
        }

       
    }

    @Override
    public boolean removeEntity (BaseEntity entity) {
        System.out.println("removing entity");
        return entities.remove(entity);
    }
    
    
}
