package ressurser.baseEntity.playable;

import ressurser.baseEntity.playable.Inventory.Inventory;
import ressurser.baseEntity.playable.Inventory.Item;
import ressurser.main.GamePanel;

public class Playable extends Moveable{

    
    Inventory inventory;
    Item equipped;

    public Playable(GamePanel panel, String name, int worldX, int worldY, short width, short height, short hitBoxWidth,
            short hitBoxHeight, short relativeXPLus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, relativeXPLus, relativeYPlus);
        //TODO Auto-generated constructor stub

        inventory = new Inventory(null);
        panel.userInterface.addContainer(inventory);
    }

    /** 
     * method should try to interact with whatever is in front. if any tools is equiped, that tool is used*/ 
    public void interact(){

    }

    public void nullPath(){
        path.clear();
    }

    

    
}
