package resources.testing.probes;

import resources.app.GameContext;
import resources.domain.entity.Entity;
import resources.domain.object.GameObject;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;
import resources.world.Chunk;

/**
 * End-to-end exercise of the chunk lifecycle persistence wiring:
 *   - drop a uniquely-named GameObject into a chunk,
 *   - call unLoad() (snapshot + clear),
 *   - call load() (repeated-load path → restore from snapshot),
 *   - assert the named object survived the round trip.
 *
 * Catches regressions where unload forgets to snapshot, where load
 * regenerates entities instead of restoring, or where the serializer
 * lookup misses a stored snapshot.
 */
public final class ChunkPersistenceProbe implements Probe {

    private static final Logger LOG = Logger.forClass(ChunkPersistenceProbe.class);
    private static final String MARKER = "birch_M";

    @Override public String name() { return "chunk-persistence"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.world().getChunks().isEmpty()) {
            return ProbeResult.skip(name() + " no chunks loaded");
        }
        Chunk chunk = ctx.world().getChunks().get(0);

        GameObject marker = new GameObject(ctx.player().panel, MARKER,
            chunk.x + 16, chunk.y + 16, 64, 64, 32, 32, 0, 0, false);
        chunk.getEntities().add(marker);
        int beforeCount = chunk.getEntities().size();

        chunk.unLoad();
        int afterUnload = chunk.getEntities().size();
        boolean clearedOnUnload = afterUnload == 0;

        chunk.load();
        int afterReload = chunk.getEntities().size();

        boolean markerFound = false;
        for (Entity e : chunk.getEntities()) {
            if (MARKER.equals(e.getName())) { markerFound = true; break; }
        }

        // Clean up: remove the marker so we don't leak state into later probes.
        chunk.getEntities().removeIf(e -> MARKER.equals(e.getName()));

        String detail = String.format(
            "before=%d, after-unload=%d, after-reload=%d, marker-survived=%s",
            beforeCount, afterUnload, afterReload, markerFound);
        LOG.info(detail);

        if (!clearedOnUnload) return ProbeResult.fail(name() + " entities not cleared on unload", detail);
        if (!markerFound)     return ProbeResult.fail(name() + " marker did not survive reload", detail);
        return ProbeResult.pass(name(), detail);
    }
}
