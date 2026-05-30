package resources.presentation.animation;

import resources.core.id.Identifier;
import resources.presentation.image.ImageContainer;

/**
 * Boot-time canonical animation clips. Keeps clip-construction (frame names,
 * timings) out of game-domain classes — tiles/entities only ask the
 * {@link AnimationLibrary} by {@link Identifier}.
 *
 * Add new clips here; consumers do not change.
 */
public final class Animations {

    public static final Identifier OCEAN_WAVES         = Identifier.of("tile/ocean_waves");
    public static final Identifier SHALLOW_WATER_WAVES = Identifier.of("tile/shallow_water_waves");
    public static final Identifier MEDIUM_WATER_WAVES  = Identifier.of("tile/medium_water_waves");

    private Animations() {}

    /** Register all canonical clips into {@code library}. Idempotent for tests. */
    public static void bootstrap(AnimationLibrary library, ImageContainer images) {
        if (!library.contains(OCEAN_WAVES))         library.register(OCEAN_WAVES,         oceanWaves(images));
        if (!library.contains(SHALLOW_WATER_WAVES)) library.register(SHALLOW_WATER_WAVES, shallowWaterWaves(images));
        if (!library.contains(MEDIUM_WATER_WAVES))  library.register(MEDIUM_WATER_WAVES,  mediumWaterWaves(images));
    }

    /** 3-frame wave loop. 30 sim-ticks/frame ≈ 0.5 second/frame. */
    private static AnimationClip oceanWaves(ImageContainer images) {
        return new AnimationClip(OCEAN_WAVES, true,
            new AnimationFrame(images.getTileImage("ocean0"), 30),
            new AnimationFrame(images.getTileImage("ocean1"), 30),
            new AnimationFrame(images.getTileImage("ocean2"), 30));
    }

    /** Same cadence as ocean, lighter sprite set used in the shore transition band. */
    private static AnimationClip shallowWaterWaves(ImageContainer images) {
        return new AnimationClip(SHALLOW_WATER_WAVES, true,
            new AnimationFrame(images.getTileImage("shallowWater0"), 30),
            new AnimationFrame(images.getTileImage("shallowWater1"), 30),
            new AnimationFrame(images.getTileImage("shallowWater2"), 30));
    }

    /** Medium-depth wave loop — same cadence, darker sprite set. */
    private static AnimationClip mediumWaterWaves(ImageContainer images) {
        return new AnimationClip(MEDIUM_WATER_WAVES, true,
            new AnimationFrame(images.getTileImage("mediumWater0"), 30),
            new AnimationFrame(images.getTileImage("mediumWater1"), 30),
            new AnimationFrame(images.getTileImage("mediumWater2"), 30));
    }
}
