package resources.presentation.camera;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.entity.component.AnimationComponent;
import resources.domain.tile.Tile;

/**
 * Draws the visible scene (tiles + entities) for a {@link Camera}.
 *
 * Kept separate from Camera so the rendering strategy can evolve — culling,
 * sprite-batching, sort tweaks, light blending — without touching the camera's
 * positional state or the debug overlay.
 */
public final class CameraSceneRenderer {

    private static final Color SHADOW = new Color(100, 100, 150, 60);

    private final GamePanel panel;
    private final Camera camera;

    public CameraSceneRenderer(GamePanel panel, Camera camera) {
        this.panel  = panel;
        this.camera = camera;
    }

    /** Paint visible tiles and entities relative to the camera. */
    public void drawScene(Graphics2D g2) {
        ArrayList<Entity> entities = panel.world.getVisibleEntities(camera);
        ArrayList<BaseEntity> tiles = panel.world.getVisibleTiles(camera);

        for (BaseEntity tile : tiles)    drawRelative(g2, tile);
        for (Entity entity : entities)   drawRelative(g2, entity);

        camera.addbackendPrintData("amount entities visible: " + entities.size());
        camera.addbackendPrintData("amount tiles visible: " + tiles.size());
    }

    /** Draw one entity in camera space, including its shadow if non-Tile. */
    public void drawRelative(Graphics2D g2, BaseEntity entity) {
        int camX = (int) camera.getWorldX();
        int camY = (int) camera.getWorldY();
        int x = (int) (entity.getWorldX() - camX);
        int y = (int) (entity.getWorldY() - camY);

        if (!(entity instanceof Tile)) {
            drawShadow(g2, entity, camX, camY);
        }

        AnimationComponent anim = entity.getComponent(AnimationComponent.class);
        BufferedImage animFrame = anim != null ? anim.currentImage() : null;
        if (animFrame != null) {
            g2.drawImage(animFrame, x, y, entity.getWidth(), entity.getHeight(), null);
            return;
        }

        for (BufferedImage img : entity.getImages()) {
            g2.drawImage(img, x, y, entity.getWidth(), entity.getHeight(), null);
        }
    }

    private void drawShadow(Graphics2D g2, BaseEntity entity, int camX, int camY) {
        int shadowX = entity.getHitBox().x - camX - 5;
        int shadowY = entity.getHitBox().y - camY + 15;
        g2.setColor(SHADOW);
        g2.fillOval(shadowX, shadowY,
            entity.getHitBox().width + 10,
            entity.getHitBox().height);
    }
}
