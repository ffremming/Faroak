package resources.testing;

import java.util.ArrayList;
import java.util.List;

import resources.testing.probes.MultiplayerAuthorityProbe;
import resources.testing.probes.MultiplayerPersistenceProbe;
import resources.testing.probes.MultiplayerReconnectProbe;
import resources.testing.probes.MultiplayerTenClientProbe;
import resources.testing.probes.MultiplayerTerrainRulesProbe;
import resources.testing.probes.MultiplayerWorldReplicationProbe;
import resources.testing.probes.AuthoritativeCombatGameplayProbe;
import resources.testing.probes.AuthoritativeCraftingGameplayProbe;
import resources.testing.probes.AuthoritativeFarmingGameplayProbe;
import resources.testing.probes.AuthoritativeSharedStateProbe;
import resources.testing.probes.AuthoritativeCommandGameplayProbe;
import resources.testing.probes.ProtocolCodecProbe;
import resources.testing.probes.ProtocolV2SnapshotProbe;
import resources.testing.probes.WebSocketGatewayProbe;
import resources.testing.probes.WebSocketTwoClientMovementProbe;
import resources.testing.probes.MpPersistenceFileProbe;
import resources.testing.probes.MpAppearanceCodecProbe;
import resources.testing.probes.MpAppearanceServerProbe;
import resources.testing.probes.MpWorldPopulationProbe;
import resources.testing.probes.MpDeathRespawnProbe;
import resources.testing.probes.MpWorldTimeProbe;
import resources.testing.probes.MpChatProbe;
import resources.testing.probes.MpPredictionProbe;
import resources.testing.probes.MpMoveSpeedProbe;
import resources.testing.probes.MpObjectCollisionProbe;
import resources.testing.probes.MpPlacementRangeProbe;
import resources.testing.probes.MpClientWorldGenProbe;
import resources.testing.probes.MpClientAuthMoveProbe;

/**
 * Focused runner for multiplayer probes only.
 */
public final class MultiplayerTestRunner {

    private static final Logger LOG = Logger.forClass(MultiplayerTestRunner.class);

    public static void main(String[] args) {
        List<Probe> probes = new ArrayList<>();
        probes.add(new ProtocolCodecProbe());
        probes.add(new ProtocolV2SnapshotProbe());
        probes.add(new MultiplayerAuthorityProbe());
        probes.add(new MultiplayerTerrainRulesProbe());
        probes.add(new AuthoritativeSharedStateProbe());
        probes.add(new AuthoritativeCommandGameplayProbe());
        probes.add(new AuthoritativeFarmingGameplayProbe());
        probes.add(new AuthoritativeCraftingGameplayProbe());
        probes.add(new AuthoritativeCombatGameplayProbe());
        probes.add(new MultiplayerTenClientProbe());
        probes.add(new MultiplayerPersistenceProbe());
        probes.add(new MultiplayerWorldReplicationProbe());
        probes.add(new MultiplayerReconnectProbe());
        probes.add(new WebSocketGatewayProbe());
        probes.add(new WebSocketTwoClientMovementProbe());
        probes.add(new MpPersistenceFileProbe());
        probes.add(new MpAppearanceCodecProbe());
        probes.add(new MpAppearanceServerProbe());
        probes.add(new MpWorldPopulationProbe());
        probes.add(new MpDeathRespawnProbe());
        probes.add(new MpWorldTimeProbe());
        probes.add(new MpChatProbe());
        probes.add(new MpPredictionProbe());
        probes.add(new MpMoveSpeedProbe());
        probes.add(new MpObjectCollisionProbe());
        probes.add(new MpPlacementRangeProbe());
        probes.add(new MpClientWorldGenProbe());
        probes.add(new MpClientAuthMoveProbe());

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
