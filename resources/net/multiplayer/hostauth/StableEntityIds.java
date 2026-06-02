package resources.net.multiplayer.hostauth;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Assigns stable {@code long} ids to engine entity instances.
 *
 * <p>The real game engine identifies entities by object identity, but the
 * multiplayer protocol needs stable longs. This registry bridges the two: the
 * same instance always maps to the same id, distinct instances get distinct ids,
 * and a {@link #forget}-ten instance is reassigned a fresh id if it re-registers.
 *
 * <p>Used host-side to serialize the authoritative world into snapshot payloads
 * and to resolve incoming entity-targeted commands back to engine instances.
 */
public final class StableEntityIds {

    private final Map<Object, Long> byEntity = new IdentityHashMap<>();
    private final Map<Long, Object> byId = new HashMap<>();
    private long next = 1L;

    /** Stable id for an entity instance, allocating one on first sight. 0 for null. */
    public synchronized long idFor(Object entity) {
        if (entity == null) return 0L;
        Long existing = byEntity.get(entity);
        if (existing != null) return existing;
        long id = next++;
        byEntity.put(entity, id);
        byId.put(id, entity);
        return id;
    }

    /** The entity previously registered under {@code id}, or null if unknown/forgotten. */
    public synchronized Object entityFor(long id) {
        return byId.get(id);
    }

    /** Drop an entity's mapping (e.g. when it leaves the world) so its id is not reused. */
    public synchronized void forget(Object entity) {
        if (entity == null) return;
        Long id = byEntity.remove(entity);
        if (id != null) byId.remove(id);
    }
}
