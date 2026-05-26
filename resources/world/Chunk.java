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
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;

import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;
import resources.domain.player.Moveable;
import resources.domain.tile.CliffTile;
import resources.domain.tile.Tile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;


public class Chunk extends TreeNode{
    static int amount = 0;
   
    ArrayList<Entity> entities = new ArrayList<>();
    Tile[][] tiles = new Tile[CHUNKSIZE][CHUNKSIZE];
    // is the chunk loaded. When the game is started the chunk will not be loaded. When chunk is rendered/loaded, boolean value is set true. 
    // this boolean needs to be stored in harddrive. If already loaded, do not need procedural generation of entites, because these is already loaded.
   
    boolean loaded = false;
    static int amtLoaded = 0;
    static int amtGenerated = 0;
    boolean sorted = false;

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

    

    public ArrayList<Entity> getEntities(){
        return entities;
    }

    private ArrayList<BaseEntity> getEntitiesWithoutTiles(){
        ArrayList<BaseEntity> entitiesFiltered = new ArrayList<>();
        for (BaseEntity be:entities){
            if (!(be instanceof Tile)){
                entitiesFiltered.add(be);
            }
        }
        return entitiesFiltered;
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
    public ArrayList<Entity> getEntitiesInBound (Rectangle rect,ArrayList<Entity> arrayList){
        for (Entity entity:entities){
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
     * adds baseEntity to entity pile/tiles
     */
    @Override
    public void addEntity(BaseEntity entity){
        if (entity== null){throw new NullPointerException("tried adding null");}
        if (entity!= null){
            if (entity instanceof Tile){
                addTile((Tile)entity);
            } else {
                entities.add((Entity)entity);
            }
        }
    }

    public void addTile(Tile entity){
        if (entity!= null){
            //convert coords to placement in tile array
            int arrayX = (int)(entity.getWorldX()-x)/64;
            int arrayY = (int)(entity.getWorldY()-y)/64;

            tiles[arrayY][arrayX] =entity;
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

    protected ArrayList<Entity> getAllEntities(){
        return entities;
    }   

    /**
     * returns first Tile that hitbox contains point.
     * 
     */
    protected Tile getTile(Point point){

        for (int i = 0;i<tiles.length;i++){
            for (int j =0;j<tiles[i].length;j++){
                if (tiles[i][j]!= null){
                    if (tiles[i][j].getHitBox().contains( point)){
                        return tiles[i][j];
                    }
                }
            }
        }
        System.out.println(point);
        return null;
    }

    /**
     * get first entity that contains point
     */
    protected Entity getEntity(Point point){

        for (Entity entity:getEntities()){
            if (entity.getHitBox().contains( point)){
            return entity;
            }
        }
        return null;
    }
    /**adds entities that contains point in list */
    public ArrayList<BaseEntity> getEntitiesatPoint(Point point, ArrayList<BaseEntity> entitiesList){
        
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
       //writeEntitiesToFile(getEntitiesWithoutTiles());
    }

    private void repeatedLoad(){
        generateTiles();
    
        //could read from file here, but not implemented, should mby be in chunSystem or implemented in another fashion.
        generateEntities();
        //read entities from file -NOT DONE
    }



    /*generate tiles, should be used every time a chunk is loaded */
    private void generateTiles(){
        addTilesToChunk();
        //connectEntities();
    }
    /**generates entities, should only be ran first time in the chunks */
    private void generateEntities(){
        addEntitiesToChunk();
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
            //if not generated before (first time load)
            if (!generated&chunkS.generate){
                initialLoad();
            
            }
            //if already loaded
            else{
                repeatedLoad();
            }
        
            loaded = true;
            amtLoaded++;
            
        }
        
    }

    

   
    private Tile getSingelTile(int worldX,int worldY){
        return chunkS.entityFactory.getTile(worldX,worldY);
    }

   

    private BaseEntity getSingelEntity(int worldX,int worldY){

        return chunkS.entityFactory.getEntity(worldX,worldY);
    }

    /**needs to be changed!! has two .. */
    private void addEntitiesToChunk(){
        long startTime = System.currentTimeMillis();

        for (int x2 = 0;x2<width;x2+= chunkS.panel.tileSize){
            for (int y2 = 0;y2<width;y2+=chunkS.panel.tileSize){
                
                

                BaseEntity ent = getSingelEntity(x+x2,y+y2);
                if (ent!= null){
                    addEntity(ent);
                }
                
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("chunk generation time" + (endTime-startTime));  
    }

    private void addTilesToChunk(){
       
        for (int x2 = 0;x2<width;x2+= chunkS.panel.tileSize){
            for (int y2 = 0;y2<width;y2+=chunkS.panel.tileSize){
                
                addEntity(getSingelTile(x+x2,y+y2));
                //tiles[x2/64][y2/64] = getSingelTile(x+x2,y+y2);
               
            }
        }
    }

    /**iterates throught all tiles in chunk, checks if needed checking of neigbors 
     * change this to go back to original.
    */
    void connectTiles(){
       
        for (int i = 0;i<tiles.length;i++){
            for (int j =0;j<tiles[i].length;j++){
                if (tiles[i][j] != null){
                    tiles[i][j].setNeighBors();
                }
            }
        }
    }

    /**checks if any entities has moved out of the specified chunk, removes them if they have, and adds them to correct chunk. */
    void flush(){

        ArrayList<BaseEntity> toBeRemoved = new ArrayList<>();
        for (BaseEntity baseE:entities){
            //should be changed to moveable
            if (baseE instanceof Moveable){
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


    /**not yet implemented... needs database for reading/writing */
    public void unLoad(){
        //loaded = false;
        //if i want to write to file, do this at certain times with long intervalls - it does cost time
        //overwritetoFile(getEntitiesWithoutTiles());
        //entities.clear();
    }
    

    
    //FILE TECH

    public String serializeEntity(BaseEntity entity) {
        // Example serialization. Customize based on your BaseEntity fields
        return entity.getName() + "," + entity.getWorldX() + "," + entity.getWorldY() + "," + entity.getClass();
    }
    
    public String serializeEntities(ArrayList<BaseEntity> entities) {
        StringBuilder sb = new StringBuilder();
        sb.append( "chunk:"+getWorldX()+","+getWorldY()+"\n");
        for (BaseEntity entity : entities) {
            sb.append(serializeEntity(entity)).append("\n");
        }
        sb.append("end\n\n\n");
        return sb.toString();
    }

    

    public void writeEntitiesToFile( ArrayList<BaseEntity> entitiesUnfiltered) {

        
        File outputFile = new File("storage.txt");

        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            // Move the file pointer to the end of the file to append
            raf.seek(raf.length());
            // Write the serialized entities to the file
            raf.writeBytes(serializeEntities(entitiesUnfiltered));
           
            
            

        } catch (IOException e) {
            e.printStackTrace();
            //System.out.println("funker ikke");
        }
    }

    /**
     * Appends text to a file.
     * 
     * @param text the text to be appended
     * @param outputFile the file to which the text should be appended
     * @throws IOException if an error occurs during writing
     */
    public static void appendTextToFile(String text, File outputFile) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            // Move the file pointer to the end of the file
            raf.seek(raf.length());
            // Write the text to the file
            raf.writeBytes(text);
        }
    }


    public void overwritetoFile(ArrayList<BaseEntity> entities) {
        StringBuilder search = new StringBuilder();
        search.append( "chunk:"+getWorldX()+","+getWorldY());

        StringBuilder newText = new StringBuilder();
        newText.append( "chunk:"+getWorldX()+","+getWorldY()+"\n");
        newText.append("endret text:)");
        for (BaseEntity entity : entities) {
            newText.append(serializeEntity(entity)).append("\n");
        }
        
        try {
            alterTextBetweenMarkers(search.toString(),"end",newText.toString(),new File("storage.txt"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void alterTextBetweenMarkers(String searchString, String endMarker, String newText, File outputFile) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            // Variables to keep track of positions and reading
            long fileLength = raf.length();
            long startPointer = 0;
            String line;

            // Iterate through the file line by line to find the search string
            while (raf.getFilePointer() < fileLength) {
                line = raf.readLine();
                if (line != null && line.equals((searchString))) {
                    startPointer = raf.getFilePointer();
                    break;
                }
            }

            if (startPointer == 0) {
                //System.out.println("Search string not found."+searchString+"!");
                return;
            } else {//System.out.println(searchString);}
            }
            // Move the pointer back to the start of the found line
            raf.seek(startPointer - searchString.length() - 1);

            // Read the rest of the file into a buffer
            StringBuilder buffer = new StringBuilder();
            while (raf.getFilePointer() < fileLength) {
                line = raf.readLine();
                if (line != null && line.contains(endMarker)) {
                    break;
                }
                buffer.append(line).append(System.lineSeparator());
            }

            // Replace the content in the buffer with the new text
            String contentToReplace = buffer.toString();
            String modifiedContent = contentToReplace.replace(contentToReplace, newText);

            // Move the file pointer back to the start of the found string
            raf.seek(startPointer - searchString.length() - 1);

            // Write the modified content back to the file
            raf.writeBytes(modifiedContent);
        }
    }
    
    public BaseEntity deserializeEntity(String entityString) {
        String[] parts = entityString.split(",");
        // Adjust based on your BaseEntity constructor and fields
        String name = parts[0];
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        String type = parts[3];
        //return new BaseEntity(name, x, y, type); // Adjust constructor
        return null;
    }

    public ArrayList<BaseEntity> readEntitiesFromFile(String fileName) {
        ArrayList<BaseEntity> entities = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                entities.add(deserializeEntity(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entities;
    }

    public ArrayList<Tile> getTilesCollidedWith(HitBox hitBox) {
        ArrayList<Tile> tilesCollidedWith = new ArrayList<Tile>();
        for (int i = 0;i<tiles.length;i++){
            for (int j =0;j<tiles[i].length;j++){
                if (tiles[i][j]!= null){
                    if (tiles[i][j].collision(hitBox)){
                
                        tilesCollidedWith.add(tiles[i][j]);
                    }
                }
            }
        }
        return tilesCollidedWith;
    }

    /**iterates thgough Tiles, checks if any contains point.return if one matches. */
    public Tile getTileInPoint(Point p){
        if (!loaded){
            load();
        }
        for (int i = 0;i<tiles.length;i++){
            for (int j =0;j<tiles[i].length;j++){
                if (tiles[i][j]!= null){
                    if (tiles[i][j].getHitBox().contains(p)){
                
                        return (tiles[i][j]);
    
                    }
                }
            }
        }
        return null;
    }


    //SORTING
    void sort(){
        if (!sorted){
            sorted = true;
            quicksort(entities,0,entities.size()-1);
        } else{
            insertionSort1(entities, 0, entities.size()-1);
        }

        
    }

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

    private static void swap(ArrayList<Entity> list, int i, int j) {
        Collections.swap(list, i, j);
    }
}
