package ressurser.chunkSystem;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;


import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Entity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.tile.Tile;
import ressurser.chunkSystem.terrainGeneration.ProceduralGeneration;
import ressurser.main.GamePanel;
import ressurser.main.GUIMenu.MenuPanel;



public class ChunkSystem {
    
    GamePanel panel;

    //updates all entities that should be rendered.
    public ArrayList<BaseEntity> semiStaticEntitiesRendered = new ArrayList<BaseEntity>();
    
    //renderdistance is the distance from the player to the border of where entities is rendered.
    int renderDistance ;
    TreeNode mother;
    ProceduralGeneration proceduralGen;
    //
    final int SIZEPOW = 7;// 5072 chunks.

    HashMap <String,Tile> tileHashMap = new HashMap<String,Tile>();



    /**
     * the chunksystem starts by creating the mother treenode, that has an set size. this treenode will create childnodes til the children are small enought to be consideed a chunk.
     * 
     * if the chunksystem is not big enought, it should create another mother.
     */
    public ChunkSystem(GamePanel panel){
        this.panel = panel;
        proceduralGen = new ProceduralGeneration();
        //this should be lower. but not sure yet.
        renderDistance = 64*panel.tileSize;
        //TODO 
        
        

        mother = new TreeNode(this,-(int)Math.pow(2,SIZEPOW)*panel.tileSize/2,-(int)Math.pow(2,SIZEPOW)*panel.tileSize/2,(int)Math.pow(2,SIZEPOW)*panel.tileSize,(int)Math.pow(2,SIZEPOW)*panel.tileSize);

        setUpTest();
    }

    private void setUpTest(){
        

        //removeEntitiesInBound()
       

        
        generateTileInAllChunks();

        //writeALlInfo();
    }

    /**
     * returns all entities in bound. in bound mean that the hitbox collides.
     * search from nearby entities. return all entities that hitboxes collide.
     * 
     * @return list of all entities that collides with hitbox
     */
    public ArrayList<BaseEntity> getEntitiesInBound(HitBox hitBox){
        System.out.println(hitBox);
        ArrayList<BaseEntity> entitiesInBound = new ArrayList<>();
        updateSemiStaticEntitiesRendered(hitBox);

        for (BaseEntity baseEntity : semiStaticEntitiesRendered){
            if (baseEntity.collision(hitBox)){
                if (baseEntity!= hitBox.getEntity()){
                    entitiesInBound.add(baseEntity);
                }
            }
        }
        return entitiesInBound;   
    }

    /**
     * returns all entities in bound. in bound mean that the hitbox collides.
     * search from all entities, not just nearby(still fast tho). return all entities that hitboxes collide.
     * 
     * @return list of all entities that collides with hitbox
     */
    
     public ArrayList<BaseEntity> getEntitiesInBound(Rectangle rect){
        ArrayList<BaseEntity> entitiesInBound = new ArrayList<>();
        return mother.getEntitiesInBound(rect,entitiesInBound);
    }

    /**
     * the method updates the enities that is nearby the baseentity in the parameter.
     * the method should not be called too frequently for perfomance.
     * the amount of room the method search for is decided by the renderdistance.
     */

    public void updateSemiStaticEntitiesRendered(BaseEntity entity){
      
        
        //load all chunks that is involved in the range of the render distance.

        semiStaticEntitiesRendered.clear();

        ArrayList<Chunk> chunks = getAllChunksInRenderDistance(entity);

        for (Chunk chunk:chunks){
            semiStaticEntitiesRendered.addAll(chunk.getEntities());
        }

       
    }

    /**
     * the method updates the enities that is nearby the baseentity in the parameter.
     * the method should not be called too frequently for perfomance.
     * the amount of room the method search for is decided by the renderdistance.
     * OBS Overloaded function
     */

    public void updateSemiStaticEntitiesRendered(HitBox hitbox){
        //load all chunks that is involved in the range of the render distance.
        semiStaticEntitiesRendered.clear();

        ArrayList<Chunk> chunks = getAllChunksInRenderDistance(hitbox);

        for (Chunk chunk:chunks){
            semiStaticEntitiesRendered.addAll(chunk.getEntities());
        }

       // semiStaticEntitiesRendered = mother.loadEntitites(renderDistance,entity.getWorldX(),entity.getWorldX());
    }

    /**
     * returns all entities within render distance given the given entity.
     */
    public ArrayList<BaseEntity> getAllEntitiesInRenderDistance(BaseEntity entity){

        ArrayList<BaseEntity> AllEntitiesInRenderDistance = new ArrayList<>();

        for (Chunk chunk:getAllChunksInRenderDistance(entity)){
            AllEntitiesInRenderDistance.addAll(chunk.getEntities());
        }
        return AllEntitiesInRenderDistance;
    }



