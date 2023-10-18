package ressurser.objects;

import ressurser.main.GamePanel;

public class FirePlant extends Farmable {

    public FirePlant(GamePanel gp, ObjectManager objM, int worldX, int worldY, String name, String type) {
        super(gp, objM, worldX, worldY, name, type);
        yieldType = "fireFruit";
    }
    
}
