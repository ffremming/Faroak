package resources.domain.object;

import java.awt.Color;

import resources.app.GamePanel;
import resources.domain.entity.component.LightSourceComponent;

/**
 * Non-solid 32x64 placeable that emits warm light via a
 * {@link LightSourceComponent}. The component registers itself with the
 * lighting field on attach, so attaching it ONLY for the real placed object
 * (never for the placement preview) is essential — otherwise every cursor
 * move would spawn a ghost light source.
 */
public class Torch extends GameObject {

    private static final int LIGHT_RADIUS    = 150;
    private static final float LIGHT_INTENS  = 0.9f;
    private static final Color LIGHT_COLOR   = new Color(255, 190, 110);

    private static final int W = 32;
    private static final int H = 64;

    public Torch(GamePanel panel, int worldX, int worldY) {
        super(panel, "torch", worldX, worldY,
              W, H,
              W, W,           // small footprint hitbox, used only for selection
              0, H - W,
              false);          // non-solid: walk-through
        addComponent(new LightSourceComponent(LIGHT_RADIUS, LIGHT_INTENS, LIGHT_COLOR));
    }

    @Override
    public GameObject placementCandidate(GamePanel targetPanel) {
        // Constructor attaches its own LightSourceComponent — one per placed torch.
        return new Torch(targetPanel, (int) getWorldX(), (int) getWorldY());
    }

    @Override
    public GameObject getPreviewObject(GamePanel targetPanel) {
        // Preview MUST NOT attach a light — return a plain GameObject with the
        // preview-suffixed sprite so the LightField isn't polluted by cursor moves.
        return new GameObject(targetPanel, "torch,preview",
                              (int) getWorldX(), (int) getWorldY(),
                              W, H, W, W, 0, H - W, false);
    }
}
