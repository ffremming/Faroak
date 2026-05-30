package resources.domain.tile;

/**
 * Deterministic per-cell selection for the layered water system. A stable hash
 * of world coordinates picks which seabed variant draws under a water tile and
 * whether a sparse detail overlay (bubble/sparkle/ripple) appears — so the water
 * field never visibly repeats and never shimmers between frames (hash, not RNG).
 */
public final class SeabedPicker {

    public static final int SEABED_VARIANTS = 8;
    public static final int DETAIL_VARIANTS = 3;
    /** Roughly 1-in-N water tiles carries a detail overlay. */
    private static final int DETAIL_SPARSITY = 11;

    private SeabedPicker() {}

    private static int hash(int x, int y) {
        int h = x * 73856093 ^ y * 19349663;
        h ^= (h >>> 13);
        return h & 0x7fffffff;
    }

    /** Seabed sprite name (seabed0..7) for the cell at the given world coords. */
    public static String seabedFor(int worldX, int worldY) {
        return "seabed" + (hash(worldX, worldY) % SEABED_VARIANTS);
    }

    /** Detail sprite name, or null if this cell has no detail overlay. */
    public static String detailFor(int worldX, int worldY) {
        int h = hash(worldX + 1, worldY - 1);
        if (h % DETAIL_SPARSITY != 0) return null;
        return "oceanDetail" + ((h / DETAIL_SPARSITY) % DETAIL_VARIANTS);
    }
}
