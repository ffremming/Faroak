package resources.domain.entity;

import resources.geometry.HitBox;
import resources.geometry.Vector;


import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.domain.tile.Tile;
import resources.app.GamePanel;

public class BaseEntity implements Tickable, Drawable {

    public GamePanel panel;

    private int age;
    protected String name;
    private String ID;
    protected boolean solid;

    

    

    public double worldX;
    public double worldY;
    protected int width;
    protected int height;

    protected HitBox hitBox;
    int hitBoxWidth,hitBoxHeight,relativeXValue,relativeYValue;
    
    public ArrayList<BufferedImage> images = new ArrayList<>();
    //might need to change to sprite.
    public boolean animated;

    public boolean lightSource = false;
    public int light = 0;

    public BaseEntity(GamePanel panel,String name,int worldX,int worldY,int width,int height,int hitBoxWidth,int hitBoxHeight,int i,int j){
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

    public BaseEntity(GamePanel panel2, String name, double worldX, double worldY, int width, int height,
            HitBox hitBox) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.width = width;
            this.height = height;
            this.name = name;
            this.hitBox = hitBox;
        
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


    public double getWorldX() {
        return worldX;
    }
    public double getWorldY() {
        return worldY;
    }


    public HitBox getHitBox() {
        return hitBox;
    }

    public HitBox getImageHitbox(){
        return new HitBox(worldX,worldY,width,height);
    }

    public void draw(Graphics2D g2){
        //panel.drawingM.draw
    }

    

    public String getID(){
        return ID;
    }

    //got to check out if this actually works.. a functions should be made to decide what col u are in.
   


    public String getName() {
        return name;
    }

    public boolean collision(BaseEntity be){
        return hitBox.collision(be.hitBox);
    }
    public boolean enlargedCollision(BaseEntity be){
        return hitBox.getEnlargedCameraHitbox().collision(be.hitBox);
    }

    
    public Boolean getAnimated() {
        return animated;
    }
    public int getWidth() {
        return width;
    }
    public int getHeight() {
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

    
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> arr = new ArrayList<>();
        if (!(this instanceof Tile)){
            arr.add(getImage());
        }else{
            this.getImages();
        }
        
       return arr;
    }

    public void animate(int value){
        
    }
    public void update() {
        
    }
    public Point getPoint() {
        return new Point((int)worldX,(int)worldY);
    }

    public boolean isSolid(){
        return solid;
    }

    @Override
    public String toString(){
        return "name: "+name+"\nsolid: " + solid+"\nanimated: "+animated + "\nlightSource: " + lightSource +
        "\ncoords: "+worldX+", "+worldY;

    }
    protected void setName(String name){
        this.name = name;
    }

    public void age(){
        age++;
    }
    public int getAge(){
        return age;
    }

    public void setWorldX(double x){
        worldX = x;
    }

    public void setWorldY(double y){
        worldY = y;
    }
    public int getLightLvl() {
        return light;
    }
}
