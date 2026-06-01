package resources.testing.probes;

import resources.app.GamePanel;
import resources.generation.factory.EntityFactory;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the client generates its procedural world (trees/rocks) the SAME way
 * online and offline. With client-authoritative movement the client owns its own
 * collision, so locally-generated objects no longer cause the position divergence
 * that produced the teleport — and the world must NOT be empty online (the server's
 * generic objects render only as placeholders, so the client's real sprited objects
 * are what populate the world).
 */
public final class MpClientWorldGenProbe implements Probe {

    @Override public String name() { return "mp-client-worldgen"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GamePanel panel = harness.panel();
        EntityFactory factory = new EntityFactory(panel,
            new resources.generation.noise.ProceduralGen(424242L));

        String prevMode = System.getProperty("game.multiplayer.mode");
        try {
            System.setProperty("game.multiplayer.mode", "offline");
            int offlineObjects = countVegetation(factory);

            System.setProperty("game.multiplayer.mode", "host");
            int onlineObjects = countVegetation(factory);

            // The world must be populated online (no empty world), and online must
            // match offline generation exactly (deterministic from the shared seed).
            boolean offlineGenerates = offlineObjects > 0;
            boolean onlineGenerates = onlineObjects > 0;
            boolean sameAsOffline = onlineObjects == offlineObjects;
            boolean ok = offlineGenerates && onlineGenerates && sameAsOffline;
            String details = "offlineObjects=" + offlineObjects + " onlineObjects=" + onlineObjects;
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } finally {
            if (prevMode == null) System.clearProperty("game.multiplayer.mode");
            else System.setProperty("game.multiplayer.mode", prevMode);
        }
    }

    /** Count vegetation objects across a grid of tiles (skips empty/water tiles). */
    private static int countVegetation(EntityFactory factory) {
        int n = 0;
        int ts = 64;
        for (int tx = -40; tx <= 40; tx++) {
            for (int ty = -40; ty <= 40; ty++) {
                if (factory.getEntity(tx * ts, ty * ts) != null) n++;
            }
        }
        return n;
    }
}
