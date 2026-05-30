package resources.domain.entity;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.component.ComponentBag;
import resources.domain.entity.component.EntityComponent;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;

/**
 * Root of the entity tree. Holds identity (name, ID), world position, hitbox,
 * sprite stack, age counter, and component bag.
 *
 * Behaviour pluggable via {@link EntityComponent} composition — see
 * {@link #addComponent(EntityComponent)}. Tickable components automatically
 * receive {@link #update()} each simulation step.
 */
public class BaseEntity implements Tickable, Drawable {

    private final ComponentBag components = new ComponentBag(this);

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
    int hitBoxWidth, hitBoxHeight;

    public ArrayList<BufferedImage> images = new ArrayList<>();
    public boolean animated;
    public boolean lightSource = false;
    public int     light       = 0;

    // ---- construction ----

    public BaseEntity(GamePanel panel, String name, int worldX, int worldY,
                      int width, int height, int hitBoxWidth, int hitBoxHeight, int i, int j) {
        this.panel = panel;
        this.name  = name;
        this.worldX = worldX;
        this.worldY = worldY;
        this.width  = width;
        this.height = height;
        this.hitBox = new HitBox(this, hitBoxWidth, hitBoxHeight, i, j);
    }

    public BaseEntity(GamePanel panel, String name) {
        this.panel = panel;
        this.name  = name;
    }

    public BaseEntity(GamePanel panel, String name, double worldX, double worldY,
                      int width, int height, HitBox hitBox) {
        this.panel = panel;
        this.name = name;
        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.hitBox = hitBox;
    }

    // ---- components ----

    public <T extends EntityComponent> T addComponent(T component)              { return components.add(component); }
    public <T extends EntityComponent> T getComponent(Class<T> type)            { return components.get(type); }
    public boolean hasComponent(Class<? extends EntityComponent> type)          { return components.has(type); }
    public ComponentBag components() { return components; }

    // ---- core ticking + drawing ----

    @Override
    public void update() {
        for (EntityComponent c : components.all()) {
            if (c instanceof Tickable) ((Tickable) c).update();
        }
    }

    public void animate(int value) {}

    public void draw(Graphics2D g2) {}

    public BufferedImage getImage() {
        try { return panel.imageContainer.getTileImage(name); }
        catch (Exception e) { System.err.println("[BaseEntity] getImage failed for '" + name + "': " + e); return null; }
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> out = new ArrayList<>();
        if (!(this instanceof Tile)) out.add(getImage());
        return out;
    }

    // ---- collision ----

    public boolean collision(HitBox hb)            { return hitBox.collision(hb); }
    public boolean collision(BaseEntity be)         { return hitBox.collision(be.hitBox); }
    public boolean enlargedCollision(BaseEntity be) { return hitBox.getEnlargedCameraHitbox().collision(be.hitBox); }

    public HitBox getHitBox()       { return hitBox; }
    public HitBox getImageHitbox()  { return new HitBox(worldX, worldY, width, height); }

    // ---- identity + position ----

    public String getID()      { return ID; }
    public String getName()    { return name; }
    protected void setName(String name) { this.name = name; }

    @Override public double getWorldX() { return worldX; }
    @Override public double getWorldY() { return worldY; }
    @Override public int    getWidth()  { return width; }
    @Override public int    getHeight() { return height; }
    public Point getPoint()             { return new Point((int) worldX, (int) worldY); }

    public void setWorldX(double v) { worldX = v; }
    public void setWorldY(double v) { worldY = v; }

    public void centerAtPosition(Point p) {
        worldX = p.x - width  / 2;
        worldY = p.y - height / 2;
        hitBox.updateCoords();
    }

    public void position(Point p) {
        worldX = p.x;
        worldY = p.y;
        hitBox.updateCoords();
    }

    // ---- misc state ----

    public boolean isSolid()       { return solid; }
    public Boolean getAnimated()   { return animated; }
    public int     getLightLvl()   { return light; }

    public void age()              { age++; }
    public int  getAge()           { return age; }

    /** Lifecycle hook for subclasses; default no-op. */
    public void remove() {}

    @Override
    public String toString() {
        return "name: " + name
            + "\nsolid: " + solid
            + "\nanimated: " + animated
            + "\nlightSource: " + lightSource
            + "\ncoords: " + worldX + ", " + worldY;
    }
}
