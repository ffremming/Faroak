package resources.net.multiplayer.state;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authoritative entity state with stable network identity and component data.
 */
public final class EntityState {

    private final long entityId;
    private final String entityType;
    private String dimensionId;
    private double worldX;
    private double worldY;
    private boolean removed;
    private long revision;
    private long lastChangedTick;
    private final LinkedHashMap<String, String> components = new LinkedHashMap<>();

    public EntityState(long entityId, String entityType, String dimensionId, double worldX, double worldY, long revision, long tick) {
        this.entityId = Math.max(0L, entityId);
        this.entityType = sanitize(entityType, "entity");
        this.dimensionId = TileMutationState.normalizeDimension(dimensionId);
        this.worldX = worldX;
        this.worldY = worldY;
        this.revision = Math.max(0L, revision);
        this.lastChangedTick = Math.max(0L, tick);
    }

    public long entityId() { return entityId; }
    public String entityType() { return entityType; }
    public String dimensionId() { return dimensionId; }
    public double worldX() { return worldX; }
    public double worldY() { return worldY; }
    public boolean removed() { return removed; }
    public long revision() { return revision; }
    public long lastChangedTick() { return lastChangedTick; }

    public Map<String, String> components() {
        return Collections.unmodifiableMap(components);
    }

    public String component(String key) {
        return components.get(key);
    }

    public void putComponent(String key, String value, long newRevision, long tick) {
        if (key == null || key.isBlank()) return;
        components.put(key.trim().toLowerCase(), value == null ? "" : value);
        touch(newRevision, tick);
    }

    public void moveTo(String dimensionId, double worldX, double worldY, long newRevision, long tick) {
        this.dimensionId = TileMutationState.normalizeDimension(dimensionId);
        this.worldX = worldX;
        this.worldY = worldY;
        touch(newRevision, tick);
    }

    public void markRemoved(long newRevision, long tick) {
        this.removed = true;
        touch(newRevision, tick);
    }

    public boolean changedSince(long sentTick) {
        return sentTick <= 0L || lastChangedTick > sentTick;
    }

    private void touch(long newRevision, long tick) {
        revision = Math.max(revision, newRevision);
        lastChangedTick = Math.max(0L, tick);
    }

    private static String sanitize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String value = raw.trim().toLowerCase();
        if (value.length() > 96) value = value.substring(0, 96);
        return value;
    }
}
