package resources.presentation.camera;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.PrimitiveEntity;
import resources.domain.object.GameObject;
import resources.geometry.HitBox;
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
    private GameObject previewObject;

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

    public void draw(Graphics g) {
        hitBox.updateCoords();
        center(followed);

        long startTime = System.nanoTime();

        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(OVERLAY_FONT);

        renderer.drawScene(g2);
        if (previewObject != null) renderer.drawRelative(g2, previewObject);
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

    public void toggleTestData() { testData = !testData; }

    // ---- debug data input/output ----

    public void addbackendPrintData(String line) { backEndData.add(line); }

    public int  getObservedFPS()             { return observedFPS; }
    public void setObservedFPS(int v)        { this.observedFPS = v; }
    public long getObservedSortTime()        { return observedSortTime; }
    public void setObservedSortTime(long v)  { this.observedSortTime = v; }
    public long getObservedChunkUpdateTime() { return observedChunkUpdateTime; }
    public void setObservedChunkUpdateTime(long v) { this.observedChunkUpdateTime = v; }

    public void setPreviewObject(GameObject o) { previewObject = o; }

    /** Bridge so other renderers (e.g. UI) can paint an entity in camera-space. */
    public void drawRelative(Graphics2D g2, BaseEntity entity) {
        renderer.drawRelative(g2, entity);
    }
}
