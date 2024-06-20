package ressurser.chunkSystem;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Entity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.Vector;
import ressurser.baseEntity.gameObject.GameObject;
import ressurser.baseEntity.playable.Inventory.Stack;
import ressurser.baseEntity.tile.Tile;
import ressurser.drawing.Camera;
import ressurser.main.GamePanel;
import ressurser.baseEntity.playable.Inventory.Item;
/** 
 * This class should work as the interface for chunkSystem. it has information about relevant entities,
 *  without having the complexity of the chunkSystem.
 * is not done. NOTE - many functions in chunkSystem should be changed or removed for cleaner and more readable code.
 */
public class WorkingMemory {
   
    ArrayList<Chunk> workingChunks = new ArrayList<>();
    public ArrayList<Entity> workingEntities = new ArrayList<>();
    private ArrayList<Tile> workingTiles = new ArrayList<>();
    private ArrayList<Entity> sortedVisibleEntities = new ArrayList<>();
    ArrayList<Chunk> sortedChunks = new ArrayList<>();

    
    ArrayList<BaseEntity> removalQueue = new ArrayList<>();
    ChunkSystem chunkSystem;
    ChunkSystem chunkSystemOverWorld;
    ChunkSystem chunkSystemCaves;
    ChunkSystem chunkSystemNether;
    private BaseEntity hoveredEntity = null;
    GamePanel panel;
    int type;
    static final int OVERWORLD = 0;
    static final int CAVE = 1;
    static final int NETHER = 2;

    
    
    public WorkingMemory(GamePanel panel){
        this.panel = panel;
        this.chunkSystem = new ChunkSystem(panel,8,type);
    }

    public void setType(int type){
        if (type == OVERWORLD){
            chunkSystem = chunkSystemOverWorld;
        } else if ((type == CAVE)){
            chunkSystem = chunkSystemCaves;
        } else {
            chunkSystem = chunkSystemNether;
        }

        //needs to reset workingentities, chunks etc
        //needs to make sure correct chunks are loaded
        //maybe need to genereate new area
        //
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
        
        //chunkSystem.unload(chunks,workingChunks);
        workingChunks.clear();
        workingChunks.addAll(chunks);
        
    }

    /**
     * clears all old entities
     * add all new entities
     */
    public void setWorkingEntities(ArrayList<Entity> entities){
        workingEntities.clear();
        workingEntities.addAll(entities);
    }

    /**
     * clears all old Tiles
     * add all new Tiles
     */
    public void setWorkingTiles(ArrayList<Tile> tiles){
        workingTiles.clear();
        workingTiles.addAll(tiles);
    }

    /**
     * filter out all Tile entities
     */
    public ArrayList<Entity> getEntities(){
        return workingEntities;
    }

    public ArrayList<Tile> getTiles(){
        return workingTiles;
    }

