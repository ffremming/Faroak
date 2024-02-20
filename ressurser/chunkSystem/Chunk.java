package ressurser.chunkSystem;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.playable.Playable;
import ressurser.baseEntity.tile.Tile;

public class Chunk extends TreeNode{
    static int amount = 0;
   
    ArrayList<BaseEntity> entities = new ArrayList<>();

    // is the chunk loaded. When the game is started the chunk will not be loaded. When chunk is rendered/loaded, boolean value is set true. 
    // this boolean needs to be stored in harddrive. If already loaded, do not need procedural generation of entites, because these is already loaded.
    boolean generated = false;
    boolean loaded = false;
    static int amtLoaded = 0;
    static int amtGenerated = 0;

    //i want all chunks to always forget the tile contents, but always remember entities(not tiles)

    public Chunk(ChunkSystem chunkS,int x, int y, int width, int height) {
        super(chunkS,x, y, width, height);
        
       
        //tileMap = new Tile [height][width];
    }

    @Override
    protected void addChildren(){
        //nothing
        amount ++;
    }

    

    public ArrayList<BaseEntity> getEntities(){
        return entities;
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

    public ArrayList<BaseEntity> getEntitiesInBound(Point p){
        ArrayList<BaseEntity> entitiesInBound = new ArrayList<>();

        for (BaseEntity baseEntity : entitiesInBound){
            if (baseEntity.getHitBox().contains(p)){
                entitiesInBound.add(baseEntity);
            }
        }
        return entitiesInBound;
    }

    

    /**
     * returns a list of all entities in the bound
     */
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
     * adds entities to entity pile
     */
    @Override
    public void addEntity(BaseEntity entity){
        if (entity!= null){
            entities.add(entity);
        }
       
        
    }


    /**
     * removes given entity
     */
    @Override
    public boolean removeEntity (BaseEntity entity) {
        return entities.remove(entity);
    }
    
    protected boolean hasEntities(){
         return (entities.size()>0);
    }

    protected ArrayList<BaseEntity> getAllEntities(){
        return entities;
    }   

    /**
     * returns first Tile that hitbox contains point.
     * 
     */
    protected BaseEntity getTile(Point point){

        for (BaseEntity entity:getEntities()){
            if (entity instanceof Tile){
                if (entity.getHitBox().contains( point)){
                return entity;
                }
            }
        }
        return null;
    }
    /**
     * get first entity that is not tile. entity must contain point
     */
    protected BaseEntity getEntity(Point point){

        for (BaseEntity entity:getEntities()){
            if (!(entity instanceof Tile)){
                if (entity.getHitBox().contains( point)){
                return entity;
                }
            }
        }
        return null;
    }
    /**adds entities that contains point in list */
    public ArrayList<BaseEntity> getEntitiesInPoint(Point point, ArrayList<BaseEntity> entitiesList){
        
        for (BaseEntity entity:getEntities()){
            
            if (entity.getHitBox().contains(point)){
               
                entitiesList.add(entity);
            }
        }
        return entitiesList;
    }

    

    private ArrayList<BaseEntity> getNeighbors(BaseEntity entity){
        //TODO NOW
        return null;
    }



    public void writeInfo(){
        System.out.println("entities:"+ entities.size() +"coords: x:"+x+"y:"+y+"\n" );
        System.out.println(getBounds());
        for (BaseEntity entity :entities){
            System.out.print("navn: "+entity.getName() +",");
            
        }

        System.out.println("\n\n");
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
       amtGenerated ++;

    }
    /*generate tiles, should be used every time a chunk is loaded */
    private void generateTiles(){
        addEntitiesToChunk();
        //connectEntities();
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
     * only does this if this is not loaded.
     */
    public void load(){

        if (!loaded){
            
            if (generated){
            generateTiles();
            loadEntitiesFromFile();
            }
            else{
                initialLoad();
            }
            
        }
        loaded = true;
        amtLoaded++;

        
        
        
        
    }

    /**should not be here.....!! //TODO */
    private Tile getSingelTile(int worldX,int worldY){
        
        //loader algorithm..
        //method returns biome type- which is a streubg
        String biomeType =  chunkS.proceduralGen.calculateBiomeString(worldX, worldY);
        int height = (int)(chunkS.proceduralGen.getHeightValue(worldX,worldY)*1000);
        return new Tile(chunkS.panel,biomeType,worldX,worldY,height);
        
    }

    private BaseEntity getSingelEntity(int worldX,int worldY){

        return chunkS.entityFactory.getEntity(worldX,worldY);


    }

    private void addEntitiesToChunk(){
       
        for (int x2 = 0;x2<width;x2+= chunkS.panel.tileSize){
            for (int y2 = 0;y2<width;y2+=chunkS.panel.tileSize){
                
                addEntity(getSingelTile(x+x2,y+y2));

                addEntity(getSingelEntity(x+x2,y+y2));
            }
        }
    }

    void connectTiles(){
       
        for (BaseEntity baseEntity:entities){
            if (baseEntity instanceof Tile){
                ((Tile)baseEntity).setNeighBors();
            }
        }
    }

    /**checks if any entities has moved out of the specified chunk, removes them if they have, and adds them to correct chunk. */
    void flush(){

        ArrayList<BaseEntity> toBeRemoved = new ArrayList<>();
        for (BaseEntity baseE:entities){
            //should be changed to moveable
            if (baseE instanceof Playable){
                if (!(this.collision(baseE.getHitBox()))){

                    //to stop concurrentmodification
                    toBeRemoved.add(baseE);
                    
                    
                }
            }
        }
        for (BaseEntity baseE:toBeRemoved){
            //eneity has moved from this chunk, and need to be placed in another chunk.
            entities.remove(baseE);
            chunkS.addEntity(baseE);
        }
    }


    public void unLoad(){
        loaded = false;
        entities = null;
    }
    

    
}
