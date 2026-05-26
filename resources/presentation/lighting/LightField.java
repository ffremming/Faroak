package resources.presentation.lighting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Active light registry. {@link LightSourceComponent}s add/remove their sources
 * here as they attach/detach; the {@link LightingPass} reads the snapshot each
 * frame.
 *
 * Kept as a thin holder rather than ramming the list onto a god object — the
 * dimension-switching work later can hand the renderer a different field per
 * scene without touching emitters.
 */
public final class LightField {

    private final List<LightSource> sources = new ArrayList<>();

    public void add(LightSource source) {
        if (source != null) sources.add(source);
    }

    public void remove(LightSource source) {
        sources.remove(source);
    }

    public List<LightSource> sources() {
        return Collections.unmodifiableList(sources);
    }

    public int size() { return sources.size(); }
}
