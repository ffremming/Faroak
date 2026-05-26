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
        world.setChunkSystem(next);
        current = event.to();
        if (panel.player != null) {
            panel.player.setWorldX(event.arrival().x);
            panel.player.setWorldY(event.arrival().y);
        }
    }

    private ChunkSystem lazyBuild(Identifier id) {
        Dimension dim = DimensionRegistry.instance().get(id).orElse(null);
        if (dim == null) return null;
        return new ChunkSystem(panel, 8, ChunkSystem.CAVE,
            new EntityFactory(panel, new ProceduralGen()));
    }
}
