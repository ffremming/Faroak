package ressurser.baseEntity.playable;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.Entity;
import ressurser.baseEntity.gameObject.GameObject;
import ressurser.baseEntity.playable.Inventory.Inventory;
import ressurser.baseEntity.playable.Inventory.Item;
import ressurser.main.GamePanel;
import java.awt.Rectangle;

public class Playable extends Moveable{

    
    private static final String BaseEntity = null;
    Inventory inventory;
    Item equipped;

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
        //TODO Auto-generated constructor stub

        inventory = new Inventory(null);
        panel.userInterface.clear();
        panel.userInterface.addContainer(inventory);


        Item hammer = new Item(panel,"hammer");
        addItem(hammer);
        equipped = hammer;

    }



    /** 
     * method should try to interact with whatever is in front. if any tools is equiped, that tool is used*/ 
    public void interact(){
    
        for (BaseEntity ent:panel.chunkSystem.workingMemory.getEntities()){
            if (ent instanceof GameObject){
              
                //TODO this does not work yet.
                GameObject e = (GameObject) ent;
                if (e.getHitBox().intersects(getInteractionHitBox())){
                    e.interact(this);
                }
            }
        }

    }

    public void nullPath(){
        path.clear();
    }

    public void addItem(Item item){
        inventory.addItem(item);
    }

    public Item getItem(){
        return equipped;
    }

    

    

    
}
