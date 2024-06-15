package ressurser.chunkSystem;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.playable.Moveable;
import ressurser.baseEntity.tile.CliffTile;
import ressurser.baseEntity.tile.Tile;
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
       writeEntitiesToFile(getEntitiesWithoutTiles());
    }

    private void repeatedLoad(){
        generateTiles();
        //read entities from file
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
            
            if (!generated&chunkS.generate){
                initialLoad();
                generateTiles();
            
            }
            else{
               
                repeatedLoad();
            }
            
        }
        loaded = true;
        amtLoaded++;
    }

    public void deload(){
        loaded = false;
        //when a chunk no longer is in the working memory, it needs to deload - write down the enitites to file...
    }

    /**should not be here.....!! //TODO */
    private Tile getSingelTile(int worldX,int worldY){
        
        //FIXTHIS!
        //loader algorithm..
        //method returns biome type- which is a streubg
        String biomeType =  chunkS.proceduralGen.calculateBiomeString(worldX, worldY);
        int height = (int)(chunkS.proceduralGen.getHeightValue(worldX,worldY)*1000);

        //if (height>100){return new Tile(chunkS.panel,biomeType,worldX,worldY,height, false);}

        if(biomeType.equals("grass")){
            double value = (Math.abs(chunkS.proceduralGen.getVegetationMidFreq(worldX,worldY)));
            double intervals = 6/1;
            
            int number = (int)(value*intervals);
            number ++;
            
            if (number>7){number = 7;}

            String streng = "grass"+number;
            if (number <2){streng = "grass";}

            return new Tile(chunkS.panel,streng,worldX,worldY,height);
        }
        return new Tile(chunkS.panel,biomeType,worldX,worldY,height);
    }

   

    private BaseEntity getSingelEntity(int worldX,int worldY){

        return chunkS.entityFactory.getEntity(worldX,worldY);
    }

    /**needs to be changed!! has two .. */
    private void addEntitiesToChunk(){
       
        for (int x2 = 0;x2<width;x2+= chunkS.panel.tileSize){
            for (int y2 = 0;y2<width;y2+=chunkS.panel.tileSize){
                
                addEntity(getSingelTile(x+x2,y+y2));

                addEntity(getSingelEntity(x+x2,y+y2));
            }
        }
    }

    /**iterates throught all tiles in chunk, checks if needed checking of neigbors 
     * change this to go back to original.
    */
    void connectTiles(){
       
        for (BaseEntity baseEntity:entities){
            if (baseEntity instanceof Tile ||baseEntity instanceof CliffTile){
                
                
                if (baseEntity instanceof CliffTile){
                    if (((CliffTile)baseEntity).hasCompleteNeigbors() ||true){
                        ((CliffTile)baseEntity).setNeighBors();
                    }
                } else{

                    if (!((Tile)baseEntity).hasCompleteNeigbors()){
                        ((Tile)baseEntity).setNeighBors();
                        
                    }
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
        loaded = false;
        overwritetoFile(getEntitiesWithoutTiles());
        entities.clear();
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
}
