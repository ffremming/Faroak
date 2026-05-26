package resources.testing.probes;

import resources.domain.entity.component.HealthComponent;
import resources.testing.Logger;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Pure data-carrier sanity check for {@link HealthComponent}:
 *   - construction clamps {@code current} to {@code max},
 *   - {@code takeDamage} decrements and never goes below zero,
 *   - depletion flips the {@code dead} flag.
 *
 * Stand-alone test — no world interaction required, so this runs cleanly even
 * if other probes have perturbed the game state.
 */
public final class HealthProbe implements Probe {

    private static final Logger LOG = Logger.forClass(HealthProbe.class);

    @Override public String name() { return "health"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        HealthComponent hp = new HealthComponent(10);

        if (hp.current() != 10)     return ProbeResult.fail(name() + " starting current != max");
        if (hp.max()     != 10)     return ProbeResult.fail(name() + " max not preserved");
        if (hp.isDead())            return ProbeResult.fail(name() + " starts dead");

        hp.takeDamage(3);
        int afterFirst = hp.current();

        hp.takeDamage(20); // overkill
        int afterOverkill = hp.current();
        boolean dead = hp.isDead();

        // takeDamage post-death should be a no-op
        hp.takeDamage(5);
        int afterPostDeath = hp.current();

        String detail = String.format("after3=%d, afterOverkill=%d, dead=%s, postDeath=%d",
            afterFirst, afterOverkill, dead, afterPostDeath);
        LOG.info(detail);

        if (afterFirst    != 7) return ProbeResult.fail(name() + " takeDamage(3) gave " + afterFirst, detail);
        if (afterOverkill != 0) return ProbeResult.fail(name() + " overkill did not clamp to 0", detail);
        if (!dead)              return ProbeResult.fail(name() + " did not mark dead on depletion", detail);
        if (afterPostDeath != 0)return ProbeResult.fail(name() + " current changed after death", detail);
        return ProbeResult.pass(name(), detail);
    }
}
