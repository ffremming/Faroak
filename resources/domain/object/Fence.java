package resources.domain.object;

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
