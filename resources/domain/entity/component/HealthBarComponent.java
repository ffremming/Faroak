package resources.domain.entity.component;

import resources.domain.entity.BaseEntity;

/**
 * Floating health bar drawn above the host entity. Pure data carrier picked up
 * by {@link resources.presentation.camera.CameraSceneRenderer} on draw, mirroring
 * {@link LabelComponent}. Used for remote players (and any entity that wants a
 * visible health bar); update {@link #set(int, int)} from the authoritative state.
 */
public final class HealthBarComponent implements EntityComponent {

    private int current;
    private int max;
    private boolean visible = true;

    public HealthBarComponent(int current, int max) {
        set(current, max);
    }

    @Override public void onAttach(BaseEntity owner) { /* no registration */ }
    @Override public void onDetach(BaseEntity owner) { /* no registration */ }

    public void set(int current, int max) {
        this.max = Math.max(1, max);
        this.current = Math.max(0, Math.min(this.max, current));
    }

    public void setVisible(boolean visible) { this.visible = visible; }

    public int current()      { return current; }
    public int max()          { return max; }
    public boolean visible()  { return visible; }
    public double fraction()  { return (double) current / max; }
}
