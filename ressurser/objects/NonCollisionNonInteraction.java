package ressurser.objects;

import ressurser.main.GamePanel;

public class NonCollisionNonInteraction extends NonInteractableObj{

    public NonCollisionNonInteraction(GamePanel gp, ObjectManager objM, int worldX, int worldY,String name,String type) {
        super(gp, objM,  worldX, worldY,name,type);
        collision = false;
    }
    
}

