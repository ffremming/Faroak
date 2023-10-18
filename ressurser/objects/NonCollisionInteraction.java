package ressurser.objects;

import ressurser.main.GamePanel;

public class NonCollisionInteraction extends InteractableObj{

    public NonCollisionInteraction(GamePanel gp, ObjectManager objM, int worldX, int worldY,String name,String type) {
        super(gp, objM,  worldX, worldY,name,type);
        collision = false;
    }

  

    @Override
    public void interact() {
    }
    
}
