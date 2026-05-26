package resources.presentation.lighting;

import java.awt.Color;

import resources.domain.entity.BaseEntity;

/**
 * One emitter in the {@link LightField}. Position is read live from the host
 * entity so the light follows it (a player-carried torch moves with the
 * player without bookkeeping).
 *
 * Immutable apart from the host reference; share freely between systems.
 */
public final class LightSource {

    private final BaseEntity host;
    private final int   radius;
    private final float intensity;
    private final Color color;

    public LightSource(BaseEntity host, int radius, float intensity, Color color) {
        this.host      = host;
        this.radius    = radius;
        this.intensity = intensity;
        this.color     = color;
    }

    public BaseEntity host()        { return host; }
    public int        radius()      { return radius; }
    public float      intensity()   { return intensity; }
    public Color      color()       { return color; }

    public int worldCenterX() { return (int) host.getWorldX() + host.getWidth()  / 2; }
    public int worldCenterY() { return (int) host.getWorldY() + host.getHeight() / 2; }
}
