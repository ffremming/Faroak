package resources.domain.object;

/**
 * 16-entry lookup mapping a fence's 4-bit N/E/S/W connection mask to the
 * basename of a PNG inside {@code resources/images/objects/structures/walls/fences/}.
 *
 * Bit encoding (matches {@link Fence#BIT_NORTH} etc.): N=1, E=2, S=4, W=8.
 *
 * These assignments are PLACEHOLDERS using the existing {@code fence0..fence9 +
 * fence!} art, mapped by actually inspecting each sprite's shape rather than its
 * filename. The clean numeric set covers 11 of the 16 cases exactly; the five
 * cases with no dedicated art (the isolated post and the four single-direction
 * endposts) reuse the nearest straight piece — a vertical bar ({@code fence0})
 * for the isolated/N/S stubs and a horizontal bar ({@code fence!}) for the E/W
 * stubs — so a lone or dead-ended fence still reads as a fence. Replace with a
 * proper 16-tile sheet later via {@link resources.presentation.image.FenceSpriteSheet}.
 *
 * Observed shapes of the existing art:
 *   fence0 = vertical bar      fence! = horizontal bar    fence5 = 4-way cross
 *   fence7 = corner NE (└)     fence9 = corner NW (┘)
 *   fence1 (1) = corner ES (┌) fence3 = corner SW (┐)
 *   fence4 = T east (├)        fence6 = T west (┤)
 *   fence8 = T north (┴)       fence2 = T south (┬)
 *
 * Image lookup is routed through {@link resources.presentation.image.ObjectImageLoader}
 * via the {@code "fence_v"} name prefix (flat-folder hook), so this class only
 * owns the filename, not the path.
 */
public final class FenceVariants {

    private static final String[] TABLE = new String[16];

    static {
        TABLE[0]  = "fence0";       // ----  isolated   → vertical bar placeholder
        TABLE[1]  = "fence0";       // N---  endpost N   → vertical bar placeholder
        TABLE[2]  = "fence!";       // -E--  endpost E   → horizontal bar placeholder
        TABLE[3]  = "fence7";       // NE--  corner NE (└)
        TABLE[4]  = "fence0";       // --S-  endpost S   → vertical bar placeholder
        TABLE[5]  = "fence0";       // N-S-  vertical straight (│)
        TABLE[6]  = "fence1 (1)";   // -ES-  corner SE (┌)  (filename has a space)
        TABLE[7]  = "fence4";       // NES-  T-junction east (├)
        TABLE[8]  = "fence!";       // ---W  endpost W   → horizontal bar placeholder
        TABLE[9]  = "fence9";       // N--W  corner NW (┘)
        TABLE[10] = "fence!";       // -E-W  horizontal straight (─)
        TABLE[11] = "fence8";       // NE-W  T-junction north (┴)
        TABLE[12] = "fence3";       // --SW  corner SW (┐)
        TABLE[13] = "fence6";       // N-SW  T-junction west (┤)
        TABLE[14] = "fence2";       // -ESW  T-junction south (┬)
        TABLE[15] = "fence5";       // NESW  4-way cross (┼)
    }

    public static String fileForMask(int mask4bit) {
        return TABLE[mask4bit & 0xF];
    }

    private FenceVariants() {}
}
