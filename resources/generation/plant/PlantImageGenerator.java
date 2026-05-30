package resources.generation.plant;

import java.awt.image.BufferedImage;

/**
 * Renders a {@link PlantSpec} by stamping pre-cut pixel-art
 * {@link PlantFeatureLibrary.Feature features} onto a transparent canvas.
 *
 * Every paint call used nearest-neighbor interpolation so the source pixels
 * stay sharp — no antialiasing, no bilinear blur. Foliage features can be
 * tinted via {@link PlantSpec.Tint}; trunk features are never tinted.
 *
 * <p>The procedural plant generator is disabled: plants are now loaded from the
 * hand-authored {@code plants_green.png} spritesheet, so {@link #generate} only
 * throws. The retained-for-reference renderer was removed; the disabled state is
 * documented on {@link #generate}.
 */
public final class PlantImageGenerator {

    /** One world tile in pixels — matches GamePanel.tileSize. */
    public static final int TILE_PX = 64;

    private PlantImageGenerator() {}

    public static BufferedImage generate(PlantSpec spec) {
        // The procedural plant pipeline is retired: plants are loaded from the
        // hand-authored spritesheet (plants_green.png). This path must never run
        // during the game — it would render/write images at runtime. Guard it so
        // an accidental re-wire fails loudly instead of silently producing PNGs.
        throw new UnsupportedOperationException(
            "Procedural plant generation is disabled. Use the plants_green.png "
          + "spritesheet via PlantCatalog/ObjectCatalog instead.");
    }
}
