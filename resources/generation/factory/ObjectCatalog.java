package resources.generation.factory;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry of every world-object kind the generator can place.
 *
 * Each entry pairs an asset name (folder under resources/images/objects/) with
 * a sprite size, hitbox footprint, and solidity flag. All sizing for placed
 * objects lives here — biomes only reference the catalog by name, and
 * {@link ObjectFactory} reads the spec to instantiate a GameObject.
 *
 * Sizes are tuned around tile = 64px:
 *   - Trees stand ~1.7–2.2 tiles tall (taller than the player) with a narrow
 *     base-only hitbox so the canopy overlaps neighbours without blocking.
 *   - Bushes/shrubs are shorter than the player so they read as ground cover.
 *   - Driftwood and stones are wide-ish and short, hitbox close to the sprite.
 *
 * The hitbox is anchored at the BASE of the sprite (bottom-centred) by default,
 * so the trunk of a tree blocks the player while the canopy can overlap freely.
 * Objects that are already roughly square (driftwood, ground decor) override
 * the offset to centre vertically as well.
 */
public final class ObjectCatalog {

    public static final class ObjectSpec {
        public final String  name;
        public final int     width;
        public final int     height;
        public final int     hitBoxWidth;
        public final int     hitBoxHeight;
        public final int     hitBoxOffsetX;
        public final int     hitBoxOffsetY;
        public final boolean solid;

        public ObjectSpec(String name, int width, int height,
                          int hitBoxWidth, int hitBoxHeight,
                          int hitBoxOffsetX, int hitBoxOffsetY, boolean solid) {
            this.name          = name;
            this.width         = width;
            this.height        = height;
            this.hitBoxWidth   = hitBoxWidth;
            this.hitBoxHeight  = hitBoxHeight;
            this.hitBoxOffsetX = hitBoxOffsetX;
            this.hitBoxOffsetY = hitBoxOffsetY;
            this.solid         = solid;
        }
    }

    private static final int T = 64;

    private static final Map<String, ObjectSpec> SPECS = new HashMap<>();

    static {
        // Trees — taller than the player (player ≈ 1 tile). Hitbox is a narrow
        // band at the trunk base so canopies can overlap freely.
        registerBaseAnchored("oak_M",    (int)(T * 1.4), (int)(T * 2.2), T / 2, T / 3, true);
        registerBaseAnchored("birch_M",  (int)(T * 1.3), (int)(T * 2.1), T / 2, T / 3, true);
        registerBaseAnchored("spruce_M", (int)(T * 1.4), (int)(T * 2.3), T / 2, T / 3, true);
        registerBaseAnchored("palm_M",   (int)(T * 1.5), (int)(T * 2.2), T / 2, T / 3, true);

        // Bushes / shrubs — shorter than the player, hitbox nearly full sprite.
        registerBaseAnchored("shrub_M",        (int)(T * 1.1), (int)(T * 0.9), T - 16, T - 24, false);
        registerBaseAnchored("bushTree",       (int)(T * 1.2), T,              T - 16, T - 20, false);
        registerBaseAnchored("desert_shrub_S", (int)(T * 0.6), (int)(T * 0.6), T / 2,  T / 2,  false);

        // Ground decor — small, non-solid. Centre the hitbox vertically too so
        // it sits on the tile, not at the bottom edge of a half-empty sprite.
        registerCentered("wildGrass",      T, T, T, T, false);

        // Rocks — solid obstacles, modest height; base-anchored so the player
        // can walk along the back/top edge instead of bonking on empty pixels.
        registerBaseAnchored("stone",      (int)(T * 0.9), (int)(T * 0.8), T - 16, T - 20, true);

        // Driftwood — non-solid beach debris, sprite is roughly square so
        // centre the hitbox.
        registerCentered("driftwood",      (int)(T * 1.0), (int)(T * 0.5), T - 16, T - 28, false);
    }

    public static ObjectSpec get(String name) {
        ObjectSpec s = SPECS.get(name);
        if (s == null) throw new IllegalArgumentException("No ObjectSpec for: " + name);
        return s;
    }

    /** Hitbox centred horizontally, anchored to the bottom of the sprite. */
    private static void registerBaseAnchored(String name, int width, int height,
                                             int hitBoxWidth, int hitBoxHeight, boolean solid) {
        int offX = (width  - hitBoxWidth)  / 2;
        int offY =  height - hitBoxHeight;
        SPECS.put(name, new ObjectSpec(name, width, height,
                hitBoxWidth, hitBoxHeight, offX, offY, solid));
    }

    /** Hitbox centred horizontally and vertically. */
    private static void registerCentered(String name, int width, int height,
                                         int hitBoxWidth, int hitBoxHeight, boolean solid) {
        int offX = (width  - hitBoxWidth)  / 2;
        int offY = (height - hitBoxHeight) / 2;
        SPECS.put(name, new ObjectSpec(name, width, height,
                hitBoxWidth, hitBoxHeight, offX, offY, solid));
    }

    private ObjectCatalog() {}
}
