package resources.testing.probes;

import resources.app.GameContext;
import resources.domain.object.GameObject;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;
import resources.world.Chunk;
import resources.world.persistence.ChunkSnapshot;
import resources.world.persistence.InMemoryChunkSerializer;

/**
 * Verifies the memento contract on a live {@link Chunk}: snapshot the
 * entity bag, mutate the chunk, then restore — the chunk's entity bag
 * should match the captured snapshot byte-for-reference.
 */
public final class PersistenceProbe implements Probe {

    private static final Logger LOG = Logger.forClass(PersistenceProbe.class);

    @Override public String name() { return "persistence"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (ctx.world().getChunks().isEmpty()) {
            return ProbeResult.skip(name() + " no chunks loaded");
        }
        Chunk chunk = ctx.world().getChunks().get(0);
        InMemoryChunkSerializer serializer = new InMemoryChunkSerializer();
        ChunkSnapshot snap = serializer.snapshot(chunk);

        int beforeCount = chunk.getEntities().size();

        GameObject ghost = new GameObject(ctx.player().panel, "wildGrass",
            chunk.x + 16, chunk.y + 16, 64, 64, 32, 32, 0, 0, false);
        chunk.getEntities().add(ghost);
        int afterMutateCount = chunk.getEntities().size();

        serializer.restore(chunk, snap);
        int afterRestoreCount = chunk.getEntities().size();

        String detail = String.format("before=%d, mutated=%d, restored=%d, snapshot-size=%d",
            beforeCount, afterMutateCount, afterRestoreCount, snap.entities().size());
        LOG.info(detail);

        if (afterMutateCount  != beforeCount + 1)  return ProbeResult.fail(name() + " mutation invisible", detail);
        if (afterRestoreCount != beforeCount)      return ProbeResult.fail(name() + " restore did not revert", detail);
        if (snap.entities().size() != beforeCount) return ProbeResult.fail(name() + " snapshot count drifted", detail);
        if (snap.tiles() != chunk.tiles)           return ProbeResult.fail(name() + " tile-grid ref drifted", detail);
        return ProbeResult.pass(name(), detail);
    }
}
