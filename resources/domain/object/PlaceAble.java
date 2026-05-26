package resources.domain.object;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.domain.player.Playable;
import resources.domain.inventory.Item;
import resources.domain.inventory.Stack;

import resources.app.GamePanel;

public class PlaceAble extends GameObject {

    public PlaceAble(GamePanel panel, String name, int worldX, int worldY, int width, int height, int hitBoxWidth,
            int hitBoxHeight, int i, int j, boolean solid) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight, i, j, solid);
        //TODO Auto-generated constructor stub
    }
    
}
