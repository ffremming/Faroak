package resources.presentation.camera;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import java.awt.FontMetrics;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.entity.component.AnimationComponent;
import resources.domain.entity.component.HealthBarComponent;
import resources.domain.entity.component.LabelComponent;
import resources.domain.object.BoatRideComponent;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;
import resources.world.Chunk;

/**
 * Draws the visible scene (tiles + entities) for a {@link Camera}.
 *
 * Kept separate from Camera so the rendering strategy can evolve — culling,
 * sprite-batching, sort tweaks, light blending — without touching the camera's
 * positional state or the debug overlay.
 */
public final class CameraSceneRenderer {

    private final GamePanel panel;
    private final Camera camera;
    private final ChunkTileCache chunkBakes = new ChunkTileCache();

    public CameraSceneRenderer(GamePanel panel, Camera camera) {
        this.panel  = panel;
        this.camera = camera;
    }

    /** Expose the chunk bake so external code (tile mutations) can invalidate. */
    public ChunkTileCache chunkBakes() { return chunkBakes; }

    /** Paint visible tiles and entities relative to the camera. */
    public void drawScene(Graphics2D g2) {
        ArrayList<Entity> entities = panel.world().getVisibleEntities(camera);
        ArrayList<BaseEntity> tiles = panel.world().getVisibleTiles(camera);

        drawStaticChunkLayer(g2);

        // After the chunk bake we only need to draw tiles whose appearance
        // changes frame-to-frame (animated water etc) — the rest is already
        // baked under the entities below.
        int animatedTiles = 0;
        for (BaseEntity tile : tiles) {
            if (!tile.hasComponent(AnimationComponent.class)) continue;
            drawRelative(g2, tile);
            animatedTiles++;
        }
        for (Entity entity : entities)   drawRelative(g2, entity);

        camera.addbackendPrintData("amount entities visible: " + entities.size());
        camera.addbackendPrintData("amount tiles visible: " + tiles.size());
        camera.addbackendPrintData("animated tiles drawn: " + animatedTiles);
    }

    /**
     * Blit the pre-composited static layer of every visible chunk. The bake
     * holds all non-animated tiles for that chunk; one drawImage per chunk
     * replaces N×N per-tile draws.
     */
    private void drawStaticChunkLayer(Graphics2D g2) {
        ArrayList<Chunk> live = panel.world().getChunks();
        chunkBakes.pruneTo(live);
        HitBox cull = camera.cullBounds();
        int camX = (int) camera.getWorldX();
        int camY = (int) camera.getWorldY();
        int chunksDrawn = 0;
        for (Chunk chunk : live) {
            if (!chunk.collision(cull)) continue;
            BufferedImage bake = chunkBakes.getOrBuild(chunk);
            if (bake == null) continue;
            g2.drawImage(bake, chunk.x - camX, chunk.y - camY, null);
            chunksDrawn++;
        }
        camera.addbackendPrintData("chunk bakes drawn: " + chunksDrawn);
    }

    /** Draw one entity in camera space. */
    public void drawRelative(Graphics2D g2, BaseEntity entity) {
        // Rider is slaved to the boat's position; rendering them on top just
        // looks like a person standing on the deck. Hide them while a
        // BoatRideComponent is attached.
        if (entity.hasComponent(BoatRideComponent.class)) return;

        int camX = (int) camera.getWorldX();
        int camY = (int) camera.getWorldY();
        int x = (int) (entity.getWorldX() - camX);
        int y = (int) (entity.getWorldY() - camY);

        AnimationComponent anim = entity.getComponent(AnimationComponent.class);
        BufferedImage animFrame = anim != null ? anim.currentImage() : null;
        if (animFrame != null && !(entity instanceof Tile)) {
            g2.drawImage(animFrame, x, y, entity.getWidth(), entity.getHeight(), null);
            return;
        }

        for (BufferedImage img : entity.getImages()) {
            g2.drawImage(img, x, y, entity.getWidth(), entity.getHeight(), null);
        }

        HealthBarComponent bar = entity.getComponent(HealthBarComponent.class);
        if (bar != null && bar.visible()) {
            drawHealthBar(g2, bar, x, y, entity.getWidth());
        }

        LabelComponent label = entity.getComponent(LabelComponent.class);
        if (label != null && label.text() != null && !label.text().isEmpty()) {
            drawLabel(g2, label, x, y, entity.getWidth());
        }
    }

    /** Thin health bar just above the entity, below where a label would sit. */
    private void drawHealthBar(Graphics2D g2, HealthBarComponent bar, int x, int y, int entityW) {
        int barW = Math.max(24, entityW - 8);
        int barH = 5;
        int barX = x + (entityW - barW) / 2;
        int barY = y - 8;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(barX - 1, barY - 1, barW + 2, barH + 2, 4, 4);
        int fillW = (int) Math.round(barW * Math.max(0.0, Math.min(1.0, bar.fraction())));
        double f = bar.fraction();
        Color fill = (f > 0.5) ? new Color(80, 200, 90)
                   : (f > 0.25) ? new Color(220, 190, 60)
                   : new Color(210, 70, 60);
        g2.setColor(fill);
        g2.fillRoundRect(barX, barY, fillW, barH, 3, 3);
    }

    /** Pill-shaped text label sitting just above the entity's top edge. */
    private void drawLabel(Graphics2D g2, LabelComponent label, int x, int y, int entityW) {
        FontMetrics fm = g2.getFontMetrics();
        String text = label.text();
        int textW = fm.stringWidth(text);
        int textH = fm.getHeight();
        int padX  = 4;
        int padY  = 2;
        int boxW  = textW + padX * 2;
        int boxH  = textH + padY;
        int boxX  = x + (entityW - boxW) / 2;
        int boxY  = y - boxH - 2;
        g2.setColor(label.background());
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 6, 6);
        g2.setColor(label.foreground());
        g2.drawString(text, boxX + padX, boxY + textH - padY);
    }

    /**
     * Tint the bounding rectangle of a placement-preview entity red to signal
     * an invalid placement. Drawn over the existing translucent preview sprite,
     * so the player still sees what they're trying to place but knows it can't
     * land there.
     */
    public void drawInvalidPreviewOverlay(Graphics2D g2, BaseEntity entity) {
        int camX = (int) camera.getWorldX();
        int camY = (int) camera.getWorldY();
        int x = (int) (entity.getWorldX() - camX);
        int y = (int) (entity.getWorldY() - camY);
        g2.setColor(INVALID_TINT);
        g2.fillRect(x, y, entity.getWidth(), entity.getHeight());
        g2.setColor(INVALID_BORDER);
        g2.drawRect(x, y, entity.getWidth(), entity.getHeight());
    }

    private static final Color INVALID_TINT   = new Color(255, 60, 60, 90);
    private static final Color INVALID_BORDER = new Color(255, 30, 30, 200);

}
