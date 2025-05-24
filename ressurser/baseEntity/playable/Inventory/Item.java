package ressurser.baseEntity.playable.Inventory;

import ressurser.baseEntity.BaseEntity;
import ressurser.main.GamePanel;

/** must be changed, not a good solution */
public class Item extends BaseEntity {
   
    BaseEntity physicalRepresentation;

    public Item(GamePanel panel, String name) {
        super(panel, name);
        //TODO Auto-generated constructor stub
        images.add(panel.imageContainer.getItemImage(name));
        physicalRepresentation = panel.itemM.getPhysicalRepresentation(name);
    }

    public BaseEntity getPhysicalRepresentation() {
        return panel.itemM.getPhysicalRepresentation(name);
    }

    
    
    
}
