package resources.world;

import java.awt.Point;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.entity.Entity;
import resources.domain.inventory.Stack;
import resources.domain.tile.Tile;
import resources.generation.factory.EntityFactory;
import resources.generation.noise.ProceduralGen;
import resources.geometry.HitBox;
import resources.geometry.Vector;
import resources.presentation.camera.Camera;

/**
 * Thin coordinator for the active simulation window. Owns the
 * {@link EntityIndex}, {@link EntityVisibility}, and {@link WorldInteraction}
 * collaborators and the {@link ChunkSystem} (per dimension).
 *
 * Its only intrinsic responsibilities are the chunk-update tick, simulation /
 * animation drive, and acting as the entry point for {@link WorldRuntime}
 * queries — everything heavier delegates.
 */
public class WorkingMemory implements WorldRuntime {

    private final GamePanel panel;
    private final EntityIndex       index       = new EntityIndex();
    private ChunkSystem             chunkSystem;
    private final EntityVisibility  visibility;
    private WorldInteraction        interaction;

    public WorkingMemory(GamePanel panel) {
        this.panel = panel;
        ProceduralGen overworldGen = new ProceduralGen();
        this.chunkSystem  = new ChunkSystem(panel, 8, ChunkSystem.OVERWORLD,
                                            new EntityFactory(panel, overworldGen));
        this.visibility  = new EntityVisibility(index);
        this.interaction = new WorldInteraction(index, chunkSystem, panel);
    }

    /**
     * Swap the active chunk system. Used by the dimension switcher when the
     * player steps through a portal — flushes the index so old-dimension
     * entities/tiles are dropped, then re-runs the initial chunk load.
     */
    public void setChunkSystem(ChunkSystem newSystem) {
        setChunkSystem(newSystem, new Point(0, 0));
    }

    /**
     * Same as {@link #setChunkSystem(ChunkSystem)} but seeds the initial chunk
     * load around {@code arrival} instead of the world origin. Needed when a
     * portal drops the player far from (0,0): loading around the origin would
     * leave the player standing in a void until the per-second world.update
     * catches up.
     */
    public void setChunkSystem(ChunkSystem newSystem, Point arrival) {
        this.chunkSystem  = newSystem;
        this.interaction  = new WorldInteraction(index, chunkSystem, panel);
        index.setEntities(new ArrayList<>());
        index.setTiles(new ArrayList<>());
        index.setChunks(new ArrayList<>());
        initial(arrival);
    }

    public ChunkSystem chunkSystem() { return chunkSystem; }

    // ---- lifecycle ----

    /** First-time chunk load around world origin. */
    public void initial() {
        initial(new Point(0, 0));
    }

    /** First-time chunk load around an arbitrary world point. */
    public void initial(Point origin) {
        index.setChunks(chunkSystem.getAllChunksInRenderDistance(origin));
        for (Chunk c : index.chunks()) c.load();
        for (Chunk c : index.chunks()) c.connectTiles();
    }

    @Override
    public void update(Point worldPoint) {
        chunkSystem.handleOutOfBounds(worldPoint);
        updateChunks(worldPoint);
        updateEntities(worldPoint);
        index.setTiles(index.harvestTilesFromChunks());
    }

    private void updateChunks(Point p) {
        long startTime = System.nanoTime();
        ArrayList<Chunk> oldChunks = new ArrayList<>(index.chunks());
        index.setChunks(chunkSystem.getAllChunksInRenderDistance(p));
        unloadChunksNotIn(oldChunks, index.chunks());
        for (Chunk c : index.chunks()) { c.flush(); c.load(); }
        for (Chunk c : index.chunks())   c.connectTiles();
        recordChunkUpdateTime(startTime);
    }

    private void updateEntities(Point p) {
        ArrayList<Entity> inRange = new ArrayList<>();
        for (Chunk c : new ArrayList<>(index.chunks())) {
            c.getEntitiesInBound(chunkSystem.getRenderRectangle(p), inRange);
        }
        index.setEntities(inRange);
        sortVisibleEntities();
    }

    private void unloadChunksNotIn(ArrayList<Chunk> oldChunks, ArrayList<Chunk> newChunks) {
        for (Chunk old : oldChunks) {
            if (!newChunks.contains(old)) old.unLoad();
        }
    }

    private void sortVisibleEntities() {
        long startTime = System.nanoTime();
        if (panel.camera != null) {
            ArrayList<Entity> inRange = new ArrayList<>();
            for (Chunk c : new ArrayList<>(index.chunks())) {
                c.getEntitiesInBound(chunkSystem.getRenderRectangle(panel.camera.getHitBox().getCenter()), inRange);
            }
            index.setEntities(inRange);
        }
        index.setSortedVisible(visibility.visibleEntities(panel.camera, index.entities()));
        EntitySorter.sortByWorldY(index.sortedVisible());
        if (panel.camera != null) {
            panel.camera.setObservedSortTime(System.nanoTime() - startTime);
        }
    }

