package ressurser.baseEntity.tile;

import ressurser.main.GamePanel;

public class MicroFeatureTile extends Tile {

    public MicroFeatureTile(GamePanel panel, String name, int worldX, int worldY, int altitude) {
        super(panel, name, worldX, worldY, altitude);
        //TODO Auto-generated constructor stub
    }

    private void setImages(){
        images.add(getImage());
    }
}
