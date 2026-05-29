package resources.domain.object;

/**
 * 16-entry lookup mapping a fence's 4-bit N/E/S/W connection mask to the
 * basename of a PNG inside {@code resources/images/objects/structures/walls/fences/}.
 *
 * Bit encoding (matches {@link Fence#BIT_NORTH} etc.): N=1, E=2, S=4, W=8.
 *
 * The chosen filenames mix the artist's B-prefixed assets (corners + sides)
 * with the smaller {@code fence0..fence9} placeholders used for endposts,
 * T-junctions, and the cross. Misassignments are one-line edits in
 * {@link #TABLE} below — the orientation of the artist's "LeftSide" /
 * "RightSide" assets in particular is a best-guess from the filenames.
 *
 * Image lookup is routed through {@link resources.presentation.image.ObjectImageLoader}
 * via the {@code "fence_v"} name prefix (flat-folder hook), so this class
 * only owns the filename, not the path.
 */
public final class FenceVariants {

    private static final String[] TABLE = new String[16];

    static {
        // Numeric endposts and T-junctions use the small fence0..fence8
        // placeholder set. Corners and sides use the B-prefixed art.
        TABLE[0]  = "BfenceStandard";              // ---- isolated post
        TABLE[1]  = "fence0";                      // N--- endpost north
        TABLE[2]  = "fence1 (1)";                  // -E-- endpost east (filename has a space)
        TABLE[3]  = "BfenceTopRightCorner";        // NE-- corner NE
        TABLE[4]  = "fence2";                      // --S- endpost south
        TABLE[5]  = "BfenceLeftSide";              // N-S- vertical straight
        TABLE[6]  = "BfenceBottomRightCorner";     // -ES- corner SE
        TABLE[7]  = "fence3";                      // NES- T-junction (no W)
        TABLE[8]  = "fence4";                      // ---W endpost west
        TABLE[9]  = "BfenceLeftUpperCorner";       // N--W corner NW
        TABLE[10] = "BfenceRightSide";             // -E-W horizontal straight
        TABLE[11] = "fence5";                      // NE-W T-junction (no S)
        TABLE[12] = "BfenceLeftBottomCorner";      // --SW corner SW
        TABLE[13] = "fence6";                      // N-SW T-junction (no E)
        TABLE[14] = "fence7";                      // -ESW T-junction (no N)
        TABLE[15] = "fence8";                      // NESW cross
    }

    public static String fileForMask(int mask4bit) {
        int idx = mask4bit & 0xF;
        return TABLE[idx];
    }

    private FenceVariants() {}
}
