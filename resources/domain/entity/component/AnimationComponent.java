package resources.domain.entity.component;

import java.awt.image.BufferedImage;

import resources.core.time.GameClock;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Tickable;
import resources.presentation.animation.AnimationClip;

/**
 * Drives a sprite animation for one host entity. Holds the current
 * {@link AnimationClip} plus the per-entity playback cursor (elapsed ticks);
 * the clip itself stays shared and immutable.
 *
 * Renderers query {@link #currentImage()} each frame and may draw it in place
 * of the host's legacy image stack. Behaviour code switches animations with
 * {@link #play(AnimationClip)} (e.g. idle → walk → attack).
 */
public final class AnimationComponent implements EntityComponent, Tickable {

    private AnimationClip clip;
    private int localTicks;
    private final GameClock sharedClock;
    private int sharedClockOrigin;

    /** Free-running component; each instance maintains its own cursor. */
    public AnimationComponent(AnimationClip initial) {
        this(initial, null);
    }

    /**
     * Clock-synced component: all instances backed by the same {@link GameClock}
     * report the same frame at any tick. Use for world-wide synchronised loops
     * (water waves, sun cycle); leave free-running for per-entity playback
     * (a walk animation that pauses with the entity).
     */
    public AnimationComponent(AnimationClip initial, GameClock sharedClock) {
        this.clip        = initial;
        this.sharedClock = sharedClock;
    }

    /** Swap to a new clip and reset the cursor (for both time-source modes). */
    public void play(AnimationClip next) {
        if (next == clip) return;
        this.clip = next;
        this.localTicks = 0;
        if (sharedClock != null) this.sharedClockOrigin = (int) sharedClock.ticks();
    }

    public AnimationClip clip() { return clip; }

    public BufferedImage currentImage() {
        return clip == null ? null : clip.frameAt(elapsed()).image();
    }

    /** Index of the current frame within the active clip; -1 when no clip. */
    public int currentFrameIndex() {
        return clip == null ? -1 : clip.frameIndexAt(elapsed());
    }

    private int elapsed() {
        if (sharedClock == null) return localTicks;
        return (int) sharedClock.ticks() - sharedClockOrigin;
    }

    @Override
    public void update() {
        if (clip != null && sharedClock == null) localTicks++;
    }

    @Override
    public void onAttach(BaseEntity owner) {
        if (sharedClock != null) this.sharedClockOrigin = (int) sharedClock.ticks();
    }

    @Override
    public void onDetach(BaseEntity owner) {
        clip = null;
    }
}
