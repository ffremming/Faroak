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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.swing.JFrame;


import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;

import resources.domain.tile.Tile;
import resources.generation.factory.EntityFactory;

import resources.app.GamePanel;




public class ChunkSystem {

    //file reading prototype:
    
    GamePanel panel;
    public WorkingMemory workingMemory;
    //renderdistance is the distance from the player to the border of where entities is rendered.
    int renderDistance ;
    TreeNode parent;
    String ID;
    
    EntityFactory entityFactory;
    
    //
    //the size of treeNode is defined by the SIZEPOW - which is the value x that results in 2^x, which is the width of the entire chunkSystem(in the beginning.)
        
    final int SIZEPOW = 8;// 5072 chunks.
    protected boolean generate = true;
    //the size 1 results in 16 chunks
    //the size 2 results in 64 chunks
    //etc 256 ...

    HashMap <String,Tile> tileHashMap = new HashMap<String,Tile>();

    int type;
    final static int OVERWORLD = 1;
    final int CAVE = 2;
    final int NETHER = 3;

    
    
    

    private void testExpandingSystem(int xOff,int yOff){
        
        

        for (int i = 0;i<10;i++){
            int width = parent.width;
            int height = parent.height;

            int x = parent.x;
            int y = parent.y;
            handleOutOfBounds(new Point(x+xOff*width,yOff*height));


            assert parent.getSquareMeter() == width*height *4: "squareMeter";
            assert parent.x !=x: "x==x";
            assert parent.y !=y: "y==y";
           

        }

        
    }

