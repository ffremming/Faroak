package resources.presentation.image;

import java.util.function.Supplier;

/**
 * Tiny thread-safe lazy holder for an expensive-to-build value (typically a
 * decoded/derived sprite list). Replaces the hand-rolled double-checked-locking
 * boilerplate that was copy-pasted across the boat combat / sprite classes.
 *
 * The {@code loader} is invoked at most once, on the first {@link #get()} call
 * that observes a {@code null} value, under a per-instance lock. The volatile
 * field gives the same publish/visibility guarantees as the original
 * {@code volatile FIELD} statics, so callers see a fully constructed value.
 */
public final class LazyImageCache<T> {

    private volatile T value;
    private final Supplier<T> loader;

    public LazyImageCache(Supplier<T> loader) {
        this.loader = loader;
    }

    public T get() {
        T v = value;
        if (v != null) return v;
        synchronized (this) {
            if (value != null) return value;
            value = loader.get();
            return value;
        }
    }
}
