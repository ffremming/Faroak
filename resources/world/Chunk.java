package resources.world;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.player.Moveable;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;

/**
 * Leaf of the chunk quadtree. Holds the tile grid and the entity bag for one
 * spatial cell of the world.
 *
 * Tile generation, entity spawning, and lifecycle state (loaded / generated /
 * counters) live here; the heavy lifting of how to generate is delegated to
 * {@link ChunkLoader} so this class stays focused on storage + access.
 */
public class Chunk extends TreeNode {

    static int amount = 0;
    static int amtLoaded = 0;
    static int amtGenerated = 0;

    public final Tile[][] tiles = new Tile[CHUNKSIZE][CHUNKSIZE];
    final ArrayList<Entity> entities = new ArrayList<>();

    private boolean loaded = false;
    private final ChunkLoader loader = new ChunkLoader(this);

    public Chunk(ChunkSystem chunkS, int x, int y, int width, int height) {
        super(chunkS, x, y, width, height);
    }

    @Override
    protected void addChildren() { amount++; }

    // ---- loading lifecycle ----

    public void load() {
        if (loaded) return;
        if (!generated && chunkS.generate) loader.initialLoad();
        else                                loader.repeatedLoad();
        loaded = true;
        amtLoaded++;
    }

    /** Currently a no-op: kept as the lifecycle hook persistence will plug into. */
    public void unLoad() { /* persistence layer attaches here later */ }

    /** Called by {@link ChunkLoader} when first-time generation completes. */
    void markGenerated() {
        generated = true;
        amtGenerated++;
    }

    public boolean isLoaded()    { return loaded; }
    public boolean isGenerated() { return generated; }

    // ---- entity / tile storage ----

    public ArrayList<Entity> getEntities() { return entities; }

    @Override
    public void addEntity(BaseEntity entity) {
        if (entity == null) throw new NullPointerException("tried adding null");
        if (entity instanceof Tile) addTile((Tile) entity);
        else                         entities.add((Entity) entity);
    }

    public void addTile(Tile tile) {
        if (tile == null) return;
        int arrayX = ((int) tile.getWorldX() - x) / 64;
        int arrayY = ((int) tile.getWorldY() - y) / 64;
        tiles[arrayY][arrayX] = tile;
    }

    @Override
    public boolean removeEntity(BaseEntity entity) {
        return entities.remove(entity);
    }

    protected ArrayList<Entity> getAllEntities() { return entities; }

    // ---- queries ----

    @Override
    public ArrayList<Entity> getEntitiesInBound(Rectangle rect, ArrayList<Entity> sink) {
        for (Entity e : entities) {
            if (rect.contains(e.getHitBox()) || rect.intersects(e.getHitBox())) sink.add(e);
        }
        return sink;
    }

    @Override
    protected void getAllChunks(Rectangle rect, ArrayList<Chunk> list) { list.add(this); }

    @Override
    protected void getAllChunks(ArrayList<Chunk> list) { list.add(this); }

    /** First tile whose hitbox contains the point, or null. */
    public Tile getTile(Point point) {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                if (tiles[i][j] != null && tiles[i][j].getHitBox().contains(point)) return tiles[i][j];
            }
        }
        return null;
    }

    /** Same as getTile but loads the chunk first if needed. */
    public Tile getTileInPoint(Point p) {
        if (!loaded) load();
        return getTile(p);
    }

    public ArrayList<Tile> getTilesCollidedWith(HitBox hitBox) {
        ArrayList<Tile> hits = new ArrayList<>();
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                if (tiles[i][j] != null && tiles[i][j].collision(hitBox)) hits.add(tiles[i][j]);
            }
        }
        return hits;
    }

    // ---- ticking ----

    /** Walk all tiles and wire each tile to its four neighbors. */
    void connectTiles() {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                if (tiles[i][j] != null) tiles[i][j].setNeighBors();
            }
        }
    }

    /** Move out any Moveable entity that has left this chunk's bounds. */
    void flush() {
        ArrayList<BaseEntity> toRemove = new ArrayList<>();
        for (BaseEntity be : entities) {
            if (be instanceof Moveable && !this.collision(be.getHitBox())) toRemove.add(be);
        }
        for (BaseEntity be : toRemove) {
            entities.remove(be);
            chunkS.addEntity(be);
        }
    }

    // ---- debug ----

    public void writeInfo() {
        System.out.println("entities:" + entities.size() + " coords: x:" + x + " y:" + y);
        System.out.println(getBounds());
        for (BaseEntity entity : entities) System.out.print("name: " + entity.getName() + ",");
        System.out.println("\n");
    }
}
