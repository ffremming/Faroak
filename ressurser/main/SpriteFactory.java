package ressurser.main;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.ArrayList;

import ressurser.baseEntity.BaseEntity;

public class SpriteFactory {
    private HashMap<String,Arraylist<BufferedImage>> spriteMap = new HashMap<>();
    public SpriteFactory(){

    
    }

    public BufferedImage getImage(BaseEntity entity){
        String name = entity.getName();

        Boolean animated = entity.getAnimated();

        //TILE SYSTEM FOR BORDERS IS NEEDED: PROBABLY A VISUAL SOLUTION WITH JUST APPENDING IS THE BEST.


    }

    private void loadImages(){

    }
}