    /**
     * returns a list of all chunks within render distance
     * should be a part of the render system 
     * when a entity wants chunk -> check if loaded - > if not, load
     */

    public ArrayList<Chunk> getAllChunksInRenderDistance(BaseEntity entity){

        int centerX = entity.getWorldX()+entity.getWidth()/2;
        int centerY = entity.getWorldY()-entity.getHeight()/2;

        int minX = centerX-renderDistance;
        int minY = centerY-renderDistance;

        Rectangle renderRect = new Rectangle(minX,minY,renderDistance*2,renderDistance*2);
      
        ArrayList<Chunk> chunkList = new ArrayList<>();

        //calls recursive method, adds all chunks.
        mother.getAllChunks(renderRect,chunkList);

        return chunkList;
    }

    private ArrayList<Chunk> getAllChunksInRenderDistance(HitBox hitBox){

        int centerX = hitBox.getWorldX()+hitBox.width/2;
        int centerY = hitBox.getWorldY()-hitBox.height/2;

        int minX = centerX-renderDistance;
        int minY = centerY-renderDistance;

        Rectangle renderRect = new Rectangle(minX,minY,renderDistance*2,renderDistance*2);
      
        ArrayList<Chunk> chunkList = new ArrayList<>();

        //calls recursive method, adds all chunks.
        mother.getAllChunks(renderRect,chunkList);

        return chunkList;
    }

    /**
     * returns arraylist with all chunks in system.
     */
    private ArrayList<Chunk> getAllChunksInSystem(){
        ArrayList<Chunk> chunkList = new ArrayList<>();
        mother.getAllChunks(chunkList);
        return chunkList;
    }

    /**
     * 
     */
    


    public ArrayList<BaseEntity> getSemiStaticEntitiesRendered(){
      return semiStaticEntitiesRendered;
    }

    /**
     * works with all entities, tiles and whatnot.
     */
    public void addEntity(BaseEntity entity){
        try{
            mother.addEntity(entity);
        }
        catch(OutOfChunkBounds e){

        }
        
    }

    /**
     * sets entity at specified position. deletes all entities in the way. 
     * this does not seem to work in ant way?
     * 
     * a solutuion is that an intance of basentity ignores coords, and is placed by the chunksystem.
     */
    public void setEntity(BaseEntity newEntity,int row,int col){
        Rectangle rect = new Rectangle(row*panel.tileSize,col*panel.tileSize,panel.tileSize,panel.tileSize);
        getEntitiesInBound(rect);
    }


    /**
     * works.
     * can be done more effectively.. combining searching and removing.
     */
    public void removeEntitiesInBound(HitBox hb){
        for (BaseEntity  be:getEntitiesInBound(hb)){
            removeEntity(be);
        }
    }


    public boolean removeEntity(BaseEntity entity){
        return mother.removeEntity(entity);
    }

    /**
     * goes thought all treeNodes. checks if the chunks contains an entity at the specified coordinate.
     */
    public boolean isEntity(int row,int col){

        return getEntitiesInBound(new Rectangle(row*panel.tileSize,col*panel.tileSize,panel.tileSize,panel.tileSize)).size() > 0;
    }

    


    public boolean isEmpty(HitBox bounds){
        return (getEntitiesInBound(bounds).size()== 0);
    }
    

    
    /**
     * i have to figure out what kind of coords this is - worldX or row
     * this is moved down to chunk. 
     */
    

    
    private void isLoaded(int worldX,int worldY){
        //TODO
    }

    public void writeALlInfo(){
        for (Chunk chunk:getAllChunksInSystem()){
            chunk.writeInfo();
        }
        System.out.println("amount of entities: "+getAllEntities().size());
        System.out.println("amount of chunks:"+getAllChunksInSystem().size());
        
        System.out.println("bounds: "+getBounds());
    }

    public  ArrayList<BaseEntity> getAllEntities(){
         ArrayList<BaseEntity> allEntities = new ArrayList<>();
        for (Chunk chunk:getAllChunksInSystem()){
            allEntities.addAll (chunk.getAllEntities());
        }
        return allEntities;
    }

    private void generateTileInAllChunks(){
        for (Chunk chunk:getAllChunksInSystem()){
            chunk.load();
        }
    }

    private ArrayList<Integer> getBounds(){
        return mother.getBounds();
    }

    //came up with different idea.
    


    /**
     * some prototype theory written 1.des 2023
     */
    private void prototype(){

        //entity require update around itself.
        //system check player position, and tries to load all chunks within the player position

        //if a chunk i not loaded, the system should load the chunk. this is done thought procedural generation.
    }
}
