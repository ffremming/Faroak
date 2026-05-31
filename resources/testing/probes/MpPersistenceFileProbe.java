package resources.testing.probes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import resources.net.multiplayer.server.persistence.JsonFilePersistenceStore;
import resources.net.multiplayer.server.persistence.PersistenceStore;
import resources.net.multiplayer.server.persistence.PersistenceStore.PersistedPlayer;
import resources.net.multiplayer.server.persistence.PersistenceStoreFactory;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the pure-JDK file persistence store round-trips player, world chunk,
 * and meta state across a close/reopen, and that the factory defaults to it.
 */
public final class MpPersistenceFileProbe implements Probe {

    @Override public String name() { return "mp-persistence-file"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        try {
            Path dir = Files.createTempDirectory("mp-file-store");

            // --- Round-trip across a close/reopen ---
            PersistenceStore a = new JsonFilePersistenceStore(dir);
            a.savePlayer(new PersistedPlayer("alice", 100.0, 200.0, 1.0, 2.0, 7L));
            a.saveWorldChunk(42L, new byte[] { 1, 2, 3 });
            a.putMeta("k", "v");
            a.checkpoint();
            a.close();

            PersistenceStore b = new JsonFilePersistenceStore(dir);
            Optional<PersistedPlayer> p = b.loadPlayer("alice");
            boolean playerOk = p.isPresent()
                && p.get().worldX == 100.0
                && p.get().worldY == 200.0
                && p.get().velocityX == 1.0
                && p.get().velocityY == 2.0
                && p.get().lastSequence == 7L;
            Optional<byte[]> chunk = b.loadWorldChunk(42L);
            boolean chunkOk = chunk.isPresent()
                && chunk.get().length == 3
                && chunk.get()[0] == 1 && chunk.get()[1] == 2 && chunk.get()[2] == 3;
            boolean metaOk = "v".equals(b.getMeta("k", "x"));
            boolean keysOk = b.listWorldChunkKeys().contains(42L);
            boolean missingOk = b.loadPlayer("nobody").isEmpty();
            b.close();

            // --- Factory defaults to the durable file store ---
            String prevBackend = System.getProperty("game.multiplayer.persistence");
            String prevDataDir = System.getProperty("game.multiplayer.dataDir");
            System.clearProperty("game.multiplayer.persistence");
            Path factoryDir = Files.createTempDirectory("mp-factory-store");
            System.setProperty("game.multiplayer.dataDir", factoryDir.toString());
            PersistenceStore def = PersistenceStoreFactory.createDefault("ignored.db");
            boolean factoryFileDefault = def instanceof JsonFilePersistenceStore;
            def.close();

            System.setProperty("game.multiplayer.persistence", "memory");
            PersistenceStore mem = PersistenceStoreFactory.createDefault("ignored.db");
            boolean factoryMemory = !(mem instanceof JsonFilePersistenceStore);
            mem.close();

            // restore properties
            if (prevBackend == null) System.clearProperty("game.multiplayer.persistence");
            else System.setProperty("game.multiplayer.persistence", prevBackend);
            if (prevDataDir == null) System.clearProperty("game.multiplayer.dataDir");
            else System.setProperty("game.multiplayer.dataDir", prevDataDir);

            boolean ok = playerOk && chunkOk && metaOk && keysOk && missingOk
                && factoryFileDefault && factoryMemory;
            String details = "player=" + playerOk + " chunk=" + chunkOk + " meta=" + metaOk
                + " keys=" + keysOk + " missing=" + missingOk
                + " factoryFileDefault=" + factoryFileDefault + " factoryMemory=" + factoryMemory;
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        }
    }
}
