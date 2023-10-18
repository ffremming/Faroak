package ressurser.objects;

import ressurser.main.GamePanel;

abstract public class InteractableObj extends SuperObject{

    public InteractableObj(GamePanel gp,ObjectManager objM,int worldX,int worldY,String name,String type) {
        super(gp, objM,worldX,worldY,name,type);
    }
    
    public abstract void interact();
}
