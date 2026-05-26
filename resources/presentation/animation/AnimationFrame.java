package resources.presentation.animation;

import java.awt.image.BufferedImage;

/**
 * One frame of an {@link AnimationClip}: the image to show plus how long it
 * stays on screen (in simulation ticks).
 *
 * Immutable value object — clips share frame instances across users without
 * worrying about mutation.
 */
public final class AnimationFrame {

    private final BufferedImage image;
    private final int durationTicks;

    public AnimationFrame(BufferedImage image, int durationTicks) {
        if (durationTicks <= 0) throw new IllegalArgumentException("durationTicks must be positive");
        this.image = image;
        this.durationTicks = durationTicks;
    }

    public BufferedImage image()        { return image; }
    public int           durationTicks(){ return durationTicks; }
}
