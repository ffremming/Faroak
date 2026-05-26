package resources.presentation.animation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import resources.core.id.Identifier;

/**
 * Named sequence of {@link AnimationFrame}s. Held in {@link AnimationLibrary}
 * and bound to entities via {@link AnimationComponent}.
 *
 * Clips are immutable; mutable per-entity state (current frame, elapsed time)
 * lives on the component.
 */
public final class AnimationClip {

    private final Identifier id;
    private final List<AnimationFrame> frames;
    private final boolean looping;
    private final int totalTicks;

    public AnimationClip(Identifier id, boolean looping, AnimationFrame... frames) {
        this(id, looping, Arrays.asList(frames));
    }

    public AnimationClip(Identifier id, boolean looping, List<AnimationFrame> frames) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("clip " + id + " must have at least one frame");
        }
        this.id = id;
        this.looping = looping;
        this.frames = Collections.unmodifiableList(frames);
        int sum = 0;
        for (AnimationFrame f : frames) sum += f.durationTicks();
        this.totalTicks = sum;
    }

    public Identifier id()       { return id; }
    public boolean    looping()  { return looping; }
    public int        totalTicks(){ return totalTicks; }
    public int        frameCount(){ return frames.size(); }

    /** Frame for the given elapsed-tick offset within the clip. */
    public AnimationFrame frameAt(int elapsedTicks) {
        return frames.get(frameIndexAt(elapsedTicks));
    }

    /** Index of the frame active at the given elapsed-tick offset. */
    public int frameIndexAt(int elapsedTicks) {
        int t = looping ? Math.floorMod(elapsedTicks, totalTicks) : Math.min(elapsedTicks, totalTicks - 1);
        for (int i = 0; i < frames.size(); i++) {
            int dur = frames.get(i).durationTicks();
            if (t < dur) return i;
            t -= dur;
        }
        return frames.size() - 1;
    }
}
