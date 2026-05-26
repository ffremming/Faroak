package resources.domain.entity;

import resources.geometry.HitBox;
import resources.geometry.Vector;

import resources.app.GamePanel;

public class PrimitiveEntity extends BaseEntity {

    public PrimitiveEntity(GamePanel panel, String name, int worldX, int worldY, int width, int height) {
        super(panel, name, worldX, worldY, width, height, width, height, 0, 0);
        //TODO Auto-generated constructor stub
    }

    public PrimitiveEntity(GamePanel panel, String name) {
        super(panel, name);
        //TODO Auto-generated constructor stub
    }
}
