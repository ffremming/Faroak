package ressurser.baseEntity;

import java.awt.Graphics2D;

import ressurser.main.GamePanel;

public class BaseEntity {

    public GamePanel panel;

    short age;
    String name;
    String nameID;
    protected byte type;

    byte TILE = 0;protected byte SUPEROBJECT = 1;byte ENTITY = 2;


    public int worldX;

    public int worldY;
    protected short width;

    protected short height;

    HitBox hitBox;
    int hitBoxWidth,hitBoxHeight,relativeXValue,relativeYValue;
    protected SpriteHandler spriteHandler;

    //hard collision is collision that is non passable
    boolean hardCollision = false;
    
    
    boolean interactable;

    

    //might need to change to sprite.
    public boolean animated;

    public BaseEntity(GamePanel panel,String name,int worldX,int worldY,short width,short height,short hitBoxWidth,short hitBoxHeight,short relativeXPLus,short relativeYPlus){
        this.panel = panel;

        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;



        hitBox = new HitBox(this, hitBoxWidth, hitBoxWidth, relativeXPLus, relativeYPlus);

        //spriteHandler = new SpriteHandler(this);
        //sprite = new Sprite(this,"filenavn",worldX,worldY,width,height);
    } 


    /*
     * removes the entity, should call the datastructure to remove the entity, not yet implementet.
     */
    public void remove(){

    }


    public boolean collision(HitBox hitBox2) {
        return hitBox.collision(hitBox2);
    }


    public int getWorldX() {
        return worldX;
    }
    public int getWorldY() {
        return worldY;
    }


    public HitBox getHitBox() {
        return hitBox;
    }

    public void draw(Graphics2D g2){
        //panel.drawingM.draw
    }

    /**
     * interaction that is called from the interactionOrigin.
     * 
     */
    public void interact(Entity interactionOrigin) {

    }

    public String getNameID(){
        return nameID;
    }


    public int getRow() {
        return worldX/panel.tileSize;
    }


    public int getCol() {
        return worldY/panel.tileSize;
    }


    public String getName() {
        return name;
    }

    public boolean collision(BaseEntity be){
        return hitBox.collision(be.hitBox);
    }

    public boolean hardCollision(BaseEntity be){
        return (collision(be) && be.hardCollision);
    }
}
