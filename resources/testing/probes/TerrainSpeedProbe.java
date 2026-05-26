package resources.testing.probes;

import java.util.HashMap;
import java.util.Map;

import resources.app.GameContext;
import resources.domain.entity.component.TerrainSpeedComponent;
import resources.domain.player.Playable;
import resources.domain.tile.Tile;
import resources.geometry.Vector;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies that attaching a {@link TerrainSpeedComponent} scales the player's
 * per-tick movement through the {@link resources.domain.player.MovementController}.
 *
 * Strategy:
 *   1. Read the tile under the player.
 *   2. Establish baseline: push a known velocity, tick, measure displacement.
 *   3. Attach a component that halves speed for that tile.
 *   4. Push the same velocity, tick, measure displacement.
 *   5. Expect the slowed displacement to be strictly less than the baseline.
 */
public final class TerrainSpeedProbe implements Probe {

    private static final Logger LOG = Logger.forClass(TerrainSpeedProbe.class);
    private static final double SLOW = 0.25;

    @Override public String name() { return "terrain-speed"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        if (!(ctx.player() instanceof Playable)) return ProbeResult.skip(name() + " no Playable");
        Playable player = (Playable) ctx.player();

        Tile under = ctx.world().getTile(player.getPoint());
        String tileName = under == null ? "unknown" : under.getName();

        double baseline = measureDisplacement(player, harness);
        player.components().add(makeComponent(tileName, SLOW));
        double slowed  = measureDisplacement(player, harness);
        // Remove so the player isn't permanently slowed for downstream probes.
        player.components().remove(TerrainSpeedComponent.class);

        String detail = String.format("tile=%s, baseline=%.3f, slowed=%.3f, ratio=%.3f",
            tileName, baseline, slowed, baseline == 0 ? 0 : slowed / baseline);
        LOG.info(detail);
        if (baseline <= 0)        return ProbeResult.fail(name() + " baseline non-positive", detail);
        if (slowed >= baseline)   return ProbeResult.fail(name() + " component had no effect", detail);
        return ProbeResult.pass(name(), detail);
    }

    private double measureDisplacement(Playable player, TestHarness harness) {
        double x0 = player.getWorldX();
        double y0 = player.getWorldY();
        player.addVelocity(new Vector(2.0, 0));
        harness.tick(1);
        return Math.hypot(player.getWorldX() - x0, player.getWorldY() - y0);
    }

    private TerrainSpeedComponent makeComponent(String tileName, double mult) {
        Map<String, Double> map = new HashMap<>();
        map.put(tileName, mult);
        return new TerrainSpeedComponent(map);
    }
}
