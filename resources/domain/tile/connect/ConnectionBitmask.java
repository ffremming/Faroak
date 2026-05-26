package resources.domain.tile.connect;

import resources.domain.entity.BaseEntity;

/**
 * Packs which of an entity's neighbours connect to it into an 8-bit field.
 * Bit layout (clockwise from N): N=0, NE=1, E=2, SE=3, S=4, SW=5, W=6, NW=7.
 *
 * Used by {@link ConnectingSprite} implementations to look up the right
 * sprite variant — fences, cliffs, walls, bushes all funnel their neighbour
 * state through this single representation so the strategy that maps mask →
 * sprite can be swapped without changing the bookkeeping.
 */
public final class ConnectionBitmask {

    public static final int N  = 1 << 0;
    public static final int NE = 1 << 1;
    public static final int E  = 1 << 2;
    public static final int SE = 1 << 3;
    public static final int S  = 1 << 4;
    public static final int SW = 1 << 5;
    public static final int W  = 1 << 6;
    public static final int NW = 1 << 7;

    public static final int CARDINAL = N | E | S | W;

    private ConnectionBitmask() {}

    /**
     * 4-cardinal bitmask for {@code self}; {@code neighbours} must be ordered
     * [N, E, S, W] (Tile's {@code getNeighbors()} convention).
     */
    public static int cardinal(BaseEntity self, BaseEntity[] neighbours, ConnectionRule rule) {
        int mask = 0;
        if (matches(self, neighbours, 0, rule)) mask |= N;
        if (matches(self, neighbours, 1, rule)) mask |= E;
        if (matches(self, neighbours, 2, rule)) mask |= S;
        if (matches(self, neighbours, 3, rule)) mask |= W;
        return mask;
    }

    public static boolean has(int mask, int bit) { return (mask & bit) != 0; }

    private static boolean matches(BaseEntity self, BaseEntity[] neighbours, int i, ConnectionRule rule) {
        return i < neighbours.length && neighbours[i] != null && rule.connects(self, neighbours[i]);
    }
}
