package resources.domain.player;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.geometry.HitBox;
import resources.geometry.Vector;

/**
 * Entity capable of self-directed movement plus directional sprite frames.
 *
 * State (velocity, direction, current path, animation phase) lives here;
 * the per-tick movement loop is delegated to {@link MovementController} so the
 * collision/step policy can be swapped (boats, sliding, knockback) without
 * editing this class.
 */
public class Moveable extends Entity {

    private static final int ANIM_FRAME_RIGHT = 1;
    private static final int ANIM_FRAME_DOWN  = 0;  // up
    private static final int ANIM_FRAME_LEFT  = 3;
    private static final int ANIM_FRAME_UP    = 2;  // down
    private static final int ANIM_WRAP        = 60;
    private static final int ANIM_HALF        = 30;

    private final Vector velocity  = new Vector();
    private final Vector direction = new Vector(1, 1);
    final ArrayList<Vector> path   = new ArrayList<>();

    private double movementSpeed = 1;
    // Direction index conventions (see ANIM_FRAME_* constants above):
    //   0 = facing up, 1 = right, 2 = down, 3 = left.
    // Default to "down" so a freshly-spawned, never-moved player has their
    // interaction box centered on the area in front of them rather than
    // pointing skyward — case 0 in computeInteractionHitBox pushes the box
    // 50 px upward, which surprised users on the first SPACE press.
    private int    directionIndex = 2;
    private HitBox interactionHitBox;

    private final MovementController movement = new MovementController(this);

    public Moveable(GamePanel panel, String name, int worldX, int worldY,
                    short width, short height, short hitBoxWidth, short hitBoxHeight,
                    short relativeXPlus, short relativeYPlus) {
        super(panel, name, worldX, worldY, width, height, hitBoxWidth, hitBoxHeight,
            relativeXPlus, relativeYPlus);
        loadDirectionalSprites();
    }

    @Override
    public void update() {
        movement.step();
        advanceAnimation();
    }

    // ---- velocity / direction / path ----

    public Vector getVelocity()        { return velocity; }
    public Vector getDirection()       { return direction; }
    public double getMovementSpeed()   { return movementSpeed; }
    public void   setMovementSpeed(double v) { this.movementSpeed = v; }

    public void addVelocity(Vector v) { velocity.add(v); }
    public void setVelocity(Vector v) { velocity.set(v.x, v.y); }
    public void setPath(ArrayList<Vector> newPath) { path.clear(); path.addAll(newPath); }
    public void addPath(ArrayList<Vector> newPath) { path.addAll(newPath); }

    // ---- collision-friendly mutation (called by controller) ----

    public void moveBy(double dx, double dy) {
        worldX += dx;
        worldY += dy;
        getHitBox().updateCoords();
    }

    // ---- interaction box ----

    public void resetInteractionHitBox() { interactionHitBox = null; }

    public HitBox getInteractionHitBox() {
        if (interactionHitBox == null) interactionHitBox = computeInteractionHitBox();
        return interactionHitBox;
    }

    public HitBox getHitboxInfront() {
        HitBox hb = getHitBox();
        hb.updateCoords();
        return new HitBox(hb.x + (int) direction.getX(), hb.y + (int) direction.getY(),
            hb.width, hb.height);
    }

    /** Cardinal facing used by combat/input when no explicit aim key is held. */
    public Vector getFacingVector() {
        switch (directionIndex) {
            case ANIM_FRAME_DOWN:  return new Vector(0, -1); // up
            case ANIM_FRAME_RIGHT: return new Vector(1, 0);
            case ANIM_FRAME_UP:    return new Vector(0, 1);  // down
            case ANIM_FRAME_LEFT:  return new Vector(-1, 0);
            default:               return new Vector(1, 0);
        }
    }

    private HitBox computeInteractionHitBox() {
        HitBox hb = getHitBox();
        hb.updateCoords();
        int half = 64 / 2;
        int xOffset = 0, yOffset = 0;
        switch (directionIndex) {
            case 0: yOffset = -50; xOffset = -(half - hb.width  / 2); break;
            case 1: xOffset = hb.width / 2; yOffset = -(half - hb.height / 2); break;
            case 2: xOffset = -(half - hb.width / 2); break;
            case 3: yOffset = -(half - hb.height / 2); xOffset = -(64 - hb.width / 2); break;
            default: break;
        }
        return new HitBox(hb.x + xOffset, hb.y + yOffset, 64, 64);
    }

    // ---- sprite + animation ----

    private void loadDirectionalSprites() {
        images = panel.imageContainer.setPlayableImages(getName());
    }

    private void advanceAnimation() {
        if (!direction.hasNoVelocity()) {
            animationIndex += 2;
            if (animationIndex >= ANIM_WRAP) animationIndex = 1;
        }
        updateDirectionIndex();
    }

    private void updateDirectionIndex() {
        if      (direction.getX() > 0) directionIndex = ANIM_FRAME_RIGHT;
        else if (direction.getX() < 0) directionIndex = ANIM_FRAME_LEFT;
        else if (direction.getY() < 0) directionIndex = ANIM_FRAME_DOWN;
        else if (direction.getY() > 0) directionIndex = ANIM_FRAME_UP;
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        ArrayList<BufferedImage> out = new ArrayList<>();
        if (!images.isEmpty()) out.add(images.get(spriteIndex()));
        return out;
    }

    private int spriteIndex() {
        int base = directionIndex * 3;
        int phase = (animationIndex == 0) ? 0 : (animationIndex < ANIM_HALF) ? 1 : 2;
        return Math.max(0, base) + phase;
    }

    public void interact() {
        throw new UnsupportedOperationException("Unimplemented: subclass to react to interaction");
    }
}
