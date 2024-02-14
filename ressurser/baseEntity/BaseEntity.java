package ressurser.baseEntity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import ressurser.baseEntity.sprite.Sprite;
import ressurser.drawing.Camera;
import ressurser.main.GamePanel;

public class BaseEntity {

    public GamePanel panel;

    short age;
    String name;
    String nameID;
    protected byte type;

    byte TILE = 0;protected byte SUPEROBJECT = 1;byte ENTITY = 2;

    Sprite sprite;

    public int worldX;

    public int worldY;
    protected short width;

    protected short height;

    protected HitBox hitBox;
    int hitBoxWidth,hitBoxHeight,relativeXValue,relativeYValue;
    

    //hard collision is collision that is non passable
    boolean hardCollision = false;
    
    
    
    boolean interactable;

    

    //might need to change to sprite.
    public boolean animated;

    public BaseEntity(GamePanel panel,String name,int worldX,int worldY,short width,short height,short hitBoxWidth,short hitBoxHeight,int i,int j){
        this.panel = panel;


        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.name = name;

        //chance that some values is not initialized..



        hitBox = new HitBox(this, hitBoxWidth, hitBoxHeight, i, j);

        //spriteHandler = new SpriteHandler(this);
        //sprite = new Sprite(this,"filenavn",worldX,worldY,width,height);
    } 
    /**
     * for unusual objects that doesnt need position or other.
     */
    public BaseEntity (GamePanel panel,String name){
        this.panel = panel;
        this.name = name;
        worldX = 0;
        worldY = 0;
        width = 0;
        height = 0;
        hitBoxWidth = 0;
        hitBoxHeight = 0;
    }

    public BufferedImage getImage(){
        
        BufferedImage image = null;
        
        try {

            image = panel.imageContainer.getTileImage(name);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return image;
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

    //got to check out if this actually works.. a functions should be made to decide what col u are in.
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
    public Boolean getAnimated() {
        return animated;
    }
    public short getWidth() {
        return width;
    }
    public short getHeight() {
        return height;
    }
    /**positions wiht the center of object at point */
    public void centerAtPosition(Point p){
        worldX = p.x-width/2;
        worldY = p.y-height/2;
        hitBox.updateCoords();
    }
    /**position with the absolute values at given point */
    public void position(Point p){
        worldX = p.x;
        worldY = p.y;
        hitBox.updateCoords();
    }   

    public void draw(Camera camera,Graphics2D g2){
        
        int x = getWorldX()-camera.worldX;
        int y = getWorldY()-camera.worldY;

        g2.setColor(Color.WHITE);
       
        g2.drawImage(sprite.getImage(),x,y,64,64,null);
        g2.drawString(getName(),x,y+15);

        //UNCOMPLETED. NEEDS A BETTER METHOD FOR DRAWING USING THE SPRITE SHIT
    }
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> arr = new ArrayList<>();
        arr.add(getImage());
       return arr;
    }

    public void animate(int value){
        
    }
}
