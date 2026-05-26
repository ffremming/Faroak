package resources.domain.entity.component;

import java.awt.Color;

import resources.domain.entity.BaseEntity;

/**
 * Visible text floating above the host entity. Picked up by
 * {@link resources.presentation.camera.CameraSceneRenderer} on draw — pure
 * data carrier, no rendering logic here.
 *
 * Why a component rather than a Portal-only feature: anything in the world
 * (NPCs, signs, named buildings, dropped items) can want a label, and we don't
 * want each one re-implementing string-positioning math.
 */
public final class LabelComponent implements EntityComponent {

    public static final Color DEFAULT_FG = new Color(255, 240, 200);
    public static final Color DEFAULT_BG = new Color(0, 0, 0, 160);

    private String text;
    private Color  foreground;
    private Color  background;

    public LabelComponent(String text) {
        this(text, DEFAULT_FG, DEFAULT_BG);
    }

    public LabelComponent(String text, Color fg, Color bg) {
        this.text       = text;
        this.foreground = fg;
        this.background = bg;
    }

    @Override public void onAttach(BaseEntity owner) { /* no registration */ }
    @Override public void onDetach(BaseEntity owner) { /* no registration */ }

    public String text()       { return text; }
    public Color  foreground() { return foreground; }
    public Color  background() { return background; }

    public void setText(String text) { this.text = text; }
}