    private void recordChunkUpdateTime(long startTime) {
        if (panel.camera != null) {
            panel.camera.setObservedChunkUpdateTime(System.nanoTime() - startTime);
        }
    }

    // ---- simulation / animation ----

    @Override
    public void simulate() {
        interaction.clearRemovalQueue();
        ArrayList<BaseEntity> snapshot = new ArrayList<>(index.entities());
        int nonTile = 0;
        for (BaseEntity baseE : snapshot) {
            if (!(baseE instanceof Tile)) { baseE.update(); nonTile++; }
        }
        for (Tile t : index.tiles()) {
            if (t != null && t.components().size() > 0) t.update();
        }
        reportTickStats(snapshot.size(), nonTile);
    }

    private void reportTickStats(int simulated, int nonTile) {
        Camera cam = panel.camera;
        if (cam == null) return;
        cam.addbackendPrintData("simulated entities: " + simulated);
        cam.addbackendPrintData("non-tile entities: " + nonTile);
        cam.addbackendPrintData("chunks in working memory: " + index.chunks().size());
        cam.addbackendPrintData("render distance: " + chunkSystem.renderDistance);
        cam.addbackendPrintData("amount chunks loaded: " + Chunk.amtLoaded);
        cam.addbackendPrintData("amount chunks generated: " + Chunk.amtGenerated);
    }

    @Override
    public void animate(int frame) {
        for (BaseEntity e : index.entities())  if (e.animated)            e.animate(frame);
        for (Tile t : index.tiles())            if (t != null && t.animated) t.animate(frame);
    }

    // ---- WorldRuntime queries / interaction (delegate) ----

    @Override public ArrayList<Tile>   getTiles()    { return index.tiles(); }
    @Override public ArrayList<Entity> getEntities() { return index.entities(); }
    @Override public ArrayList<Chunk>  getChunks()   { return index.chunks(); }

    @Override public ArrayList<Entity>     getVisibleEntities(Camera c) { return visibility.visibleEntities(c); }
    @Override public ArrayList<BaseEntity> getVisibleTiles(Camera c)    { return visibility.visibleTiles(c); }

    @Override public Tile           getTile(Point p)                     { return interaction.tileAt(p); }
    @Override public resources.domain.farming.FarmTile tillTileAt(Point p) { return interaction.tillTileAt(p); }
    @Override public resources.domain.farming.FarmTile farmTileAt(Point p) { return interaction.farmTileAt(p); }
    @Override public boolean        solidCollision(HitBox hb)            { return interaction.solidCollision(hb); }
    @Override public boolean        solidCollision(HitBox hb, BaseEntity mover) { return interaction.solidCollision(hb, mover); }
    @Override public ArrayList<BaseEntity> getEntitiesCollidedWith(HitBox hb) { return interaction.entitiesCollidedWith(hb); }
    @Override public ArrayList<BaseEntity> getEntitiesCollidedWith(Point p)   { return interaction.entitiesCollidedWith(p); }
    @Override public boolean        placeEntity(BaseEntity e)            { return interaction.placeEntity(e); }
    @Override public boolean        placeEntityIgnoringTerrainCollision(BaseEntity e) {
        return interaction.placeEntityIgnoringTerrainCollision(e);
    }
    @Override public void           removeEntity(BaseEntity e)           { interaction.removeEntity(e); }
    @Override public void           addToRemovalQueue(BaseEntity e)      { interaction.addToRemovalQueue(e); }
    @Override public BaseEntity     getHoveredEntity()                   { return interaction.getHoveredEntity(); }
    @Override public void           setHoveredEntity(int sx, int sy)     { interaction.setHoveredEntity(sx, sy); }

    // Convenience / legacy surface
    public boolean tryPlaceEntity(Stack equipped)       { return interaction.tryPlaceEntity(equipped); }
    public void    addObjectPreview(Stack equipped)     { interaction.addObjectPreview(equipped); }
    public boolean tryHarvestAtMouse(resources.domain.player.Playable player,
                                     resources.app.GameContext ctx)        { return interaction.tryHarvestAtMouse(player, ctx); }
    public ArrayList<Vector> getPath(Entity baseE, Point p) {
        ArrayList<Vector> path = new ArrayList<>();
        path.add(new Vector(p.x - baseE.getHitBox().getWorldX(), p.y - baseE.getHitBox().getWorldY()));
        return path;
    }

    public void writeInfo() {
        for (Chunk c : index.chunks()) c.writeInfo();
        System.out.println("amount of chunks: " + index.chunks().size());
        System.out.println("amount of Entities: " + index.entities().size());
    }
}
