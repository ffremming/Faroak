package ressurser.baseEntity;

import ressurser.main.GamePanel;

public class primitiveEntity extends BaseEntity {

    public primitiveEntity(GamePanel panel, String name, int worldX, int worldY, int width, int height) {
        super(panel, name, worldX, worldY, width, height, width, height, 0, 0);
        //TODO Auto-generated constructor stub
    }

    public primitiveEntity(GamePanel panel, String name) {
        super(panel, name);
        //TODO Auto-generated constructor stub
    }
}
