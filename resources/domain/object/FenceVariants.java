package resources.domain.object;

/**
 * 16-entry lookup mapping a fence's 4-bit N/E/S/W connection mask to the
 * basename of a PNG inside {@code resources/images/objects/structures/walls/fences/}.
 *
 * Bit encoding (matches {@link Fence#BIT_NORTH} etc.): N=1, E=2, S=4, W=8.
 *
 * Each mask {@code m} maps to its own dedicated tile {@code fence_<m>.png}, so
 * the lookup is the identity: {@code TABLE[m] == "fence_" + m}. There is a
 * purpose-built sprite for all 16 cases — the isolated post, the four
 * single-direction endposts, the two straights, the four corners, the four
 * T-junctions, and the cross — with no reuse of one shape to stand in for
 * another.
 *
 * The 16 tiles are composed procedurally (see {@code tools/}/the fence art
 * pipeline) from two reusable base sprites: a central wooden post hub and a
 * single rail segment. For each mask, the four cardinal half-rail "arms" whose
 * bits are set are composited around the hub, so every tile shares identical
 * post and rail pixels and therefore lines up seamlessly with its neighbours.
 *
 * Image lookup is routed through {@link resources.presentation.image.ObjectImageLoader}
 * via the {@code "fence_v"} name prefix (flat-folder hook), so this class only
 * owns the filename, not the path. Network sync (see
 * {@code AuthoritativeGameHost.fenceVariantName}) reuses {@link #fileForMask}
 * the same way.
 */
public final class FenceVariants {

    private static final String[] TABLE = new String[16];

    static {
        for (int mask = 0; mask < TABLE.length; mask++) {
            TABLE[mask] = "fence_" + mask;
        }
    }

    public static String fileForMask(int mask4bit) {
        return TABLE[mask4bit & 0xF];
    }

    private FenceVariants() {}
}
