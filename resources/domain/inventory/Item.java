package resources.domain.inventory;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;

import resources.domain.entity.BaseEntity;
import resources.app.GamePanel;

/** must be changed, not a good solution */
public class Item extends BaseEntity {
   
    BaseEntity physicalRepresentation;

    public Item(GamePanel panel, String name) {
        super(panel, name);
        //TODO Auto-generated constructor stub
        images.add(panel.images().getItemImage(name));
        physicalRepresentation = panel.items().getPhysicalRepresentation(name);
    }

    public BaseEntity getPhysicalRepresentation() {
        return panel.items().getPhysicalRepresentation(name);
    }

    
    
    
}
