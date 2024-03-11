package ressurser.baseEntity.gameObject;

import java.util.ArrayList;

import ressurser.baseEntity.playable.Playable;
import ressurser.main.GamePanel;

public class InteractableGameObject extends GameObject {

    ArrayList<ActionModule> images;

    public InteractableGameObject(GamePanel panel, String name, int worldX, int worldY, short width, short height,
            short hitBoxWidth, short hitBoxHeight, short i, short j, boolean solid) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, i, j, solid);
        //TODO Auto-generated constructor stub
    }

    
    
}
