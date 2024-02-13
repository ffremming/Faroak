package ressurser.chunkSystem;

import java.awt.Point;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.tile.Tile;

/** 
 * This class should work as the interface for chunkSystem. it has information about relevant entities,
 *  without having the complexity of the chunkSystem.
 * is not done. NOTE - many functions in chunkSystem should be changed or removed for cleaner and more readable code.
 */
public class WorkingMemory {

    ArrayList<Chunk> workingChunks = new ArrayList<>();
    ArrayList<BaseEntity> workingEntities = new ArrayList<>();
    ChunkSystem chunkSystem;



    public WorkingMemory(ChunkSystem chunkSystem){
        this.chunkSystem = chunkSystem;
        
    }

    /**starts the system up. connect tiles toghetet after all tiles are made. */
    public void initial(){
        setWorkingChunks(chunkSystem.getAllChunksInRenderDistance(new Point(0,0)));

        for (Chunk chunk:getChunks()){
            chunk.load();
        }
        for (Chunk chunk:getChunks()){
            chunk.connectTiles();
        }
    }

    /**
     * clears all chunks
     * add all new chunks
     */
    public void setWorkingChunks(ArrayList<Chunk> chunks){
        workingChunks.clear();
        workingChunks.addAll(chunks);
    }

    /**
     * clears all old entities
     * add all new entities
     */
    public void setWorkingEntities(ArrayList<BaseEntity> entities){
        workingEntities.clear();
        workingEntities.addAll(entities);
    }

    /**
     * filter out all Tile entities
     */
    public ArrayList<BaseEntity> getEntities(){
        ArrayList<BaseEntity> notTiles = new ArrayList<>();
        for (BaseEntity entity:workingEntities){
            if (!(entity instanceof Tile)){
                notTiles.add(entity);
            }
        }
        return notTiles;
    }

    public ArrayList<BaseEntity> getBaseEntities(){
        return workingEntities;
    }

    public ArrayList<Chunk> getChunks(){
        return workingChunks;
    }

    /**
     * 
     * set working entities and chunks
     * adds storage capacity if needed
     * loads unloaded chunks
     */
    public void update(Point p){

        
        //check if needed loading of new chunks
        chunkSystem.handleOutOfBounds(p);

        //update chunks:
        setWorkingChunks(chunkSystem.getAllChunksInRenderDistance(p));

        //load chunks that might not be loaded: - is dependent on the working memory chunks
        //chunkSystem.loadChunks();

        //TODO kkeps getting exceptiosn
        for (Chunk chunk:getChunks()){
            chunk.load();
        }
        for (Chunk chunk:getChunks()){
            chunk.connectTiles();
        }
        

        //sets working entities - all entities within render distance.
        ArrayList<BaseEntity> AllEntitiesInRenderDistance = new ArrayList<>();
        ArrayList<Chunk> chunksCopy = new ArrayList<>(workingChunks);
        for (Chunk chunk:chunksCopy){
            chunk.getEntitiesInBound(chunkSystem.getRenderRectangle(p),AllEntitiesInRenderDistance);
        }
        setWorkingEntities(AllEntitiesInRenderDistance);
        
        
        //writeInfo();
    }

    /**
     * should only be called when it is time to animate the pieces.
     */
    public void animate(int value){
        for (BaseEntity visible:getvisibleEntities()){
            if (visible.animated){
                visible.animate(value);
                
                //TODO
            }
        }
    }

    //** test if objects are the same */
    private void testIfObjectsAreTheSame(){
        int amount = 0;
        for (BaseEntity ent:getBaseEntities()){
            for (BaseEntity ent2:getBaseEntities()){
                if (ent ==(ent2)){
                    amount ++;
                }
        }
        }
        System.out.println("amount is:"+amount+" ,but amount should be: "+getBaseEntities().size());
    }


    public void writeInfo(){

        for (Chunk c:workingChunks){
           c.writeInfo();
        }

        System.out.println("amount of chunks: "+workingChunks.size());
        System.out.println("amount of Entities: "+workingEntities.size());
    }

    /**takes all loaded baseEntities, and checks what entities that collides with camera hitBox.
     * @return all entities that should be drawn
     */
    public  ArrayList<BaseEntity> getvisibleEntities(){
        ArrayList<BaseEntity> visible = new ArrayList<>();
        for (BaseEntity ent:getBaseEntities()){
            if (chunkSystem.panel.camera.collision(ent)){
                visible.add(ent);
            }
        }
        return visible;
    }

    public Tile getTile(Point p){
        //try to get tile from active memory
        //can figure out how to do this faster, can separate tiles and other entities..
        for (BaseEntity ent:getBaseEntities()){
            if (ent instanceof Tile){
                if (ent.getHitBox().contains(p)){
                    //return (Tile)ent;
                }
            }
        }
        //try to get tile from chunkSystem
        return chunkSystem.getTile(p);
    }





}
