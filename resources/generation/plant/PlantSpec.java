package resources.generation.plant;

/**
 * Immutable description of a procedurally generated plant. Drives
 * {@link PlantImageGenerator}.
 *
 * Sprites are no longer drawn with primitives — they're assembled by stamping
 * pixel-art {@link PlantFeatureLibrary.Feature} cut-outs. So the spec controls
 * which features to stamp ({@link Shape}, {@link Trunk}), the overall
 * footprint ({@link Size}), and an optional colour shift ({@link Tint}) for
 * dry / autumn / dark variants.
 */
public final class PlantSpec {

    public enum Shape {
        /** Tip + tiered foliage rows on top of a trunk — spruce-style. */
        CONIFER,
        /** One big foliage mass made of shrub-clump features on a trunk. */
        ROUND,
        /** Several shrub-clumps stacked at ground level, optional small stem. */
        CLUSTER,
        /** Tall thin trunk with leaf clumps fanning out at the top. */
        FROND
    }

    public enum Size {
        S(0.7, 0.7),
        M(1.1, 1.4),
        L(1.5, 2.2);
        public final double widthTiles;
        public final double heightTiles;
        Size(double w, double h) { this.widthTiles = w; this.heightTiles = h; }
    }

    public enum Trunk {
        NONE,    // pure shrub / ground cover
        THIN,    // sapling — shrub_stem feature stretched
        THICK,   // mature tree — spruce_trunk feature
        MULTI    // two-stem
    }

    /**
     * Colour shift applied to every stamped foliage feature. Trunk pixels
     * (red-dominant, dark blue) are detected and skipped so plants always
     * keep their woody trunks regardless of tint.
     *
     * Implemented as hue rotation + lightness multiply rather than per-channel
     * RGB multiply — a pure RGB multiply can't push a green pixel into orange
     * because the source green channel is already much larger than red.
     *
     * {@code hueShift} is in degrees on the standard HSB wheel
     * (0=red, 60=yellow, 120=green, ...). The source bush is around 110°, so
     * for example {@code AUTUMN} shifts +(−95)°≈25° to land on orange-rust.
     */
    public enum Tint {
        NONE  (   0, 1.00, 1.00),
        DRY   ( -45, 1.00, 1.05),  // green → olive / dry yellow
        AUTUMN( -95, 1.00, 1.05),  // green → orange / rust
        DARK  (   0, 0.75, 1.00),  // unchanged hue, darker
        SAGE  (   5, 1.20, 0.65);  // slightly cooler, paler, lower saturation
        public final double hueShift;
        public final double satMul;
        public final double valMul;
        Tint(double hueShift, double valMul, double satMul) {
            this.hueShift = hueShift; this.valMul = valMul; this.satMul = satMul;
        }
    }

    public final String name;
    public final long   seed;
    public final Size   size;
    public final Shape  shape;
    public final Trunk  trunk;
    public final Tint   tint;
    /** True if the plant blocks the player (trees yes, shrubs no). */
    public final boolean solid;

    public PlantSpec(String name, long seed,
                     Size size, Shape shape, Trunk trunk, Tint tint,
                     boolean solid) {
        this.name  = name;
        this.seed  = seed;
        this.size  = size;
        this.shape = shape;
        this.trunk = trunk;
        this.tint  = tint;
        this.solid = solid;
    }
}