    /**
     * the chunksystem starts by creating the parent treenode, that has an set size. this treenode will create childnodes til the children are small enought to be consideed a chunk.
     * 
     * if the chunksystem is not big enought, it should create another parent.
     */
    public ChunkSystem(GamePanel panel,int SizePow,int type,EntityFactory entFact){

        this.type = type;
        this.panel = panel;

        setID();
        
        

        //needs cleanup
        try {
            clearFile(new File("storage.txt"));
            System.out.println("File cleared successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        entityFactory = entFact;
        //this should be lower. but not sure yet.
        renderDistance = 20*panel.tileSize;

        parent = new TreeNode(this,-(int)Math.pow(2,SizePow)*panel.tileSize*8,-(int)Math.pow(2,SizePow)*panel.tileSize*8,(int)Math.pow(2,SizePow)*panel.tileSize*16,(int)Math.pow(2,SizePow)*panel.tileSize*16);
       
    }

    

   

    private void setUpTest(){
        
        //removeEntitiesInBound()
        generateTileInAllChunks();
        writeALlInfo();
    }

    

    /**
     * returns all entities in bound. in bound mean that the hitbox collides.
     * search from all entities, not just nearby(still fast tho). return all entities that hitboxes collide.
     * 
     * @return list of all entities that collides with hitbox
     */
    
     public ArrayList<Entity> getEntitiesInBound(Rectangle rect){
        ArrayList<Entity> entitiesInBound = new ArrayList<>();
        return parent.getEntitiesInBound(rect,entitiesInBound);
    }


    /**
     * return list of entities at the specified point.
     * @return all kinds of entities
     */
    public ArrayList<BaseEntity> getEntities(int worldX,int worldY){
        ArrayList<BaseEntity> entitiesInPoint = new ArrayList<BaseEntity>();
        return parent.getEntitiesInPoint(new Point(worldX,worldY),entitiesInPoint);
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

        int centerX = ((int)entity.getWorldX()+entity.getWidth()/2);
        int centerY = (int)entity.getWorldY()-entity.getHeight()/2;
        return getAllChunksInRenderDistancefromPoint(new Point(centerX,centerY));
    }

    private ArrayList<Chunk> getAllChunksInRenderDistance(HitBox hitBox){

        int centerX = hitBox.getWorldX()+hitBox.width/2;
        int centerY = hitBox.getWorldY()-hitBox.height/2;
        return getAllChunksInRenderDistancefromPoint(new Point(centerX,centerY));
    }

    ArrayList<Chunk> getAllChunksInRenderDistance(Point p){
        return getAllChunksInRenderDistancefromPoint(p);
    }

    /**@return the rectangle that is represented based on point middle and radius */
    private Rectangle getRectangle(Point middle,int radius){
        int minX = middle.x-radius;
        int minY = middle.y-radius;

        Rectangle renderRect = new Rectangle(minX,minY,radius*2,radius*2);
        return renderRect;
    }
    /**
     * @return complete rectangle that represent the area that is rendered
     */
    Rectangle getRenderRectangle(Point p){
       return getRectangle(p,renderDistance);
    }

    /**returns all entities in render distance, takes in point */
    private ArrayList<Chunk> getAllChunksInRenderDistancefromPoint(Point p){
        //adjust for renderdistance
        
        ArrayList<Chunk> chunkList = new ArrayList<>();

        //calls recursive method, adds all chunks.
        parent.getAllChunks(getRenderRectangle(p),chunkList);
        return chunkList;
    }


    /**
     * returns arraylist with all chunks in system.
     */
    private ArrayList<Chunk> getAllChunksInSystem(){
        ArrayList<Chunk> chunkList = new ArrayList<>();
        parent.getAllChunks(chunkList);
        return chunkList;
    }

    


    

    /**
     * works with all entities, tiles and whatnot.
     */
    public void addEntity(BaseEntity entity){
        try{
            parent.addEntity(entity);
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
    public void setEntity(BaseEntity newEntity){
        //TODO
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
        return parent.removeEntity(entity);
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

    private Rectangle getBounds(){
        return parent.getBounds();
    }

   
    


    


    /**
     * i want to make a function that loades the chunks in the render area around the point p
     * i dont think this needs to be called too often. Depends on the range of renderdistance.
     */
    public void handleOutOfBounds (Point p){
       
        Rectangle renderRect = getRenderRectangle(p);
        
        //check if out of bounds
        if (OutOfBounds(renderRect)){
            try {
                expand(renderRect);
                System.out.println("expand system");
            } catch (OutOfChunkBounds e) {
                //should never happen. 
                e.printStackTrace();
            }
        } 


        //could be smart to load semistatic entities here //TODO
    }

    /**not for updating chunks, but loading the content on to it. does nothing if chunks already are loaded.
     * should be called ONLY after working memory has updated chunks to load
     */
    void loadChunks(){
        for (Chunk chunk:workingMemory.getChunks()){
            chunk.load();
        }
    }

    /**check if given parameter is containted by parent
     * @return true - if given parameter is outside bounds of system
     */
    private boolean OutOfBounds(Rectangle rect){
        return !parent.contains(rect);
    }

    /**check if given parameter is containted by parent
     * @return true - if given parameter is outside bounds of system
     */
    private boolean OutOfBounds(Point p){
        return !parent.contains(p);
    }

    private void expand(Point p) throws OutOfChunkBounds{

        Rectangle southEast = new Rectangle(parent.x,parent.y,parent.width*2,parent.height*2);
        Rectangle southWest = new Rectangle(parent.x-parent.width,parent.y,parent.width*2,parent.height*2);

        Rectangle NorthEast = new Rectangle(parent.x,parent.y-parent.height,parent.width*2,parent.height*2);
        Rectangle NorthWest = new Rectangle(parent.x-parent.width,parent.y-parent.height,parent.width*2,parent.height*2);

        if (southEast.contains(p)){
            expandTree(southEast);
        }
        else if (southWest.contains(p)){
            expandTree(southEast);
        }
        else if (NorthEast.contains(p)){
            expandTree(southEast);
        }
        else if (NorthWest.contains(p)){
            expandTree(southEast);
        }
        else{
            throw new OutOfChunkBounds("unable to create new chunks with the right constraints");
        }
    }

    private void expand(Rectangle rect) throws OutOfChunkBounds{

        Rectangle southEast = new Rectangle(parent.x,parent.y,parent.width*2,parent.height*2);
        Rectangle southWest = new Rectangle(parent.x-parent.width,parent.y,parent.width*2,parent.height*2);

        Rectangle NorthEast = new Rectangle(parent.x,parent.y-parent.height,parent.width*2,parent.height*2);
        Rectangle NorthWest = new Rectangle(parent.x-parent.width,parent.y-parent.height,parent.width*2,parent.height*2);

        if (southEast.contains(rect)){
            expandTree(southEast);                                                                       
        }

        else if (southWest.contains(rect)){
            expandTree(southWest);
        }

        else if (NorthEast.contains(rect)){
            expandTree(NorthEast);
        }

        else if (NorthWest.contains(rect)){
            expandTree(NorthWest);
        }

        else{
            System.out.println(rect +" is not contained by any parent nodes");
          
            //this should never happen, since we are increasing the storage by 4, and 
            throw new OutOfChunkBounds("unable to create new chunks with the right constraints");
        }
    }


    /** creates another parent, puts old parent in old parent as a child.
     * have check, does work*/ 
    private void expandTree(Rectangle newBounds){
        parent = new TreeNode(this,newBounds.x,newBounds.y,newBounds.width,newBounds.height,parent);
        
    }

    /**returns first (should only be one) tile at specified pos 
     * . if it cant find Tile, null is returned.
    */

    public Tile getTile(Point p) {
        
        return parent.getTileInPoint(p);
           
    }

    private int getPercentagegenerated(){
        int generated = 0;
        
        for (Chunk chunk:getAllChunksInSystem()){
            if (chunk.loaded){

                generated ++;
            } 
        }
        int percentage = 100*generated/getAllChunksInSystem().size();
        return percentage;
    }

    public void unload(ArrayList<Chunk> newWorkingchunks, ArrayList<Chunk> workingChunks) {
        for (Chunk wc:workingChunks){
            if (!newWorkingchunks.contains(wc)){
                wc.loaded = false;
            }
        }
    }

    public static void clearFile(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            // Set the length of the file to zero
            raf.setLength(0);
        }
    }

    private String generateNewID() {
        //TODO
        return ""+( new Random().nextInt());
    }

    public String getID() {
       return ID;
    }

    private void setID() {
        if (type==0 ){
            this.ID = generateNewID();
        }else{
            this.ID = ""+type;
        }
    }
}
