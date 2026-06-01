package resources.domain.object;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;

/**
 * Solid 64x64 placeable that visually connects to neighbouring stone walls.
 *
 * Mirrors {@link Fence}: the hitbox covers only the bottom half (64x32) so the
 * player's feet are blocked while sprite tops can overlap, and
 * {@link #connectionMask(GamePanel)} returns a 4-bit N/E/S/W mask of adjacent
 * stone walls that drives the auto-tiling variant lookup.
 */
public class StoneWall extends GameObject {

    /** N bit in connection mask. */
    public static final int BIT_NORTH = 1;
    /** E bit in connection mask. */
    public static final int BIT_EAST  = 1 << 1;
    /** S bit in connection mask. */
    public static final int BIT_SOUTH = 1 << 2;
    /** W bit in connection mask. */
    public static final int BIT_WEST  = 1 << 3;

    private static final int TILE = 64;

    public StoneWall(GamePanel panel, int worldX, int worldY) {
        super(panel, "stone_wall", worldX, worldY,
              TILE, TILE,
              TILE, TILE / 2,   // hitbox 64x32 — bottom half only
              0, TILE / 2,
              true);
    }

    @Override
    public GameObject placementCandidate(GamePanel targetPanel) {
        return new StoneWall(targetPanel, (int) getWorldX(), (int) getWorldY());
    }

    /**
     * Preview clone is a real StoneWall so the variant-resolving
     * {@link #getImage} fires for the ghost too. Drawn at full opacity (the
     * stored sprite already encodes the auto-connect choice); validity is
     * conveyed by the camera's invalid-overlay tint, matching the fence/boat
     * preview behaviour.
     */
    @Override
    public GameObject getPreviewObject(GamePanel targetPanel) {
        return new StoneWall(targetPanel, (int) getWorldX(), (int) getWorldY());
    }

    /**
     * Resolve the variant sprite each frame by recomputing the neighbour mask
     * and routing the lookup name through the {@code stone_wall_v...}
     * flat-folder hook in
     * {@link resources.presentation.image.ObjectImageLoader}.
     *
     * Recomputing on every draw is simple and self-correcting: place, remove,
     * or chunk-load events all reach this path naturally without extra event
     * plumbing.
     */
    @Override
    public BufferedImage getImage() {
        if (panel != null && panel.world != null) {
            this.name = "stone_wall_v" + StoneWallVariants.fileForMask(connectionMask(panel));
        }
        return super.getImage();
    }

    /**
     * The renderer draws via {@code getImages()}, not {@code getImage()}, so the
     * per-frame variant resolution must be driven from here too — otherwise the
     * sprite would freeze at whatever variant was resolved during construction
     * (always the isolated block) and never react to walls placed or removed
     * around it. See {@link Fence#getImages()} for the same reasoning.
     */
    @Override
    public ArrayList<BufferedImage> getImages() {
        getImage();
        return super.getImages();
    }

    /**
     * Bitmask of adjacent stone walls in the four cardinal directions. Pure
     * read query; no side effects.
     */
    public int connectionMask(GamePanel ctx) {
        int mask = 0;
        int x = (int) getWorldX();
        int y = (int) getWorldY();
        if (hasWallAt(ctx, x,        y - TILE)) mask |= BIT_NORTH;
        if (hasWallAt(ctx, x + TILE, y))        mask |= BIT_EAST;
        if (hasWallAt(ctx, x,        y + TILE)) mask |= BIT_SOUTH;
        if (hasWallAt(ctx, x - TILE, y))        mask |= BIT_WEST;
        return mask;
    }

    private boolean hasWallAt(GamePanel ctx, int x, int y) {
        if (ctx == null || ctx.world == null) return false;
        for (BaseEntity ent : ctx.world.getEntities()) {
            if (ent instanceof StoneWall
                && (int) ent.getWorldX() == x
                && (int) ent.getWorldY() == y) {
                return true;
            }
        }
        return false;
    }
}
