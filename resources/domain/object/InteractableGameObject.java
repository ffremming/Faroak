package resources.domain.object;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.domain.player.Playable;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;

import java.util.ArrayList;
import resources.app.GamePanel;

public class InteractableGameObject extends GameObject {

    ArrayList<ActionModule> images;

    public InteractableGameObject(GamePanel panel, String name, int worldX, int worldY, short width, short height,
            short hitBoxWidth, short hitBoxHeight, short i, short j, boolean solid) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, i, j, solid);
    }

    
    
}
