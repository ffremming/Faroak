package resources.domain.object;

/**
 * 16-entry lookup mapping a stone wall's 4-bit N/E/S/W connection mask to the
 * basename of a PNG inside {@code resources/images/objects/structures/walls/stone_walls/}.
 *
 * Bit encoding (matches {@link StoneWall#BIT_NORTH} etc.): N=1, E=2, S=4, W=8.
 *
 * Each mask {@code m} maps to its own dedicated tile {@code stone_wall_<m>.png},
 * so the lookup is the identity: {@code TABLE[m] == "stone_wall_" + m}. There is
 * a purpose-built sprite for all 16 cases — the isolated block, the four
 * single-direction stubs, the two straights, the four corners, the four
 * T-junctions, and the cross.
 *
 * The 16 tiles are composed procedurally (see the stone-wall art pipeline) from
 * two reusable base sprites: a central stone block hub and a single straight
 * wall segment. For each mask, the four cardinal half-wall "arms" whose bits are
 * set are composited around the hub, so every tile shares identical block and
 * wall pixels and therefore lines up seamlessly with its neighbours. This is the
 * same technique as {@link FenceVariants}.
 *
 * Image lookup is routed through {@link resources.presentation.image.ObjectImageLoader}
 * via the {@code "stone_wall_v"} name prefix (flat-folder hook), so this class
 * only owns the filename, not the path.
 */
public final class StoneWallVariants {

    private static final String[] TABLE = new String[16];

    static {
        for (int mask = 0; mask < TABLE.length; mask++) {
            TABLE[mask] = "stone_wall_" + mask;
        }
    }

    public static String fileForMask(int mask4bit) {
        return TABLE[mask4bit & 0xF];
    }

    private StoneWallVariants() {}
}
