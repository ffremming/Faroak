package resources.domain.object;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;

/**
 * Solid 64x64 placeable that visually connects to neighbouring fences.
 *
 * The hitbox covers only the bottom half (64x32) so the player's feet are
 * blocked while sprite tops can overlap. {@link #connectionMask(GamePanel)}
 * returns a 4-bit N/E/S/W mask of adjacent fences for future renderer use.
 */
public class Fence extends GameObject {

    /** N bit in connection mask. */
    public static final int BIT_NORTH = 1;
    /** E bit in connection mask. */
    public static final int BIT_EAST  = 1 << 1;
    /** S bit in connection mask. */
    public static final int BIT_SOUTH = 1 << 2;
    /** W bit in connection mask. */
    public static final int BIT_WEST  = 1 << 3;

    private static final int TILE = 64;

    public Fence(GamePanel panel, int worldX, int worldY) {
        super(panel, "fence", worldX, worldY,
              TILE, TILE,
              TILE, TILE / 2,   // hitbox 64x32 — bottom half only
              0, TILE / 2,
              true);
    }

    @Override
    public GameObject placementCandidate(GamePanel targetPanel) {
        return new Fence(targetPanel, (int) getWorldX(), (int) getWorldY());
    }

    /**
     * Preview clone is a real Fence so the variant-resolving {@link #getImage}
     * fires for the ghost too. The preview is drawn at full opacity (the
     * stored sprite already encodes the auto-connect choice); validity is
     * conveyed by the camera's invalid-overlay tint rather than a faded
     * sprite, matching the boat preview's behaviour.
     */
    @Override
    public GameObject getPreviewObject(GamePanel targetPanel) {
        return new Fence(targetPanel, (int) getWorldX(), (int) getWorldY());
    }

    /**
     * Resolve the variant sprite each frame by recomputing the neighbour
     * mask and routing the lookup name through the
     * {@code fence_v...} flat-folder hook in
     * {@link resources.presentation.image.ObjectImageLoader}.
     *
     * Recomputing on every draw is simple and self-correcting: place,
     * remove, or chunk-load events all reach this path naturally without
     * extra event plumbing. Cost is one O(N) scan per visible fence per
     * frame, which is fine at expected fence counts.
     */
    @Override
    public BufferedImage getImage() {
        if (panel != null && panel.world != null) {
            this.name = "fence_v" + FenceVariants.fileForMask(connectionMask(panel));
        }
        return super.getImage();
    }

    /**
     * The renderer draws via {@code getImages()}, not {@code getImage()}, so the
     * per-frame variant resolution in {@link #getImage()} must be driven from
     * here too — otherwise the sprite would freeze at whatever variant was
     * resolved during construction (always the isolated post, since the fence
     * has no neighbours when first built) and never react to fences placed or
     * removed around it. Recomputing here keeps placed fences, the placement
     * ghost, and chunk-reloaded fences all self-correcting.
     */
    @Override
    public ArrayList<BufferedImage> getImages() {
        getImage();
        return super.getImages();
    }

    /**
     * Bitmask of adjacent fences in the four cardinal directions, intended for
     * a future tile-variant renderer. Pure read query; no side effects.
     */
    public int connectionMask(GamePanel ctx) {
        int mask = 0;
        int x = (int) getWorldX();
        int y = (int) getWorldY();
        if (hasFenceAt(ctx, x,        y - TILE)) mask |= BIT_NORTH;
        if (hasFenceAt(ctx, x + TILE, y))        mask |= BIT_EAST;
        if (hasFenceAt(ctx, x,        y + TILE)) mask |= BIT_SOUTH;
        if (hasFenceAt(ctx, x - TILE, y))        mask |= BIT_WEST;
        return mask;
    }

    private boolean hasFenceAt(GamePanel ctx, int x, int y) {
        if (ctx == null || ctx.world == null) return false;
        for (BaseEntity ent : ctx.world.getEntities()) {
            if (ent instanceof Fence
                && (int) ent.getWorldX() == x
                && (int) ent.getWorldY() == y) {
                return true;
            }
        }
        return false;
    }
}
