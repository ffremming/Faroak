package resources.domain.entity.component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import resources.domain.entity.BaseEntity;

/**
 * Component container owned by a single {@link BaseEntity}. Lookup is by exact
 * concrete class — fast (hash map) and explicit.
 *
 * Iteration order matches insertion order, so a tick pass over Tickable
 * components is deterministic.
 *
 * Kept as a small standalone collaborator (rather than inlining the map on
 * BaseEntity) so the container's behaviour can evolve — type-hierarchy lookup,
 * pooling, event hooks — without touching every entity subclass.
 */
public final class ComponentBag {

    private final BaseEntity owner;
    private final Map<Class<? extends EntityComponent>, EntityComponent> byType = new LinkedHashMap<>();

    public ComponentBag(BaseEntity owner) {
        this.owner = owner;
    }

    /** Attach a component. Returns the same instance for fluent use. */
    public <T extends EntityComponent> T add(T component) {
        Class<? extends EntityComponent> key = component.getClass();
        if (byType.containsKey(key)) {
            throw new IllegalStateException(owner + " already has " + key.getSimpleName());
        }
        byType.put(key, component);
        component.onAttach(owner);
        return component;
    }

    /** @return the component of the given exact class, or {@code null}. */
    @SuppressWarnings("unchecked")
    public <T extends EntityComponent> T get(Class<T> type) {
        return (T) byType.get(type);
    }

    public boolean has(Class<? extends EntityComponent> type) {
        return byType.containsKey(type);
    }

    /** Remove a component if present; runs {@link EntityComponent#onDetach(BaseEntity)}. */
    public void remove(Class<? extends EntityComponent> type) {
        EntityComponent c = byType.remove(type);
        if (c != null) c.onDetach(owner);
    }

    public Collection<EntityComponent> all() {
        return Collections.unmodifiableCollection(byType.values());
    }

    public int size() { return byType.size(); }
}
