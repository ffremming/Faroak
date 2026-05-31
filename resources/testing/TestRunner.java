package resources.testing;

import java.util.ArrayList;
import java.util.List;

import resources.testing.probes.AIProbe;
import resources.testing.probes.AnimationFrameProbe;
import resources.testing.probes.BeachWaterTransitionProbe;
import resources.testing.probes.WaterDepthProbe;
import resources.testing.probes.WaterRenderProbe;
import resources.testing.probes.BoatProbe;
import resources.testing.probes.FactionProbe;
import resources.testing.probes.WeaponLoadoutProbe;
import resources.testing.probes.CaveEntryProbe;
import resources.testing.probes.CaveProbe;
import resources.testing.probes.CaveWallSpriteProbe;
import resources.testing.probes.CaveWallVariantProbe;
import resources.testing.probes.PlayerVisibleAfterPortalProbe;
import resources.testing.probes.ChunkPersistenceProbe;
import resources.testing.probes.DimensionProbe;
import resources.testing.probes.CropArtAliasProbe;
import resources.testing.probes.CropGrowthPaceProbe;
import resources.testing.probes.CropHarvestProbe;
import resources.testing.probes.FarmBorderProbe;
import resources.testing.probes.FarmingProbe;
import resources.testing.probes.GrowableProbe;
import resources.testing.probes.GroundItemProbe;
import resources.testing.probes.CatalogProbe;
import resources.testing.probes.PlaceAllProbe;
import resources.testing.probes.HarvestableProbe;
import resources.testing.probes.HealthProbe;
import resources.testing.probes.InteriorProbe;
import resources.testing.probes.LightingProbe;
import resources.testing.probes.MobSpawnProbe;
import resources.testing.probes.MovementPerfProbe;
import resources.testing.probes.MultiplayerAuthorityProbe;
import resources.testing.probes.MultiplayerPersistenceProbe;
import resources.testing.probes.MultiplayerTenClientProbe;
import resources.testing.probes.MultiplayerWorldReplicationProbe;
import resources.testing.probes.PersistenceProbe;
import resources.testing.probes.PlacementPreviewProbe;
import resources.testing.probes.RenderPerfProbe;
import resources.testing.probes.SimulatePerfProbe;
import resources.testing.probes.TerrainSpeedProbe;
import resources.testing.probes.TileBorderProbe;
import resources.testing.probes.ProtocolCodecProbe;
import resources.testing.probes.WebSocketGatewayProbe;
import resources.testing.probes.WebSocketTwoClientMovementProbe;

/**
 * Entry point for the test harness. Boots a {@link TestHarness}, runs each
 * registered {@link Probe}, prints a summary, exits with code 1 if any probe
 * fails.
 *
 * Use case: invoke from CLI after a refactor to confirm gameplay invariants
 * (borders present, animation ticking, simulate() not blowing up) without a
 * full visual inspection.
 *
 * Run with:
 *   java -cp /tmp/gamebuild resources.testing.TestRunner
 */
public final class TestRunner {

    private static final Logger LOG = Logger.forClass(TestRunner.class);

    public static void main(String[] args) {
        if (args.length > 0 && "verbose".equalsIgnoreCase(args[0])) {
            Logger.setThreshold(Logger.Level.DEBUG);
        }

        List<Probe> probes = new ArrayList<>();
        probes.add(new TileBorderProbe());
        probes.add(new BeachWaterTransitionProbe());
        probes.add(new WaterDepthProbe());
        probes.add(new WaterRenderProbe());
        probes.add(new AnimationFrameProbe());
        probes.add(new LightingProbe());
        probes.add(new HarvestableProbe());
        probes.add(new GroundItemProbe());
        probes.add(new CatalogProbe());
        probes.add(new PlaceAllProbe());
        probes.add(new GrowableProbe());
        probes.add(new AIProbe());
        probes.add(new TerrainSpeedProbe());
        probes.add(new PersistenceProbe());
        probes.add(new SimulatePerfProbe());
        probes.add(new MovementPerfProbe());
        probes.add(new RenderPerfProbe());
        probes.add(new ProtocolCodecProbe());
        probes.add(new MultiplayerAuthorityProbe());
        probes.add(new MultiplayerTenClientProbe());
        probes.add(new MultiplayerPersistenceProbe());
        probes.add(new MultiplayerWorldReplicationProbe());
        probes.add(new WebSocketGatewayProbe());
        probes.add(new WebSocketTwoClientMovementProbe());
        probes.add(new ChunkPersistenceProbe());
        probes.add(new HealthProbe());
        probes.add(new FarmingProbe());
        probes.add(new CropHarvestProbe());
        probes.add(new CropGrowthPaceProbe());
        probes.add(new CropArtAliasProbe());
        probes.add(new FarmBorderProbe());
        probes.add(new MobSpawnProbe());
        probes.add(new PlacementPreviewProbe());
        probes.add(new BoatProbe());
        probes.add(new FactionProbe());
        probes.add(new WeaponLoadoutProbe());
        probes.add(new CaveEntryProbe());
        probes.add(new CaveProbe());
        probes.add(new CaveWallVariantProbe());
        probes.add(new CaveWallSpriteProbe());
        probes.add(new PlayerVisibleAfterPortalProbe());
        probes.add(new InteriorProbe());
        // Run last — swaps chunk systems and perturbs world state.
        probes.add(new DimensionProbe());

        int failures = 0;
        try (TestHarness harness = new TestHarness()) {
            harness.tick(120); // warm-up: load chunks, settle animation
            LOG.info("running %d probes…", probes.size());

            for (Probe probe : probes) {
                LOG.info("→ %s", probe.name());
                ProbeResult result = probe.run(harness);
                printResult(probe, result);
                if (result.status() == ProbeResult.Status.FAIL) failures++;
            }
        }

        LOG.info("---");
        LOG.info("%d probe(s), %d failure(s)", probes.size(), failures);
        if (failures > 0) System.exit(1);
    }

    private static void printResult(Probe probe, ProbeResult result) {
        String prefix = "  " + result.status() + "  " + probe.name();
        LOG.info("%s — %s", prefix, result.headline());
        if (!result.details().isEmpty()) LOG.info("      %s", result.details());
    }
}