    public ArrayList<Entity> getBaseEntities(){
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
     * takes around 5-10 ms when exploring
     */
    public void update(Point p){
        long startTime = System.nanoTime();
        //1. check if needed loading of new chunks
        chunkSystem.handleOutOfBounds(p);
        
        //2. updates chunks (unloading, loading, flushing, connecting)
        updateChunks(p);

        //3. updates list of all entities, sorts sorted list
        updateEntities(p);

        updateTiles();
        long endTime = System.nanoTime();
        System.out.println("update time!: "+(endTime-startTime)/1000+"microseconds\n");
    }

    private void updateTiles() {
        setWorkingTiles(getTilesFromChunks());
    }

    /**unloades chunks, flushes out moveables, reloades chunk and connect tiles */
    private void updateChunks(Point p){
        
        long startTime = System.nanoTime();


        ArrayList<Chunk> oldChunks = new ArrayList<>(workingChunks);
        long start = System.nanoTime();
        //Compares new and old chunks - if chunks no longer is working, unload those chunks
        //time expensive, could be <100ms
        
        setWorkingChunks(chunkSystem.getAllChunksInRenderDistance(p));
        compareAndSelectUnloadedChunks(oldChunks,workingChunks);
        
        long end = System.nanoTime();
        //System.out.println("time taken by unloading chunks: "+(end-start)/1000+"microseconds\n");
        

        //flush all chunks and then loads chunks
        //not time expensive - <1ms
        long startFlushLoad = System.nanoTime();
        for (Chunk chunk:getChunks()){
            chunk.flush();
            chunk.load();
        }
        long endFlushLoad = System.nanoTime();
        //System.out.println("time taken by load/flush: "+(endFlushLoad-startFlushLoad)/1000+"microseconds\n");
        
        long startConnectingTime = System.nanoTime();
        //when loading new chunks, takes arounnd 1 - 5ms
        for (Chunk chunk:getChunks()){
            chunk.connectTiles();
        }
        long endConnectingTime = System.nanoTime();
        //System.out.println("time taken by connecting tiles: "+(endConnectingTime-startConnectingTime)/1000+"microseconds\n");

        setChunkUpdateTime(startTime);
    }


    /**3. updates list of all entities - all entities within render distance. Then sorts the visibleEntities based on new workingEntities*/
    private void updateEntities(Point p){
        ArrayList<Entity> allEntitiesInRenderDistance = new ArrayList<>();
        ArrayList<Chunk> chunksCopy = new ArrayList<>(workingChunks);
        for (Chunk chunk:chunksCopy){
            chunk.getEntitiesInBound(chunkSystem.getRenderRectangle(p),allEntitiesInRenderDistance);
        }

        setWorkingEntities(allEntitiesInRenderDistance);
        sortVisibleEntities();
    }

    /**compares the old workingchunks with the new workingchunks. if a chunk no longer is in the new Chunks, it is unloaded */
    private void compareAndSelectUnloadedChunks(ArrayList<Chunk> oldChunks, ArrayList<Chunk> newChunks) {
    
    // Iterate through oldChunks and check if each chunk is not in newChunksSet
    for (Chunk oldChunk : oldChunks) {
        if (!newChunks.contains(oldChunk)) {
            oldChunk.unLoad();
        }
    }
}


    /**
     * updates/simulates all actions of the entities
     */
    public void simulate(){
        clearRemovalQueue();
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
        for (Tile visibleTile:getTiles()){
            //System.out.println("fuck");
            if (visibleTile.animated){
                visibleTile.animate(value);
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

        for (BaseEntity ent:sortedVisibleEntities){
            if (chunkSystem.panel.camera.getHitBox().collision(new HitBox((ent.getWorldX()),ent.getWorldY(),ent.getWidth(),ent.getHeight()))){
                visible.add(ent);
            }
        }
        return visible;
    }

     /**takes all loaded baseEntities, and checks what entities that collides with a larger camera hitBox.
     * @return all entities that should be drawn
     */
    public  ArrayList<BaseEntity> getVisibleEntitiesenlarged(){
        ArrayList<BaseEntity> visible = new ArrayList<>();

        for (Entity ent:sortedVisibleEntities){
            if (chunkSystem.panel.camera.getHitBox().getEnlargedCameraHitbox().collision(new HitBox((ent.getWorldX()),ent.getWorldY(),ent.getWidth(),ent.getHeight()))){
                visible.add(ent);
            }
        }
        return visible;
    }

    public  ArrayList<Tile> getVisibleTiles(){
        ArrayList<Tile> visible = new ArrayList<>();

        for (Tile ent:getTiles()){
            if (chunkSystem.panel.camera.collision(ent)){
                visible.add(ent);
            }
        }
        return visible;
    }

    public  ArrayList<BaseEntity> getVisibleTilesEnlarged(){
        ArrayList<BaseEntity> visible = new ArrayList<>();

        for (BaseEntity ent:getTiles()){
            if (chunkSystem.panel.camera.enlargedCollision(ent)){
                visible.add(ent);
            }
        }
        return visible;
    }

    

    public Tile getTile(Point p){
        
        for (Chunk chunk:workingChunks){
            if (chunk.contains(p)){
                return chunk.getTile(p);
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

        for (Chunk chunk:workingChunks){
            if (chunk.collision(hitBox)){
                collided.addAll(chunk.getTilesCollidedWith(hitBox));
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

        for (Chunk chunk:workingChunks){
            if (chunk.collision(p)){
                collided.add(chunk.getTile(p));
            }
        }
    return collided;
    }

    ///END COLLISION::::::::

    public void setHoveredEntity(int x,int y){
        
        int worldX =  (int)chunkSystem.panel.camera.getWorldX()+x;
        
        int worldY =  (int)chunkSystem.panel.camera.getWorldY()+y;
        ArrayList<BaseEntity> entities = getEntitiesCollidedWith(new Point(worldX,worldY));
        chunkSystem.panel.camera.addbackendPrintData(worldX +","+worldY);
        for (BaseEntity baseE:entities){
            //TODO
            if (entities.size() == 1){
                hoveredEntity = baseE;
                return;
            } else{
                chunkSystem.panel.camera.addbackendPrintData(entities.get(0)+","+entities.get(1));
        
                if (!(baseE instanceof Tile)){
                    hoveredEntity = baseE;
                }
            }
        }
    }

    public ArrayList<Vector> getPath(Entity baseE,Point p){
        ArrayList<Vector> path = new ArrayList<>();
        path.add(new Vector(p.x-baseE.getHitBox().getWorldX(),p.y-baseE.getHitBox().getWorldY()));
        return path;
    }


     private static void swap(ArrayList<Entity> list, int i, int j) {
        Collections.swap(list, i, j);
    }

    private static void swapChunk(ArrayList<Chunk> list, int i, int j) {
        Collections.swap(list, i, j);
    }

    // Partition function to partition the arraylist and return the pivot index
    private static int partition(ArrayList<Entity> list, int low, int high) {
        Entity pivot = list.get(high);
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

    private static int partitionChunk(ArrayList<Chunk> list, int low, int high) {
        Chunk pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (list.get(j).getWorldY() < pivot.getWorldY()) {
                i++;
                swapChunk(list, i, j);
            }
        }
        swapChunk(list, i + 1, high);
        return i + 1;
    }

    // Quicksort function to recursively sort the arraylist
    private static void quicksort(ArrayList<Entity> list, int low, int high) {
        if (low < high) {
            // Optimized for small arrays: switch to insertion sort if partition size is small
            if (high - low + 1 <= 10) {
                insertionSort1(list, low, high);
            } else {
                int pivotIndex = partition(list, low, high);
                quicksort(list, low, pivotIndex - 1);
                quicksort(list, pivotIndex + 1, high);
            }
        }
    }

    // Insertion sort function for sorting small subarrays
    private static void insertionSort1(ArrayList<Entity> list, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            Entity key = list.get(i);
            int j = i - 1;
            while (j >= low && (list.get(j).getHitBox().getWorldY() > key.getHitBox().getWorldY()) ) {

                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    private static void quickChunksort(ArrayList<Chunk> list, int low, int high) {
        if (low < high) {
            // Optimized for small arrays: switch to insertion sort if partition size is small
            if (high - low + 1 <= 10) {
                insertionSortChunk(list, low, high);
            } else {
                int pivotIndex = partitionChunk(list, low, high);
                quickChunksort(list, low, pivotIndex - 1);
                quickChunksort(list, pivotIndex + 1, high);
            }
        }
    }

    private static void insertionSortChunk(ArrayList<Chunk> chunkList, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            Chunk key = chunkList.get(i);
            int j = i - 1;
            while (j >= low && (chunkList.get(j).getWorldY() > key.getWorldY()) ) {

                chunkList.set(j + 1, chunkList.get(j));
                j--;
            }
            chunkList.set(j + 1, key);
        }
    }

    

    /**takes all workingEntities, filter out entities that isnt close to screen, then sort */
    private void sortVisibleEntities() {
        long startTime = System.nanoTime();
        sortedVisibleEntities = getVisibleEntities(panel.camera,workingEntities);
        quicksort(sortedVisibleEntities, 0, sortedVisibleEntities.size() - 1);
        
        if (panel.camera != null){
            panel.camera.setObservedSortTime((long)(System.nanoTime()-startTime));
        }
        
    }

    private void chunkSort(ArrayList<Entity> list){
        
        sortedChunks = workingChunks;
        quickChunksort(sortedChunks,0,workingChunks.size()-1);
        
        sortedVisibleEntities = new ArrayList<>();
        for (Chunk chunk:sortedChunks){
            quicksort(chunk.entities, 0, chunk.entities.size() - 1);
            sortedVisibleEntities.addAll(chunk.entities);
        }

        
    }


    

    // Insertion sort function for sorting small subarrays based on BaseEntity's getWorldY() and getWorldX() methods
    private static void insertionSort(ArrayList<BaseEntity> list, int low, int high) {
        for (int i = low + 1; i <= high; i++) {
            BaseEntity key = list.get(i);
            int j = i - 1;
            // Compare based on worldY
            while (j >= low && (list.get(j).getWorldY() > key.getWorldY() ||
                    (list.get(j).getWorldY()+list.get(j).getHeight() == key.getWorldY()+key.getWorldY()+key.getHeight() && list.get(j).getWorldX()+list.get(j).getHeight() > key.getWorldX()+key.getHeight()))) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    // Public method to call the insertion sort function with the entire arraylist
    public static void insertionSort(ArrayList<BaseEntity> list) {
        insertionSort(list, 0, list.size() - 1);
    }

    /**firstly remove from sorted */
    public void addToRemovalQueue(BaseEntity ent) {
        removalQueue.add(ent);
    }

    /**should be called in beginning of every simulate
     * removes all entities in removalQueue from workingEntities and sortedEntities
    */
    public void clearRemovalQueue() {
        for (BaseEntity ent:removalQueue){
            sortedVisibleEntities.remove(ent);
            chunkSystem.removeEntity(ent);
        }
        removalQueue.clear();
    }

    public void placeEntity(BaseEntity ent){
        if (!solidCollision(ent.getHitBox()))
        chunkSystem.addEntity(ent);
    }

    public void removeEntity(BaseEntity ent){
        chunkSystem.removeEntity(ent);
    }

    public void tryPlaceEntity(Stack equipped) {
        if (equipped!= null && !(equipped.isEmpty())){

            BaseEntity ent = equipped.getItem(0);
            BaseEntity go = ((Item) ent).getPhysicalRepresentation();
            System.out.println("ent is: "+ent);
            System.out.println("go is: "+go);
            
            if (go instanceof GameObject){
                GameObject pr = (GameObject)go;

                pr.setWorldX(chunkSystem.panel.camera.getWorldX()+(int)chunkSystem.panel.mouse.getX()-(pr.getWidth()/2));
                pr.setWorldY(chunkSystem.panel.camera.getWorldY()+(int)chunkSystem.panel.mouse.getY()-(pr.getHeight()/2));

                if (!solidCollision(pr.getHitBox())){
                    placeEntity(pr);
                    equipped.removeOneItem();
                    System.out.println("placed entity");
                    System.out.println(pr);
                }  
            }
        }
    }

    /**returns all tiles that are going to be drawn */
    public ArrayList<BaseEntity> getVisibleTiles(Camera camera) {
        ArrayList<BaseEntity> visibleTiles = new ArrayList<>();

        //uses cams HB many times so stored upfront.
        HitBox camHB = camera.getHitBox();

        for (Chunk chunk:workingChunks){
            for (int i =0;i< chunk.tiles.length;i++){
                for (int j =0;j< chunk.tiles.length;j++){

                    //if Tile HB are inside Cameras hitBox, it is added to list of tiles in HB 
                    if (chunk.tiles[i][j] != null){//SHOULD NOT HAPPENsd
                        if (chunk.tiles[i][j].getHitBox().collision(camHB)){
                            visibleTiles.add(chunk.tiles[i][j]);
                        }
                    }
                    
                }
            }
        }
        return visibleTiles;
    }

    /**returns all tiles that are going to be drawn */
    public ArrayList<Tile> getTilesFromChunks() {
        ArrayList<Tile> tiles = new ArrayList<>();

        for (Chunk chunk:workingChunks){
            for (int i =0;i< chunk.tiles.length;i++){
                for (int j =0;j< chunk.tiles.length;j++){
                    tiles.add(chunk.tiles[i][j]);
                }
            }
        }
        return tiles;
    }

    

    /**returns all entities that are going to be drawn */
    public ArrayList<Entity> getVisibleEntities(Camera camera) {
        return getVisibleEntities(camera,sortedVisibleEntities);
    }

    private ArrayList<Entity> getVisibleEntities(Camera camera,ArrayList<Entity> entities){
        ArrayList<Entity> visibleEntities = new ArrayList<>();

        
        if (camera== null){return visibleEntities;}
        //uses cams HB many times so stored upfront.
        HitBox camHB = camera.getImageHitbox();

        for (Entity entity:entities){
            
            //if entitys image are inside Cameras hitBox, it is added to list of entities inHB 
            if (entity.getImageHitbox().collision(camHB)|| entity.getHitBox().collision(camHB)){
                visibleEntities.add(entity);
            }
        }
        return visibleEntities;
    }

    private ArrayList<BaseEntity> getEntitiesNearCamera(Camera camera,ArrayList<Entity> entities){
        ArrayList<BaseEntity> visibleEntities = new ArrayList<>();

        //uses cams HB many times so stored upfront.
        HitBox camHB = camera.getImageHitbox().getEnlargedCameraHitbox();

        for (BaseEntity entity:entities){
            
            //if entitys image are inside Cameras hitBox, it is added to list of entities inHB 
            if (entity.getImageHitbox().collision(camHB)|| entity.getHitBox().collision(camHB)){
                visibleEntities.add(entity);
            }
        }
        return visibleEntities;
    }


    public BaseEntity getHoveredEntity() {
        return hoveredEntity;
    }
    public void setHoveredEntity(BaseEntity hoveredEntity){
        this.hoveredEntity = hoveredEntity;
    }

    private void setChunkUpdateTime(long startTime){
        long endTime = System.nanoTime();

        if (panel.camera!= null){
            panel.camera.setObservedChunkUpdateTime(endTime-startTime);
        }
    }
}
