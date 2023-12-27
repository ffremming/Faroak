package ressurser.chunkSystem;

import java.awt.Rectangle;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.tile.Tile;

public class Chunk extends TreeNode{
    static int amount = 0;
    Tile [][] tileMap;  //still not a valid option
    ArrayList<BaseEntity> entities = new ArrayList<>();

    // is the chunk loaded. When the game is started the chunk will not be loaded. When chunk is rendered/loaded, boolean value is set true. 
    // this boolean needs to be stored in harddrive. If already loaded, do not need procedural generation of entites, because these is already loaded.
    boolean generated = false;

    //i want all chunks to always forget the tile contents, but always remember entities(not tiles)

    public Chunk(ChunkSystem chunkS,int startXValue, int startYValue, int width, int height) {
        super(chunkS,startXValue, startYValue, width, height);
        
        tileMap = new Tile [CHUNKSIZE][CHUNKSIZE];
        //tileMap = new Tile [height][width];
    }

    @Override
    protected void addChildren(){
        //nothing
        amount ++;
        
        
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
        
    }
    /*
     * adds chunk to list
     */
    protected void getAllChunks(ArrayList<Chunk> list){
        list.add(this);
        
    }
    /*
     * great writing here. adds entities based on the instance.
     */
    @Override
    public void addEntity(BaseEntity entity){

        if (entity instanceof Tile && false){   //TODO not supposed to be like this, problem with the incex allocation system.
            addTile((Tile) entity,entity.getRow()-startXValue/chunkS.panel.tileSize ,entity.getCol()-startXValue/chunkS.panel.tileSize);
        } else {
            entities.add(entity);
        }
    }

    @Override
    public boolean removeEntity (BaseEntity entity) {
        System.out.println("removing entity");
        return entities.remove(entity);
    }
    
    protected boolean hasEntities(){
         return (entities.size()>0);
    }

    protected ArrayList<BaseEntity> getAllEntities(){
        return entities;
    }   

    public void writeInfo(){
        System.out.println("entities:"+ entities.size() +"coords: x:"+startXValue+"y:"+startYValue+"\n" );
        System.out.println(getBounds());
        for (BaseEntity entity :entities){
            System.out.println("navn: "+entity.getName());
            
        }

        System.out.println("");
    }

    /**
     * init load, loads all for the first time.
     * -generate and add:
     * -tiles
     * -entities
     */
    private void initialLoad(){
       generateTiles();
       generateEntities();
       generated = true;
    }
    /*generate tiles, should be used every time a chunk is loaded */
    private void generateTiles(){
        addEntitiesToChunk(startXValue,startYValue,width);
    }
    /**generates entities, should only be ran first time in the chunks */
    private void generateEntities(){
        //TODO
    }   

    /**
     * long before this is added.
     */
    private void loadEntitiesFromFile(){
        //TODO
    }

    /**
     * adds tiles, and load entities from file
     */
    void load(){
        if (generated){
            generateTiles();
            loadEntitiesFromFile();
        }
        else{
            initialLoad();
        }
        
    }

    private Tile getSingelTile(int worldX,int worldY){
        
        //loader algorithm..
        //method returns biome type- which is a streubg
        String biomeType =  chunkS.proceduralGen.calculateBiomeString(worldX, worldY);
        
        return new Tile(chunkS.panel,biomeType,worldX,worldY);
        
    }

    private void addEntitiesToChunk(int startX,int startY, int width){
       
        for (int x = 0;x<width;x+= chunkS.panel.tileSize){
            for (int y = 0;y<width;y+=chunkS.panel.tileSize){
                
                addEntity(getSingelTile(startX+x,startY+y));
            }
        }
    }

    
}
