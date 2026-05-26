package resources.core.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import resources.core.id.Identifier;

/**
 * Generic typed registry keyed by {@link Identifier}. Insertion-ordered so iteration
 * is deterministic for save formats, debug UIs, and tests.
 *
 * Used as the canonical store for ItemType, TileType, EntityType, BiomeType, AIBehavior,
 * Dimension, etc. Subclasses can layer validation on top (e.g. {@code ItemRegistry}).
 *
 * Thread-safety: NOT thread-safe; intended to be populated during boot and then
 * treated as effectively immutable. A wrapper can add synchronisation if needed.
 */
public class Registry<T> {

    private final String name;
    private final Map<Identifier, T> entries = new LinkedHashMap<>();

    public Registry(String name) {
        this.name = name;
    }

    /** Register a new entry. Fails fast on duplicate keys to surface ID collisions. */
    public T register(Identifier id, T value) {
        if (entries.containsKey(id)) {
            throw new IllegalStateException(name + " already contains " + id);
        }
        entries.put(id, value);
        return value;
    }

    public Optional<T> get(Identifier id) {
        return Optional.ofNullable(entries.get(id));
    }

    public T require(Identifier id) {
        T value = entries.get(id);
        if (value == null) {
            throw new IllegalArgumentException(name + " has no entry for " + id);
        }
        return value;
    }

    public boolean contains(Identifier id) { return entries.containsKey(id); }

    public Collection<T> values() { return Collections.unmodifiableCollection(entries.values()); }

    public Collection<Identifier> ids() { return Collections.unmodifiableCollection(entries.keySet()); }

    public int size() { return entries.size(); }

    public String name() { return name; }
}
