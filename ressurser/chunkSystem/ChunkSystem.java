package ressurser.chunkSystem;

import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JFrame;


import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Entity;
import ressurser.baseEntity.HitBox;
import ressurser.baseEntity.tile.Tile;
import ressurser.main.GamePanel;


public class ChunkSystem {
    
    GamePanel panel;

    //updates all entities that should be rendered.
    ArrayList<BaseEntity> semiStaticEntitiesRendered = new ArrayList<BaseEntity>();
    
    //renderdistance is the distance from the player to the border of where entities is rendered.
    int renderDistance ;
    TreeNode mother;
    //
    final int SIZEPOW = 11;// 2^10 == 1024 338997 chunks.




    /**
     * the chunksystem starts by creating the mother treenode, that has an set size. this treenode will create childnodes til the children are small enought to be consideed a chunk.
     * 
     * if the chunksystem is not big enought, it should create another mother.
     */
    public ChunkSystem(GamePanel panel){
        this.panel = panel;

        //this should be lower. but not sure yet.
        renderDistance = 32*panel.tileSize;
        System.out.println((int)Math.pow(2,SIZEPOW)*panel.tileSize/panel.tileSize);
        mother = new TreeNode(this,-(int)Math.pow(2,SIZEPOW)*panel.tileSize/2,-(int)Math.pow(2,SIZEPOW)*panel.tileSize/2,(int)Math.pow(2,SIZEPOW)*panel.tileSize,(int)Math.pow(2,SIZEPOW)*panel.tileSize,5);

    }

    /**
     * returns all entities in bound. in bound mean that the hitbox collides.
     * search from nearby entities. return all entities that hitboxes collide.
     * 
     * @return list of all entities that collides with hitbox
     */
    public ArrayList<BaseEntity> getEntitiesInBound(HitBox hitBox){
        
        ArrayList<BaseEntity> entitiesInBound = new ArrayList<>();

        for (BaseEntity baseEntity : semiStaticEntitiesRendered){
            if (baseEntity.collision(hitBox)){
                if (baseEntity!= hitBox.getEntity()){
                    entitiesInBound.add(baseEntity);
                }
            }
        }
        return entitiesInBound;   
    }

    /**
     * returns all entities in bound. in bound mean that the hitbox collides.
     * search from all entities, not just nearby(still fast tho). return all entities that hitboxes collide.
     * 
     * @return list of all entities that collides with hitbox
     */
    
     public ArrayList<BaseEntity> getEntitiesInBound(Rectangle rect){
        ArrayList<BaseEntity> entitiesInBound = new ArrayList<>();
        return mother.getEntitiesInBound(rect,entitiesInBound);
    }

    /**
     * the method updates the enities that is nearby the baseentity in the parameter.
     * the method should not be called too frequently for perfomance.
     * the amount of room the method search for is decided by the renderdistance.
     */


    public void updateSemiStaticEntitiesRendered(BaseEntity entity){
      
        
        //load all chunks that is involved in the range of the render distance.

        semiStaticEntitiesRendered.clear();

        ArrayList<Chunk> chunks = getAllChunks(entity);

        for (Chunk chunk:chunks){
            semiStaticEntitiesRendered.addAll(chunk.getEntities());
        }

       // semiStaticEntitiesRendered = mother.loadEntitites(renderDistance,entity.getWorldX(),entity.getWorldX());
    }

    /**
     * returns a list of all chunks within render distance
     */

    private ArrayList<Chunk> getAllChunks(BaseEntity entity){

        int centerX = entity.getWorldX();
        int centerY = entity.getWorldY();

        int minX = centerX-renderDistance;
        int minY = centerY-renderDistance;

        Rectangle renderRect = new Rectangle(minX,minY,renderDistance*2,renderDistance*2);
        System.out.println(renderRect);
        ArrayList<Chunk> chunkList = new ArrayList<>();

        //calls recursive method, adds all chunks.
        mother.getAllChunks(renderRect,chunkList);

        return chunkList;
    }


    public ArrayList<BaseEntity> getSemiStaticEntitiesRendered(){
      return semiStaticEntitiesRendered;
    }

    /**
     * works with all entities, tiles and whatnot.
     */
    public void addEntity(BaseEntity entity){
        mother.addEntity(entity);
    }

    /**
     * sets entity at specified position. deletes all entities in the way. 
     * this does not seem to work in ant way?
     * 
     * a solutuion is that an intance of basentity ignores coords, and is placed by the chunksystem.
     */
    public void setEntity(BaseEntity newEntity,int row,int col){
        Rectangle rect = new Rectangle(row*panel.tileSize,col*panel.tileSize,panel.tileSize,panel.tileSize);
        getEntitiesInBound(rect);
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
        return mother.removeEntity(entity);
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
    

    public static void main(String[] args) {
        //System.out.println((int)Math.pow(2,10)*64);

        GamePanel panel = new GamePanel(new JFrame(),true);
        ChunkSystem cS = new ChunkSystem(panel);
        

        

        short w = 32;
        BaseEntity ent = new BaseEntity(panel,"name",641, 520, w, (short)64,(short)28,(short)40,(short)2,(short)24);
        BaseEntity ent2 = new BaseEntity(panel,"name",640, 530, w, (short)64,(short)28,(short)40,(short)2,(short)24);
        cS.addEntity(ent2);
        cS.addEntity(ent);

        System.out.println(cS.getAllChunks(ent).size()); 

        cS.updateSemiStaticEntitiesRendered(ent);
        cS.removeEntitiesInBound(ent.getHitBox());
        cS.updateSemiStaticEntitiesRendered(ent);

        //removeEntitiesInBound()
        System.out.println(cS.getEntitiesInBound(ent.getHitBox()));
       System.out.println(cS.semiStaticEntitiesRendered);
    }

}
