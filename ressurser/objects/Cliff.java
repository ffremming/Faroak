package ressurser.objects;

import ressurser.main.GamePanel;

public class Cliff extends SuperObject {

    public Cliff(GamePanel gp, ObjectManager objM, int worldX, int worldY, String name, String type) {
        super(gp, objM, worldX, worldY, name, type);
        //TODO Auto-generated constructor stub
        directionCollision = "opp";
        pixlePlusYvalue = -16;
    }
}
