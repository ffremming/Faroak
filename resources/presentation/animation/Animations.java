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

    public static final Identifier OCEAN_WAVES = Identifier.of("tile/ocean_waves");

    private Animations() {}

    /** Register all canonical clips into {@code library}. Idempotent for tests. */
    public static void bootstrap(AnimationLibrary library, ImageContainer images) {
        if (library.contains(OCEAN_WAVES)) return;
        library.register(OCEAN_WAVES, oceanWaves(images));
    }

    /** 3-frame wave loop. 60 sim-ticks/frame ≈ 1 second/frame, matching the legacy cadence. */
    private static AnimationClip oceanWaves(ImageContainer images) {
        return new AnimationClip(OCEAN_WAVES, true,
            new AnimationFrame(images.getTileImage("ocean0"), 60),
            new AnimationFrame(images.getTileImage("ocean1"), 60),
            new AnimationFrame(images.getTileImage("ocean2"), 60));
    }
}
