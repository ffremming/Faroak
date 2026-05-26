package resources.presentation.animation;

import resources.core.registry.Registry;

/**
 * Named registry of {@link AnimationClip}s. Lets behaviours/components ask for
 * animations by {@link resources.core.id.Identifier} instead of holding direct
 * references — clips can be replaced or hot-swapped at boot without ripping
 * through call sites.
 *
 * Populated at boot (later: from {@code Animations.bootstrap(library)}); treat
 * as effectively immutable afterwards.
 */
public final class AnimationLibrary extends Registry<AnimationClip> {

    public AnimationLibrary() {
        super("animations");
    }
}
