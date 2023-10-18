package ressurser.objects;

import ressurser.main.GamePanel;

public class NonInteractableObj extends SuperObject{

    public NonInteractableObj(GamePanel gp, ObjectManager objM,int worldX,int worldY,String name,String type) {
        super(gp, objM, worldX, worldY,name,type);
        interactable = false;
    }
    
}
