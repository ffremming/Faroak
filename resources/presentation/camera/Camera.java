package resources.presentation.camera;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.PrimitiveEntity;
import resources.domain.player.Moveable;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.presentation.lighting.LightingPass;

/**
 * Viewport + follow target. Owns nothing about how the scene is painted —
 * it delegates to {@link CameraSceneRenderer} (main pass) and
 * {@link CameraDebugOverlay} (debug pass).
 *
 * Refactored to keep this class focused on camera state (position, follow,
 * viewport, FPS observation) and the high-level draw entry point. Per-element
 * rendering lives in the renderers.
 */
public class Camera extends PrimitiveEntity {

    private static final Font OVERLAY_FONT = new Font("Arial", Font.PLAIN, 7);

    public  int  FPS = 60;
    public  long splitTime    = 1000000000L / FPS;
    public  long nextDrawTime = System.nanoTime() + splitTime;

    public boolean testData = false;

    public HitBox visibilityArea;

    private BaseEntity followed;
    // BaseEntity (not GameObject) so non-GameObject placeables — boats — can
    // also show a ghost preview while being aimed.
    private BaseEntity previewObject;
    private boolean previewValid = true;

    private final ArrayList<String> backEndData = new ArrayList<>();
    private int  observedFPS;
    private long observedSortTime;
    private long observedChunkUpdateTime;

    private final CameraSceneRenderer renderer;
    private final CameraDebugOverlay  debug;
    private final LightingPass        lighting;

    public Camera(GamePanel panel, String name, int worldX, int worldY, short width, short height) {
        super(panel, name, worldX, worldY, (short) 50, (short) 50);
        this.width  = width;
        this.height = height;
        this.visibilityArea = new HitBox(0, 0, width + 500, height + 500);
        this.renderer = new CameraSceneRenderer(panel, this);
        this.debug    = new CameraDebugOverlay(panel, this);
        this.lighting = new LightingPass(panel.lighting(), panel.clock());
        follow(panel.player);
    }

    public Camera(GamePanel panel, String name) {
        super(panel, name);
        worldX = 0;
        worldY = 0;
        width  = (short) panel.screenWidth;
        height = (short) panel.screenHeight;
        hitBox = new HitBox(this);
        this.visibilityArea = new HitBox(0, 0, width + 500, height + 500);
        this.renderer = new CameraSceneRenderer(panel, this);
        this.debug    = new CameraDebugOverlay(panel, this);
        this.lighting = new LightingPass(panel.lighting(), panel.clock());
    }

    // ---- entry point ----

    /**
     * Graphics2D overload — overrides BaseEntity.draw(Graphics2D), which is a
     * no-op stub. Without this, callers that pass a Graphics2D directly (e.g.
     * the headless test probes via BufferedImage.createGraphics()) would
     * dispatch to the empty parent method and render nothing — a footgun
     * that masked render-output bugs for months. Delegates to the canonical
     * Graphics path.
     */
    @Override
    public void draw(Graphics2D g2) {
        draw((Graphics) g2);
    }

    public void draw(Graphics g) {
        hitBox.updateCoords();
        center(followed);

        long startTime = System.nanoTime();

        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(OVERLAY_FONT);
        // Pixel-art sprites: nearest-neighbour beats the default bilinear (no
        // blur on upscale + faster blits). AA off on the bulk scene pass since
        // every sprite is axis-aligned and pre-pixeled.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_SPEED);

        renderer.drawScene(g2);
        if (previewObject != null) {
            renderer.drawRelative(g2, previewObject);
            if (!previewValid) renderer.drawInvalidPreviewOverlay(g2, previewObject);
        }
        lighting.apply(g2, this, panel.getWidth(), panel.getHeight());
        panel.userInterface.draw(g2);

        long endTime = System.nanoTime();
        drawDebug(g2, startTime, endTime);
    }

