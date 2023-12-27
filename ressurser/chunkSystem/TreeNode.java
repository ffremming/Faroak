package ressurser.chunkSystem;

import java.awt.Rectangle;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;

public class TreeNode {

    

    int startXValue;
    int startYValue;
    int width;
    int height;

    ChunkSystem chunkS;

    Rectangle rect;

    final int CHUNKSIZE = 16;

    boolean loaded;

    private TreeNode [] children = new TreeNode[4];

    public TreeNode(ChunkSystem chunkS,int startXValue,int startYValue,int width,int height){
        this.chunkS = chunkS;

        rect = new Rectangle(startXValue,startYValue,width,height);

        this.startXValue = startXValue;
        this.startYValue = startYValue;
        this.width = width;
        this.height = height;

       
        
        //if (width > CHUNKSIZE){
            addChildren();
        //}
        
    }


    protected void addChildren(){

        if (width == CHUNKSIZE*chunkS.panel.tileSize){      //to create chunks at bottom lvl
            children[0] = (new Chunk(chunkS,startXValue,startYValue,width/2,height/2));
            children[1] = (new Chunk(chunkS,startXValue+width/2,startYValue,width/2,height/2));
            children[2] = (new Chunk(chunkS,startXValue,startYValue+height/2,width/2,height/2));
            children[3] = (new Chunk(chunkS,startXValue+width/2,startYValue+height/2,width/2,height/2));
        } else {

            children[0] = (new TreeNode(chunkS,startXValue,startYValue,width/2,height/2));
            children[1] = (new TreeNode(chunkS,startXValue+width/2,startYValue,width/2,height/2));
            children[2] = (new TreeNode(chunkS,startXValue,startYValue+height/2,width/2,height/2));
            children[3] = (new TreeNode(chunkS,startXValue+width/2,startYValue+height/2,width/2,height/2));
        }
    }

    

    public boolean hasChildren(){
        return children[0]!= null;
    }

    protected void getAllChunks(ArrayList<Chunk> list){
        for(int i=0; i<children.length; i++) {
                children[i].getAllChunks(list);
            
        }
    }


    protected void getAllChunks(Rectangle rect,ArrayList<Chunk> list){
       
        for(int i=0; i<children.length; i++) {
            if (children[i].rect.contains(rect)||children[i].rect.intersects(rect)){
              
                children[i].getAllChunks(rect,list);
                

            }
        }

    }


    public void addEntity(BaseEntity entity) throws OutOfChunkBounds{
        for(int i=0; i<children.length; i++) {
            if (children[i].rect.contains(entity.getHitBox())||children[i].rect.intersects(entity.getHitBox())){
                children[i].addEntity(entity);
                return;
            }
        }
    }

    public void setEntity(BaseEntity newEntity,int row,int col,Rectangle rect){
        
    }
    


    public ArrayList<BaseEntity> getEntitiesInBound (Rectangle rect,ArrayList<BaseEntity> arrayList){
        for(int i=0; i<children.length; i++) {
            if (children[i].rect.contains(rect)||children[i].rect.intersects(rect)){
                getEntitiesInBound(rect,arrayList);
            }
        }
        return arrayList;
    }


    public boolean removeEntity (BaseEntity entity) {
        for(int i=0; i<children.length; i++) {
            if (children[i].rect.contains(entity.getHitBox())||children[i].rect.intersects(entity.getHitBox())){
               
                return children[i].removeEntity(entity);
                

            }
        }
        return false;
    }

    protected ArrayList<Integer> getBounds(){
        ArrayList<Integer> listOfCoords = new ArrayList<>();
        listOfCoords.add(startXValue);
        listOfCoords.add(startYValue);

        listOfCoords.add(startXValue+width);
        listOfCoords.add(startYValue+height);
        return listOfCoords;
    }

    public int getWorldX(){
        return startXValue;
    }
    public int getWorldY(){
        return startYValue;
    }
    public int getWidth(){
        return width;
    }
    public int getHeight(){
        return height;
    }
    
}


