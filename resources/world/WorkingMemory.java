package resources.world;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.tile.Tile;
import resources.domain.tile.CliffTile;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.generation.factory.EntityFactory;
import resources.domain.object.GameObject;
import resources.domain.player.Moveable;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;
import resources.presentation.camera.Camera;

import java.awt.Point;
import java.util.ArrayList;

import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.domain.object.GameObject;
import resources.domain.inventory.Stack;
import resources.domain.tile.Tile;
import resources.generation.factory.EntityFactory;
import resources.presentation.camera.Camera;
import resources.app.GamePanel;
import resources.generation.noise.ProceduralGen;
import resources.domain.inventory.Item;
/** 
 * This class should work as the interface for chunkSystem. it has information about relevant entities,
 *  without having the complexity of the chunkSystem.
 * is not done. NOTE - many functions in chunkSystem should be changed or removed for cleaner and more readable code.
 */
public class WorkingMemory implements WorldRuntime {
   
    ArrayList<Chunk> workingChunks = new ArrayList<>();
    public ArrayList<Entity> workingEntities = new ArrayList<>();
    private ArrayList<Tile> workingTiles = new ArrayList<>();
    private ArrayList<Entity> sortedVisibleEntities = new ArrayList<>();
    ArrayList<BaseEntity> removalQueue = new ArrayList<>();
    ChunkSystem chunkSystem;
    ChunkSystem chunkSystemOverWorld;
    ChunkSystem chunkSystemCaves;
    ChunkSystem chunkSystemNether;
    private BaseEntity hoveredEntity = null;
    GamePanel panel;
    
    static final int OVERWORLD = 0;
    static final int CAVE = 1;
    static final int NETHER = 2;

    public WorkingMemory(GamePanel panel){
        this.panel = panel;
        ProceduralGen overworldGen = new ProceduralGen();
        this.chunkSystemOverWorld = new ChunkSystem(panel,8,ChunkSystem.OVERWORLD,new EntityFactory(panel, overworldGen));
        this.chunkSystem = chunkSystemOverWorld;
    }

    

    /**starts the system up. connect tiles together after all tiles are made. */
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
            
            if (visibleTile!= null && visibleTile.animated){
                visibleTile.animate(value);
            }
        }
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
            if (chunkSystem.panel.camera.visibilityArea.collision(new HitBox((ent.getWorldX()),ent.getWorldY(),ent.getWidth(),ent.getHeight()))){
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
        chunkSystem.panel.camera.addbackendPrintData("cameras pos: "+worldX +","+worldY);
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

    public ArrayList<Vector> getPath(Entity baseE,Point p){
        ArrayList<Vector> path = new ArrayList<>();
        path.add(new Vector(p.x-baseE.getHitBox().getWorldX(),p.y-baseE.getHitBox().getWorldY()));
        return path;
    }



    /**takes all workingEntities, filter out entities that isnt close to screen, then sort */
    private void sortVisibleEntities() {
        long startTime = System.nanoTime();
        if (panel.camera != null){
            //TODO this is bad code
            ArrayList<Entity> allEntitiesInRenderDistance = new ArrayList<>();
            ArrayList<Chunk> chunksCopy = new ArrayList<>(workingChunks);
            for (Chunk chunk:chunksCopy){
                chunk.getEntitiesInBound(chunkSystem.getRenderRectangle(panel.camera.getHitBox().getCenter()),allEntitiesInRenderDistance);
            }
    
            setWorkingEntities(allEntitiesInRenderDistance);
        }
        sortedVisibleEntities = getVisibleEntities(panel.camera,workingEntities);
        EntitySorter.sortByWorldY(sortedVisibleEntities);
        long now = System.nanoTime();
        if (panel.camera != null){
            panel.camera.setObservedSortTime((now-startTime));
        }
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

    public boolean placeEntity(BaseEntity ent){
        if (!solidCollision(ent.getHitBox())){
            chunkSystem.addEntity(ent);
            return true;
        }
        return false;
    }

    public void removeEntity(BaseEntity ent){
        if (ent== null){return;}
        chunkSystem.removeEntity(ent);
    }

    public boolean tryPlaceEntity(Stack equipped) {
        
        System.out.println("try place entity"+equipped);
        if (equipped!= null && !(equipped.isEmpty())){

            BaseEntity item = equipped.getItem();
            if (item== null){ System.out.println("item is: "+item);return false;}
            BaseEntity gameObject = ((Item) item).getPhysicalRepresentation();
            System.out.println("item is: "+item);
            System.out.println("go is: "+gameObject);
            
            if (gameObject instanceof GameObject){
                GameObject pr = (GameObject)gameObject;

                pr.setWorldX(panel.mouse.getMouseWorldX()-(pr.getWidth()/2));
                pr.setWorldY(panel.mouse.getMouseWorldY()-(pr.getHeight()/2));

                if (!solidCollision(pr.getHitBox())){
                    placeEntity(pr);
                    equipped.removeOneItem();
                    return true;
                }  
            }
        }{System.out.println("not equipped");}
        return false;
    }

    public void addObjectPreview(Stack equipped){

        if (equipped.getItem()== null){return;}
            GameObject gameObject = (GameObject) equipped.getItem().getPhysicalRepresentation();
            if (gameObject != null){
                GameObject preview = gameObject.getPreviewObject(panel);
                panel.camera.setPreviewObject(preview);
                placeEntity(preview);
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
                
            }
            visibleEntities.add(entity);
        }
        return visibleEntities;
    }

    private ArrayList<Chunk> getVisibleChunks(Camera camera){
        ArrayList<Chunk> visibleChunks = new ArrayList<>();

        
        if (camera== null){return visibleChunks;}
        //uses cams HB many times so stored upfront.
        HitBox camHB = camera.getImageHitbox();

        for (Chunk chunk:workingChunks){
            
            //if chunk are inside Cameras hitBox, it is added to list of entities inHB 
            if (chunk.collision(camHB)){
                visibleChunks.add(chunk);
            }
        }
        return visibleChunks;
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
