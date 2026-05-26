package resources.world;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;

/**
 * Internal quadtree node above the chunk-leaf layer. Subdivides the world into
 * four quadrants; {@link Chunk} extends this and overrides the recursion to
 * stop at leaves.
 *
 * The two constructors handle the two creation paths:
 *   - fresh build (creates four children eagerly)
 *   - expansion (re-roots with an existing child placed in one quadrant)
 */
public class TreeNode extends HitBox {

    /** Smallest leaf size in chunks per side; one quadrant of this size holds 8x8 chunks. */
    final int CHUNKSIZE = 8;

    final ChunkSystem chunkS;
    boolean generated = false;

    private final TreeNode[] children = new TreeNode[4];

    public TreeNode(ChunkSystem chunkS, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.chunkS = chunkS;
        addChildren();
    }

    /** Re-root case: one of the new node's quadrants is the existing child. */
    public TreeNode(ChunkSystem chunkS, int x, int y, int width, int height, TreeNode child) {
        super(x, y, width, height);
        this.chunkS = chunkS;
        addChildren(child);
    }

    // ---- subdivision ----

    protected void addChildren() {
        boolean atChunkLevel = width == CHUNKSIZE * chunkS.panel.tileSize;
        int halfW = width / 2, halfH = height / 2;
        children[0] = childAt(x,         y,         halfW, halfH, atChunkLevel);
        children[1] = childAt(x + halfW, y,         halfW, halfH, atChunkLevel);
        children[2] = childAt(x,         y + halfH, halfW, halfH, atChunkLevel);
        children[3] = childAt(x + halfW, y + halfH, halfW, halfH, atChunkLevel);
    }

    private TreeNode childAt(int cx, int cy, int cw, int ch, boolean asChunk) {
        return asChunk ? new Chunk(chunkS, cx, cy, cw, ch) : new TreeNode(chunkS, cx, cy, cw, ch);
    }

    protected void addChildren(TreeNode existing) {
        int existingX = existing.x, existingY = existing.y;
        int counter = 0;
        for (int newX = x; newX < x + width;  newX += width / 2) {
            for (int newY = y; newY < y + width; newY += width / 2) {
                children[counter++] = (newX == existingX && newY == existingY)
                    ? existing
                    : new TreeNode(chunkS, newX, newY, width / 2, width / 2);
            }
        }
    }

    public boolean hasChildren()       { return children[0] != null; }
    protected TreeNode[] getChildren() { return children; }

    // ---- recursion across children ----

    protected void getAllChunks(ArrayList<Chunk> list) {
        for (TreeNode c : children) c.getAllChunks(list);
    }

    protected void getAllChunks(Rectangle rect, ArrayList<Chunk> list) {
        for (TreeNode c : children) {
            if (c.contains(rect) || c.intersects(rect)) c.getAllChunks(rect, list);
        }
    }

    public void addEntity(BaseEntity entity) throws OutOfChunkBounds {
        HitBox hb = entity.getHitBox();
        for (TreeNode c : children) {
            if (c.contains(hb) || c.intersects(hb)) { c.addEntity(entity); return; }
        }
    }

    public ArrayList<BaseEntity> getEntitiesInPoint(Point point, ArrayList<BaseEntity> sink) {
        for (TreeNode c : children) {
            if (c.contains(point)) c.getEntitiesInPoint(point, sink);
        }
        return sink;
    }

    public ArrayList<Entity> getEntitiesInBound(Rectangle rect, ArrayList<Entity> sink) {
        for (TreeNode c : children) {
            if (c.contains(rect) || c.intersects(rect)) c.getEntitiesInBound(rect, sink);
        }
        return sink;
    }

    public boolean removeEntity(BaseEntity entity) {
        HitBox hb = entity.getHitBox();
        Rectangle bbox = new Rectangle(
            (int) entity.getWorldX(), (int) entity.getWorldY(),
            entity.getWidth(), entity.getHeight());
        for (TreeNode c : children) {
            if ((c.contains(hb) || c.intersects(hb) || c.intersects(bbox)) && c.removeEntity(entity)) {
                return true;
            }
        }
        return false;
    }

    public Tile getTileInPoint(Point p) {
        for (TreeNode c : children) {
            if (c.contains(p)) return c.getTileInPoint(p);
        }
        return null;
    }

    // ---- position ----

    public int getWorldX()      { return x; }
    public int getWorldY()      { return y; }
    public int getSquareMeter() { return width * height; }
}
