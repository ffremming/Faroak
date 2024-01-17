package ressurser.baseEntity;

import java.util.ArrayList;

import ressurser.baseEntity.sprite.Sprite;

public abstract class SpriteHandler {
    
    protected BaseEntity baseEntity;

    ArrayList<Sprite> spriteList = new ArrayList<>();
    Sprite activeSprite;

    String spritePath;

    //an baseEntity will have one SpriteHandler
    //the spritehandler will store all the sprites the baseEntity will need.
    //the spritehandler SpriteList will contain different sprite variations. 

    //when a sprite is drawn, the spriteHandler class is called.

    public SpriteHandler (BaseEntity baseEntity){   
        loadSprites();
    }


    /*
     * will cahnge the active sprite.
     * could be when an object age or something.
     */
    public void changeActiveSprite(){
        
    }


    /*
     * will update the activeSprite
     */
    public void updateActiveSprite(){
        activeSprite.update();
    }


    public void loadSprites(){

    }
}
