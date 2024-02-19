package ressurser.chunkSystem;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.Vector;
import ressurser.baseEntity.tile.Tile;

/** 
 * This class should work as the interface for chunkSystem. it has information about relevant entities,
 *  without having the complexity of the chunkSystem.
 * is not done. NOTE - many functions in chunkSystem should be changed or removed for cleaner and more readable code.
 */
public class WorkingMemory {

    ArrayList<Chunk> workingChunks = new ArrayList<>();
    public ArrayList<BaseEntity> workingEntities = new ArrayList<>();
    ArrayList<BaseEntity> sortedEntities = new ArrayList<>();
    ChunkSystem chunkSystem;
    public BaseEntity hoveredEntity = null;


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

    public ArrayList<BaseEntity> getTiles(){
        ArrayList<BaseEntity> notTiles = new ArrayList<>();
        for (BaseEntity entity:workingEntities){
            if ((entity instanceof Tile)){
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

        sortedEntities = getEntities();

        System.out.println("updating WM"+ p);
        //check if needed loading of new chunks
        chunkSystem.handleOutOfBounds(p);

        //update chunks:
        setWorkingChunks(chunkSystem.getAllChunksInRenderDistance(p));

        //load chunks that might not be loaded: - is dependent on the working memory chunks
        for (Chunk chunk:getChunks()){
            chunk.flush();
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
        
        System.out.println("end updating WM");
        //writeInfo();

        sort(sortedEntities);
    }

    /**
     * updates/simulates all actions of the entities
     */
    public void simulate(){
        
        int nonTile = 0;

        ArrayList<BaseEntity> simulatedEntites = new ArrayList<>(workingEntities);
        
        for (BaseEntity baseE:simulatedEntites){
            if (!(baseE instanceof Tile)){
                baseE.update();
                nonTile ++;
            }
        }

        chunkSystem.panel.camera.addbackendPrintData("simulated entities: "+String.valueOf(simulatedEntites.size()));
        chunkSystem.panel.camera.addbackendPrintData("non-tile entities: "+nonTile);
        chunkSystem.panel.camera.addbackendPrintData("chunks in working memory: "+String.valueOf(workingChunks.size()));
        chunkSystem.panel.camera.addbackendPrintData("render distance: "+chunkSystem.renderDistance);

        chunkSystem.panel.camera.addbackendPrintData("amount chunks loaded: "+String.valueOf(Chunk.amtLoaded));
        chunkSystem.panel.camera.addbackendPrintData("amount chunks generated: "+Chunk.amtGenerated);

        
        

    }

    /**
     * should only be called when it is time to animate the pieces.
     */
    public void animate(int value){
        for (BaseEntity visible:workingEntities){
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

        for (BaseEntity ent:sortedEntities){
            if (chunkSystem.panel.camera.collision(ent)){
                visible.add(ent);
            }
        }
        return visible;
    }

    public  ArrayList<BaseEntity> getVisibleTiles(){
        ArrayList<BaseEntity> visible = new ArrayList<>();

        for (BaseEntity ent:getTiles()){
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
   


    //COLLISION:::::::::

    public boolean solidCollision(HitBox hitbox){
        
        for (BaseEntity baseE:getEntitiesCollidedWith(hitbox)){
            if (baseE.isSolid()){

                //can implement cheks here for boats and others...
                return true;
            }
            //must check if given entity is solid, and wheter the entity that is calling collision has the requirments to move throught.
        }
        return false;
    }

    public ArrayList<BaseEntity> getEntitiesCollidedWith(HitBox hitBox){
        ArrayList<BaseEntity> collided = new ArrayList<>();
        for (BaseEntity baseE:workingEntities){

            //if there is collision between the hitboxes and they are not the same hitbox, the enotity is added to list.
            if (hitBox.collision(baseE.getHitBox()) && baseE.getHitBox() != hitBox){
                collided.add(baseE);
            }
        
        }
    return collided;
    }

    public ArrayList<BaseEntity> getEntitiesCollidedWith(Point p){
        ArrayList<BaseEntity> collided = new ArrayList<>();
        for (BaseEntity baseE:workingEntities){

            //if there is collision between the hitboxes and they are not the same hitbox, the enotity is added to list.

            if (baseE.getHitBox().collision(p)){
                collided.add(baseE);
            }
        
        }
    return collided;
    }

    ///END COLLISION::::::::

    public void setHoveredEntity(int x,int y){
        int worldX =  chunkSystem.panel.camera.getWorldX()+x;
        int worldY =  chunkSystem.panel.camera.getWorldY()+y;
        ArrayList<BaseEntity> entities = getEntitiesCollidedWith(new Point(worldX,worldY));
        chunkSystem.panel.camera.addbackendPrintData(worldX +","+worldY);
        for (BaseEntity baseE:entities){
            //TODO
            if (entities.size() == 1){
                hoveredEntity = baseE;
                return;
            } else{
                if (!(baseE instanceof Tile)){
                    hoveredEntity = baseE;
                }
            }
        }
    }

    public ArrayList<Vector> getPath(BaseEntity baseE,Point p){
        ArrayList<Vector> path = new ArrayList<>();
        path.add(new Vector(p.x-baseE.getHitBox().getWorldX(),p.y-baseE.getHitBox().getWorldY()));
        return path;
    }


     private static void swap(ArrayList<BaseEntity> list, int i, int j) {
        Collections.swap(list, i, j);
    }

    // Partition function to partition the arraylist and return the pivot index
    private static int partition(ArrayList<BaseEntity> list, int low, int high) {
        BaseEntity pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (list.get(j).getHitBox().getWorldY() < pivot.getHitBox().getWorldY()) {
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    // Quicksort function to recursively sort the arraylist
    private static void quicksort(ArrayList<BaseEntity> list, int low, int high) {
        if (low < high) {
            // Optimized for small arrays: switch to insertion sort if partition size is small
            if (high - low + 1 <= 10) {
                insertionSort(list, low, high);
            } else {
                int pivotIndex = partition(list, low, high);
                quicksort(list, low, pivotIndex - 1);
                quicksort(list, pivotIndex + 1, high);
            }
        }
    }

    // Insertion sort function for sorting small subarrays
    private static void insertionSort(ArrayList<BaseEntity> list, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            BaseEntity key = list.get(i);
            int j = i - 1;
            while (j >= low && (list.get(j).getHitBox().getWorldY() > key.getHitBox().getWorldY()) ) {

                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    // Public method to call the quicksort function with the entire arraylist
    public static void sort(ArrayList<BaseEntity> list) {
        quicksort(list, 0, list.size() - 1);
    }

}
