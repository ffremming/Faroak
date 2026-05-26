package resources.domain.entity.component;

import java.awt.Color;

import resources.domain.entity.BaseEntity;
import resources.presentation.lighting.LightField;
import resources.presentation.lighting.LightSource;

/**
 * Attaches a {@link LightSource} to its host entity for the lifetime of the
 * component. Registers with the {@link LightField} on attach and unregisters
 * on detach — no manual bookkeeping at the entity site.
 *
 * Position follows the host automatically (LightSource reads it live), so the
 * same component works for a placed torch and a player-carried torch alike.
 */
public final class LightSourceComponent implements EntityComponent {

    private final int   radius;
    private final float intensity;
    private final Color color;

    private LightSource source;
    private LightField  field;

    public LightSourceComponent(int radius, float intensity, Color color) {
        this.radius    = radius;
        this.intensity = intensity;
        this.color     = color;
    }

    @Override
    public void onAttach(BaseEntity owner) {
        this.field  = owner.panel.lighting();
        this.source = new LightSource(owner, radius, intensity, color);
        field.add(source);
    }

    @Override
    public void onDetach(BaseEntity owner) {
        if (field != null) field.remove(source);
        source = null;
        field  = null;
    }

    public LightSource source() { return source; }
}
