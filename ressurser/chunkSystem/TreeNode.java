package ressurser.chunkSystem;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.HitBox;

public class TreeNode extends HitBox{

    

    

    ChunkSystem chunkS;

    

    final int CHUNKSIZE = 16;

    boolean loaded;

    private TreeNode [] children = new TreeNode[4];

    public TreeNode(ChunkSystem chunkS,int x,int y,int width,int height){
        super(x,y,width,height);
        this.chunkS = chunkS;

            
            addChildren();
            
            
    }

    /**new inserted parent nodes */
    public TreeNode(ChunkSystem chunkS,int x,int y,int width,int height,TreeNode child){
        super(x,y,width,height);
        this.chunkS = chunkS;

            addChildren(child);
    }


    
    


    protected void addChildren(){

        if (width == CHUNKSIZE*chunkS.panel.tileSize){      //to create chunks at bottom lvl
            children[0] = (new Chunk(chunkS,x,y,width/2,height/2));
            children[1] = (new Chunk(chunkS,x+width/2,y,width/2,height/2));
            children[2] = (new Chunk(chunkS,x,y+height/2,width/2,height/2));
            children[3] = (new Chunk(chunkS,x+width/2,y+height/2,width/2,height/2));
        } else {

            children[0] = (new TreeNode(chunkS,x,y,width/2,height/2));
            children[1] = (new TreeNode(chunkS,x+width/2,y,width/2,height/2));
            children[2] = (new TreeNode(chunkS,x,y+height/2,width/2,height/2));
            children[3] = (new TreeNode(chunkS,x+width/2,y+height/2,width/2,height/2));
        }
    }

    /** only is used when adding new children when one child is already in the system. */
    protected void addChildren(TreeNode child){

        int childX = child.x;
        int childY = child.y;
        int counter = 0;
        for (int newX = x;newX<x+width;newX+=width/2){
            for (int newY = y;newY<y+width;newY+=width/2){
                if (newY != childY && newX != childX){
                        children[counter] =new TreeNode(chunkS,newX,newY,width/2,height/2);
                } else {
                    children[counter] = child;
                }
                counter ++;
            }
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

    /**figure out which chunk/node fits with rect. adds chunk to list. does not return. */
    protected void getAllChunks(Rectangle rect,ArrayList<Chunk> list){
       
        for(int i=0; i<children.length; i++) {
            if (children[i].contains(rect)||children[i].intersects(rect)){
              
                children[i].getAllChunks(rect,list);
            }
        }
    }

    /**
     * can be placed in different 
     */
    public void addEntity(BaseEntity entity) throws OutOfChunkBounds{
        for(int i=0; i<children.length; i++) {
            if (children[i].contains(entity.getHitBox())||children[i].intersects(entity.getHitBox())){
                children[i].addEntity(entity);
                return;
            }
        }
    }

    


    /**recursive method of getting all kinds of entities at specified point */
    public ArrayList<BaseEntity> getEntitiesInPoint (Point point,ArrayList<BaseEntity> arrayList){
        for(int i=0; i<children.length; i++) {
            if (children[i].contains(point)){
                
                children[i].getEntitiesInPoint(point,arrayList);
            }
        }
        return arrayList;
    }

     /**recursive method of getting all kinds of entities at specified rect(hitbox) */
    public ArrayList<BaseEntity> getEntitiesInBound (Rectangle rect,ArrayList<BaseEntity> arrayList){
        for(int i=0; i<children.length; i++) {
            if (children[i].contains(rect)||children[i].intersects(rect)){
                getEntitiesInBound(rect,arrayList);
            }
        }
        return arrayList;
    }


    public boolean removeEntity (BaseEntity entity) {
        for(int i=0; i<children.length; i++) {
            if (children[i].contains(entity.getHitBox())||children[i].intersects(entity.getHitBox()) ||children[i].intersects(new Rectangle((int)(entity.getWorldX()),(int)entity.getWorldY(),(int)entity.getWidth(),(int)entity.getHeight()))){
               
                if (children[i].removeEntity(entity)){
                    return true;
                }
                

            }
        }
        return false;
    }
    

    private BufferedImage getImageOfEntities(){
        return chunkS.proceduralGen.getImage(x,y,width,height);
    }

    
    void paintMap(){
        System.out.println("painting map for treeNode");
        try{
            BufferedImage image = getImageOfEntities();
            ImageIO.write(image, "png", new File("reeNodeImage.png"));
        }
        catch (IOException e){
            System.out.println("could not paint map");
            e.printStackTrace();
        }
	} 


    public int getWorldX(){
        return x;
    }
    public int getWorldY(){
        return y;
    }
    //methods from rectangle is inherited!
    

    protected TreeNode [] getChildren(){
        return children;
    }
    public int getSquareMeter(){
        return width*height;
    }
    
}


