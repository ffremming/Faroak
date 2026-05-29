package resources.testing;

import java.util.ArrayList;
import java.util.List;

import resources.testing.probes.MultiplayerAuthorityProbe;
import resources.testing.probes.MultiplayerPersistenceProbe;
import resources.testing.probes.MultiplayerReconnectProbe;
import resources.testing.probes.MultiplayerTenClientProbe;
import resources.testing.probes.MultiplayerWorldReplicationProbe;
import resources.testing.probes.ProtocolCodecProbe;
import resources.testing.probes.WebSocketGatewayProbe;
import resources.testing.probes.WebSocketTwoClientMovementProbe;

/**
 * Focused runner for multiplayer probes only.
 */
public final class MultiplayerTestRunner {

    private static final Logger LOG = Logger.forClass(MultiplayerTestRunner.class);

    public static void main(String[] args) {
        List<Probe> probes = new ArrayList<>();
        probes.add(new ProtocolCodecProbe());
        probes.add(new MultiplayerAuthorityProbe());
        probes.add(new MultiplayerTenClientProbe());
        probes.add(new MultiplayerPersistenceProbe());
        probes.add(new MultiplayerWorldReplicationProbe());
        probes.add(new MultiplayerReconnectProbe());
        probes.add(new WebSocketGatewayProbe());
        probes.add(new WebSocketTwoClientMovementProbe());

        int failures = 0;
        try (TestHarness harness = new TestHarness()) {
            harness.tick(60);
            LOG.info("running %d multiplayer probes...", probes.size());
            for (Probe probe : probes) {
                LOG.info("-> %s", probe.name());
                ProbeResult result = probe.run(harness);
                LOG.info("  %s  %s", result.status(), result.headline());
                if (!result.details().isEmpty()) LOG.info("      %s", result.details());
                if (result.status() == ProbeResult.Status.FAIL) failures++;
            }
        }
        LOG.info("---");
        LOG.info("%d probe(s), %d failure(s)", probes.size(), failures);
        if (failures > 0) System.exit(1);
    }
}
