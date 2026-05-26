package resources.presentation.camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.player.Moveable;
import resources.geometry.HitBox;
import resources.world.Chunk;

/**
 * All debug + diagnostic visuals for a {@link Camera}: chunk outlines, hitboxes,
 * coordinate labels, hovered-entity info, back-end performance data.
 *
 * Lives separately so the production render path stays small and the overlay
 * can be toggled, repurposed, or replaced wholesale.
 */
public final class CameraDebugOverlay {

    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 16);

    private final GamePanel panel;
    private final Camera camera;

    public CameraDebugOverlay(GamePanel panel, Camera camera) {
        this.panel = panel;
        this.camera = camera;
    }

    /** Per-entity debug visuals — only call if testData is on. */
    public void drawEntityDebug(Graphics2D g2, ArrayList<BaseEntity> tiles, ArrayList<Entity> entities) {
        for (BaseEntity tile : tiles)     { drawHitBox(g2, tile);   drawCoords(g2, tile); }
        for (Entity entity : entities)    { drawHitBox(g2, entity); drawCoords(g2, entity); }
    }

    public void drawChunks(Graphics2D g2) {
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.WHITE);
        int camX = (int) camera.getWorldX();
        int camY = (int) camera.getWorldY();
        for (Chunk chunk : new ArrayList<>(panel.world.getChunks())) {
            int x = (int) (chunk.getWorldX() - camX);
            int y = (int) (chunk.getWorldY() - camY);
            g2.drawRect(x, y, (int) chunk.getWidth(), (int) chunk.getHeight());
        }
    }

    private void drawHitBox(Graphics2D g2, BaseEntity entity) {
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        HitBox hb = entity.getHitBox();
        int camX = (int) camera.getWorldX();
        int camY = (int) camera.getWorldY();
        g2.drawRect(hb.x - camX, hb.y - camY, hb.width, hb.height);

        if (entity instanceof Moveable) {
            Rectangle infront = ((Moveable) entity).getHitboxInfront();
            g2.drawRect(infront.x - camX, infront.y - camY, infront.width, infront.height);
        }
    }

    private void drawCoords(Graphics2D g2, BaseEntity entity) {
        g2.setColor(Color.RED);
        int x = (int) (entity.getWorldX() - camera.getWorldX());
        int y = (int) (entity.getWorldY() - camera.getWorldY() + 30);
        g2.drawString("x: " + entity.getWorldX() + ", y: " + entity.getWorldY(), x, y);
    }

    public void drawHoveredEntityOutline(Graphics2D g2) {
        BaseEntity hovered = panel.world.getHoveredEntity();
        if (hovered == null) return;
        int x = (int) (hovered.getWorldX() - camera.getWorldX());
        int y = (int) (hovered.getWorldY() - camera.getWorldY());
        if (hovered.getImages().isEmpty()) return;
        g2.drawImage(panel.imageContainer.getOutline(hovered.getImages().get(0)),
            x, y, hovered.getWidth(), hovered.getHeight(), null);
    }

    public void writeHoveredInformation(Graphics g2) {
        BaseEntity hovered = panel.world.getHoveredEntity();
        if (hovered == null) return;
        g2.setColor(Color.WHITE);
        g2.setFont(LABEL_FONT);
        int y = 20;
        for (String line : hovered.toString().split("\n")) {
            g2.drawString(line, panel.screenWidth - 150, y);
            y += 25;
        }
    }

    public void drawBackEndData(Graphics g2, ArrayList<String> lines) {
        g2.setColor(Color.WHITE);
        g2.setFont(LABEL_FONT);
        int y = 20;
        for (String line : new ArrayList<>(lines)) {
            g2.drawString(line, 20, y);
            y += 25;
        }
        lines.clear();
    }
}