    private void drawDebug(Graphics2D g2, long startTime, long endTime) {
        if (testData) {
            debug.drawChunks(g2);
            debug.writeHoveredInformation(g2);
            recordFrameStats(startTime, endTime);
            debug.drawBackEndData(g2, backEndData);
        } else {
            backEndData.clear();
        }
        debug.drawHoveredEntityOutline(g2);
    }

    private void recordFrameStats(long startTime, long endTime) {
        long durationMs = (endTime - startTime) / 1_000_000L;
        addbackendPrintData("drawtime ms: " + durationMs);
        addbackendPrintData("FPS: " + observedFPS);
        addbackendPrintData("sort time: " + (observedSortTime / 1000) + " us");
        addbackendPrintData("chunk update time: " + (observedChunkUpdateTime / 1_000_000) + " ms");
    }

    // ---- viewport + follow ----

    public void follow(BaseEntity entity) { this.followed = entity; }
    public BaseEntity getFollowed()       { return followed; }
    public void setFollowed(BaseEntity e) { this.followed = e; }

    private void center(BaseEntity entity) {
        if (entity == null) {
            throw new NullPointerException("followed is null in Camera");
        }
        visibilityArea.centerAtPosition(entity.getPoint());
        worldX = (int) entity.getWorldX() - width / 2;
        worldY = (int) entity.getWorldY() - height / 2;
    }

    public void moveX(int v) { worldX += v; }
    public void moveY(int v) { worldY += v; }
    public void setWidth(int i)  { width  = (short) i; }
    public void setHeight(int i) { height = (short) i; }

    /**
     * Rectangle (world-space) used for visibility culling. Sized from
     * {@code worldX,worldY,width,height} so it actually matches what the
     * renderer can paint — the inherited {@link #getHitBox()} is sized off the
     * 50×50 stub used at Camera construction and would only catch tiles right
     * under the camera origin. Includes:
     *   - a one-tile margin in every direction (prevents partial sprites at
     *     the edge from popping in/out between frames)
     *   - extra padding in the followed entity's movement direction (when the
     *     player moves right we briefly need to include tiles a bit further
     *     right than where the camera currently sits, otherwise the leading
     *     edge of the screen is unpainted for a frame).
     */
    public HitBox cullBounds() {
        int tile = panel.tileSize;
        int padX = tile;
        int padY = tile;
        int biasX = 0, biasY = 0;
        if (followed instanceof Moveable) {
            Vector v = ((Moveable) followed).getVelocity();
            int bias = tile;
            if (v.x >  0) biasX =  bias;
            if (v.x <  0) biasX = -bias;
            if (v.y >  0) biasY =  bias;
            if (v.y <  0) biasY = -bias;
        }
        int x = (int) worldX - padX + Math.min(0, biasX);
        int y = (int) worldY - padY + Math.min(0, biasY);
        int w = width  + padX * 2 + Math.abs(biasX);
        int h = height + padY * 2 + Math.abs(biasY);
        return new HitBox(x, y, w, h);
    }

    public void toggleTestData() { testData = !testData; }

    // ---- debug data input/output ----

    public void addbackendPrintData(String line) { backEndData.add(line); }

    public int  getObservedFPS()             { return observedFPS; }
    public void setObservedFPS(int v)        { this.observedFPS = v; }
    public long getObservedSortTime()        { return observedSortTime; }
    public void setObservedSortTime(long v)  { this.observedSortTime = v; }
    public long getObservedChunkUpdateTime() { return observedChunkUpdateTime; }
    public void setObservedChunkUpdateTime(long v) { this.observedChunkUpdateTime = v; }

    public void setPreviewObject(BaseEntity o) { previewObject = o; }
    public void setPreviewValid(boolean v)     { this.previewValid = v; }
    public boolean isPreviewValid()            { return previewValid; }

    /** Bridge so other renderers (e.g. UI) can paint an entity in camera-space. */
    public void drawRelative(Graphics2D g2, BaseEntity entity) {
        renderer.drawRelative(g2, entity);
    }
}
