package ressurser.baseEntity.playable.Inventory;

import java.util.HashMap;

import ressurser.baseEntity.BaseEntity;
import ressurser.baseEntity.gameObject.PlaceAble;
import ressurser.main.GamePanel;

public class ItemManager {

    GamePanel panel;

    private HashMap <String,Item> items = new HashMap<String,Item>();
    private HashMap <String,BaseEntity> physicalRepresentations = new HashMap<String,BaseEntity>();

    public ItemManager(GamePanel panel) {
        this.panel = panel;
        setupPR();
       
    }

    

    private void setupPR(){
        physicalRepresentations.put("hammer",new PlaceAble(panel, "demoHouse", 0, 0, 5*64, 5*64, 5*64, 64, 0, 4*64, true));
   
    }

    public BaseEntity getPhysicalRepresentation(String name) {
        return physicalRepresentations.get(name);
    }


}
