package resources.domain.tile;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.geometry.HitBox;
import resources.presentation.image.ImageContainer;

import resources.app.GamePanel;

public class MicroFeatureTile extends Tile {

    public MicroFeatureTile(GamePanel panel, String name, int worldX, int worldY, int altitude) {
        super(panel, name, worldX, worldY, altitude);
        //TODO Auto-generated constructor stub
    }

    private void setImages(){
        images.add(getImage());
    }
}
