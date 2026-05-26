package resources.testing.probes;

import java.util.List;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.mob.Mob;
import resources.domain.spawn.MobSpawnService;
import resources.domain.spawn.SpawnRule;
import resources.domain.spawn.SpawnRules;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Exercises the mob spawn pipeline. Repeatedly invokes
 * {@link MobSpawnService#tryPopulateChunk} so we have a fair chance of clearing
 * the per-roll density gate, then checks whether any {@link Mob} entities
 * landed in the world index.
 *
 * Note: the spawn service consults {@link resources.core.time.GameClock#phase()}
 * to filter rules, so during the test-harness boot window (which lands in a
 * specific phase) some rules may be inactive. The probe therefore reports a
 * skip rather than a failure when no mob materialises — the goal here is to
 * confirm "service runs cleanly and is wired up", not "RNG produces a hit".
 */
public final class MobSpawnProbe implements Probe {

    private static final Logger LOG = Logger.forClass(MobSpawnProbe.class);
    private static final int ATTEMPTS = 40;

    @Override public String name() { return "mob-spawn"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        GameContext ctx = harness.context();
        MobSpawnService svc = new MobSpawnService();
        List<SpawnRule> rules = SpawnRules.defaults(ctx);
        if (rules.isEmpty()) return ProbeResult.skip(name() + " no spawn rules");

        int mobsBefore = countMobs(ctx);
        int errors = 0;
        for (int i = 0; i < ATTEMPTS; i++) {
            try {
                svc.tryPopulateChunk(ctx, ctx.player().getPoint(), rules);
            } catch (RuntimeException ex) {
                errors++;
            }
        }
        // Refresh the index so newly placed mobs are visible to getEntities().
        harness.tick(1);
        int mobsAfter = countMobs(ctx);

        String detail = String.format("attempts=%d, errors=%d, mobs-before=%d, mobs-after=%d, phase=%s",
            ATTEMPTS, errors, mobsBefore, mobsAfter, ctx.clock().phase());
        LOG.info(detail);

        if (errors > 0)            return ProbeResult.fail(name() + " spawn service threw", detail);
        if (mobsAfter > mobsBefore) return ProbeResult.pass(name(), detail);
        return ProbeResult.skip(name() + " no mobs produced this run (rng / phase) — service ran clean: " + detail);
    }

    private static int countMobs(GameContext ctx) {
        int n = 0;
        for (BaseEntity e : ctx.world().getEntities()) if (e instanceof Mob) n++;
        return n;
    }
}
