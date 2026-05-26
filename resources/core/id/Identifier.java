package resources.core.id;

import java.util.Objects;

/**
 * Namespaced string key used for registry lookups, save files, network sync.
 * Format: {@code namespace:path} (e.g. {@code core:oak_tree}, {@code mod:fence}).
 *
 * Identifiers are value objects — immutable, hashable, comparable by content.
 */
public final class Identifier implements Comparable<Identifier> {

    public static final String DEFAULT_NAMESPACE = "core";

    private final String namespace;
    private final String path;

    public Identifier(String namespace, String path) {
        this.namespace = require(namespace, "namespace");
        this.path = require(path, "path");
    }

    /** Convenience for the default namespace. */
    public static Identifier of(String path) {
        return new Identifier(DEFAULT_NAMESPACE, path);
    }

    public static Identifier parse(String full) {
        int idx = full.indexOf(':');
        if (idx < 0) return of(full);
        return new Identifier(full.substring(0, idx), full.substring(idx + 1));
    }

    public String namespace() { return namespace; }
    public String path()      { return path; }

    @Override public String toString() { return namespace + ":" + path; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Identifier)) return false;
        Identifier i = (Identifier) o;
        return namespace.equals(i.namespace) && path.equals(i.path);
    }

    @Override public int hashCode() { return Objects.hash(namespace, path); }

    @Override public int compareTo(Identifier o) {
        int n = namespace.compareTo(o.namespace);
        return n != 0 ? n : path.compareTo(o.path);
    }

    private static String require(String s, String label) {
        if (s == null || s.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return s;
    }
}
