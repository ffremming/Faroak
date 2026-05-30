package resources.world;

import java.util.HashMap;
import java.util.Map;

import resources.app.GamePanel;
import resources.core.event.DimensionChangeEvent;
import resources.core.event.EventBus;
import resources.core.id.Identifier;
import resources.generation.dimension.Dimension;
import resources.generation.dimension.DimensionRegistry;
import resources.generation.factory.EntityFactory;
import resources.generation.noise.ProceduralGen;

/**
 * Owns one {@link ChunkSystem} per known dimension and swaps the
 * {@link WorkingMemory}'s active one in response to
 * {@link DimensionChangeEvent}s. Each dimension keeps its own chunk system so
 * loaded chunks survive a round trip back to the same dimension instead of
 * being regenerated.
 *
 * Bootstrap path: {@code GenerationManager.generateMap()} constructs one of
 * these once and subscribes it to the panel's event bus.
 */
public final class DimensionService {

    private final GamePanel panel;
    private final WorkingMemory world;
    private final Map<Identifier, ChunkSystem> systems = new HashMap<>();
    private Identifier current;

    public DimensionService(GamePanel panel, WorkingMemory world) {
        this.panel   = panel;
        this.world   = world;
        this.current = DimensionRegistry.OVERWORLD;
        systems.put(current, world.chunkSystem());
    }

    /** Subscribe to dimension-change events on the panel's bus. */
    public void subscribe(EventBus bus) {
        bus.subscribe(DimensionChangeEvent.class, this::onDimensionChange);
    }

    public Identifier currentDimension() { return current; }

    public void registerDimension(Identifier id, ChunkSystem system) {
        systems.put(id, system);
    }

    private void onDimensionChange(DimensionChangeEvent event) {
        if (event.to() == null || event.to().equals(current)) return;
        ChunkSystem next = systems.computeIfAbsent(event.to(), this::lazyBuild);
        if (next == null) return;
        // Order matters here:
        //   1. Teleport the player FIRST so all subsequent world queries see
        //      them at the arrival point.
        //   2. Snap the camera to the arrival point so the visibility filter
        //      inside world.update() (which runs sortVisibleEntities against
        //      camera.getImageHitbox()) culls against the NEW location, not
        //      the stale overworld viewport — without this, the player
        //      vanishes for the ~1s until EnvironmentManager's next tick.
        //   3. Swap the chunk system with the arrival-aware overload so the
        //      new dimension's chunks are loaded around the arrival.
        //   4. Re-place the player in the new chunk system and force an
        //      immediate world.update so the entity index includes them.
        java.awt.Point arrival = event.arrival();
        if (panel.player() != null) {
            panel.player().setWorldX(arrival.x);
            panel.player().setWorldY(arrival.y);
        }
        snapCameraTo(arrival);
        world.setChunkSystem(next, arrival);
        current = event.to();
        if (panel.player() != null) {
            world.placeEntity(panel.player());
            world.update(panel.player().getPoint());
        }
    }

    /** Force the camera onto the arrival point so any visibility-filter pass
     * during the swap uses the new viewport instead of stale coords. */
    private void snapCameraTo(java.awt.Point arrival) {
        if (panel.camera() == null) return;
        panel.camera().worldX = arrival.x - panel.camera().getWidth()  / 2;
        panel.camera().worldY = arrival.y - panel.camera().getHeight() / 2;
        panel.camera().getHitBox().x = arrival.x - panel.camera().getHitBox().width  / 2;
        panel.camera().getHitBox().y = arrival.y - panel.camera().getHitBox().height / 2;
    }

    private ChunkSystem lazyBuild(Identifier id) {
        Dimension dim = DimensionRegistry.instance().get(id).orElse(null);
        if (dim == null) return null;
        // Honour the dimension's pre-registered generator if it's an EntityFactory
        // (overworld), otherwise wrap it via the per-coord adapter. We currently
        // only have EntityFactory implementations that ChunkSystem accepts
        // directly; cave/interior generators implement WorldGenerator and need
        // adapting. Use the adapter path.
        return new ChunkSystem(panel, 8, ChunkSystem.CAVE,
            adapt(dim, panel));
    }

    /**
     * Bridge a {@link Dimension}'s pre-registered {@link resources.generation.WorldGenerator}
     * to the {@link ChunkSystem}'s {@link EntityFactory} dependency. If the
     * dimension was registered with a real EntityFactory we use it as-is; otherwise
     * we build a thin EntityFactory subclass that delegates per-coord lookups to
     * the dimension's WorldGenerator.
     */
    private static EntityFactory adapt(Dimension dim, GamePanel panel) {
        resources.generation.WorldGenerator gen = dim.generator();
        if (gen instanceof EntityFactory) return (EntityFactory) gen;
        return new EntityFactory(panel, new ProceduralGen()) {
            @Override public resources.domain.tile.Tile getTile(int x, int y) { return gen.getTile(x, y); }
            @Override public resources.domain.entity.BaseEntity getEntity(int x, int y) { return gen.getEntity(x, y); }
        };
    }
}
