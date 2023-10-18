package ressurser.baseEntity.tile;




import ressurser.baseEntity.Sprite;
import ressurser.main.GamePanel;

public class TileSprite extends Sprite {

    


    public TileSprite(String basePath,GamePanel panel) {
       super(basePath);
        //TODO Auto-generated constructor stub

        activeImage = panel.spriteLoader.getTileImage(basePath);
    }

    
    
}
