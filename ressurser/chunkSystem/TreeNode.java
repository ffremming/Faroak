package ressurser.chunkSystem;

import java.awt.Rectangle;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;

public class TreeNode {

    int level;

    int startXValue;
    int startYValue;
    int width;
    int height;

    ChunkSystem chunkS;

    Rectangle rect;

    final int CHUNKSIZE = 16;

    private TreeNode [] children = new TreeNode[4];

    public TreeNode(ChunkSystem chunkS,int startXValue,int startYValue,int width,int height,int level){
        this.chunkS = chunkS;

        rect = new Rectangle(startXValue,startYValue,width,height);

        this.startXValue = startXValue;
        this.startYValue = startYValue;
        this.width = width;
        this.height = height;

        level ++;
        this.level = level;
        //if (width > CHUNKSIZE){
            addChildren();
        //}
        
    }


    protected void addChildren(){

        

        if (width == CHUNKSIZE*2*chunkS.panel.tileSize){      //to create chunks at bottom lvl
            children[0] = (new Chunk(chunkS,startXValue,startYValue,width/2,height/2,level));
            children[1] = (new Chunk(chunkS,startXValue+width/2,startYValue,width/2,height/2,level));
            children[2] = (new Chunk(chunkS,startXValue,startYValue+height/2,width/2,height/2,level));
            children[3] = (new Chunk(chunkS,startXValue+width/2,startYValue+height/2,width/2,height/2,level));
        } else {

            children[0] = (new TreeNode(chunkS,startXValue,startYValue,width/2,height/2,level));
            children[1] = (new TreeNode(chunkS,startXValue+width/2,startYValue,width/2,height/2,level));
            children[2] = (new TreeNode(chunkS,startXValue,startYValue+height/2,width/2,height/2,level));
            children[3] = (new TreeNode(chunkS,startXValue+width/2,startYValue+height/2,width/2,height/2,level));

        }


        
    }

    public boolean hasChildren(){
        return children[0]!= null;
    }




    protected void getAllChunks(Rectangle rect,ArrayList<Chunk> list){
        //System.out.println("get all chunks");
        for(int i=0; i<children.length; i++) {
            if (children[i].rect.contains(rect)||children[i].rect.intersects(rect)){
                //System.out.println("child get chunks");
                children[i].getAllChunks(rect,list);
                

            }
        }

    }


    public void addEntity(BaseEntity entity){
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

    
}


