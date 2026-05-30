package resources.world;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Random;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.tile.Tile;
import resources.generation.factory.EntityFactory;
import resources.world.persistence.ChunkSerializer;
import resources.world.persistence.InMemoryChunkSerializer;

/**
 * Root of one dimension's chunk quadtree. Owns the entity factory and the
 * render distance, delegates root expansion to {@link ChunkExpander}, and
 * delegates per-chunk generation to {@link ChunkLoader} via {@link Chunk}.
 *
 * Once a dimension is moved behind the {@code Dimension} abstraction
 * (Phase 3.7), each Dimension will own its own ChunkSystem.
 */
public class ChunkSystem {

    public static final int OVERWORLD = 1;
    public static final int CAVE      = 2;
    public static final int NETHER    = 3;

    final GamePanel panel;
    final EntityFactory entityFactory;
    final int renderDistance;

    TreeNode parent;
    String ID;
    final int type;
    protected boolean generate = true;

    private final ChunkExpander expander = new ChunkExpander(this);
    private ChunkSerializer serializer = new InMemoryChunkSerializer();

    public ChunkSystem(GamePanel panel, int sizePow, int type, EntityFactory entFact) {
        this.panel         = panel;
        this.type          = type;
        this.entityFactory = entFact;
        this.renderDistance = 30 * panel.tileSize;

        setID();
        tryClearStorage();

        int span = (int) Math.pow(2, sizePow) * panel.tileSize * 16;
        int origin = -span / 2;
        this.parent = new TreeNode(this, origin, origin, span, span);
    }

    // ---- queries ----

    /** All chunks in render distance of the given world point. */
    public ArrayList<Chunk> getAllChunksInRenderDistance(Point p) {
        ArrayList<Chunk> list = new ArrayList<>();
        parent.getAllChunks(getRenderRectangle(p), list);
        return list;
    }

    /** All chunks in render distance of the given entity's centre. */
    public ArrayList<Chunk> getAllChunksInRenderDistance(BaseEntity entity) {
        int cx = (int) entity.getWorldX() + entity.getWidth()  / 2;
        int cy = (int) entity.getWorldY() - entity.getHeight() / 2;
        return getAllChunksInRenderDistance(new Point(cx, cy));
    }

    public Rectangle getRenderRectangle(Point p) {
        int minX = p.x - renderDistance;
        int minY = p.y - renderDistance;
        return new Rectangle(minX, minY, renderDistance * 2, renderDistance * 2);
    }

    /** First tile at the given world point, loading the chunk if needed. */
    public Tile getTile(Point p) {
        return parent.getTileInPoint(p);
    }

    // ---- entity mutation ----

    /**
     * Insert {@code entity} into the chunk tree. Returns {@code true} on
     * success; {@code false} if the target tile is outside the currently
     * loaded chunk bounds (caller is responsible for retrying after the
     * world expands, or rolling back any item consumption that anticipated
     * placement).
     */
    public boolean addEntity(BaseEntity entity) {
        try {
            parent.addEntity(entity);
            return true;
        } catch (OutOfChunkBounds ignored) {
            return false; // expected: target chunk not loaded yet; caller retries after world expands
        }
    }

    public boolean removeEntity(BaseEntity entity) {
        return parent.removeEntity(entity);
    }

    // ---- expansion ----

    /** Grow the root if the render area has left current bounds. */
    public void handleOutOfBounds(Point p) {
        Rectangle renderRect = getRenderRectangle(p);
        if (parent.contains(renderRect)) return;
        try {
            expander.expand(renderRect);
        } catch (OutOfChunkBounds e) {
            e.printStackTrace();
        }
    }

    // ---- persistence ----

    public ChunkSerializer serializer() { return serializer; }

    public void setSerializer(ChunkSerializer serializer) { this.serializer = serializer; }

    // ---- identity ----

    public String getID() { return ID; }

    private void setID() {
        this.ID = (type == 0) ? Integer.toString(new Random().nextInt()) : Integer.toString(type);
    }

    // ---- bootstrap helper ----

    private void tryClearStorage() {
        try { clearFile(new File("storage.txt")); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public static void clearFile(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(0);
        }
    }
}
