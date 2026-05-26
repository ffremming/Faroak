package resources.domain.inventory;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;

import java.util.HashMap;

import resources.domain.entity.BaseEntity;
import resources.domain.object.PlaceAble;
import resources.app.GamePanel;

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
        physicalRepresentations.put("demoHouse",new PlaceAble(panel, "demoHouse", 0, 0, 3*64, 2*64, 64, 64, 0, 64, true));
        physicalRepresentations.put("block",new PlaceAble(panel, "block", 0, 0, 64, 64, 64, 64, 0, 64, true));
        
    }

    public BaseEntity getPhysicalRepresentation(String name) {
        return physicalRepresentations.get(name);
    }


}
