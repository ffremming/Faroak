package resources.domain.spawn;

import java.awt.Point;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import resources.app.GamePanel;
import resources.core.time.GameClock;
import resources.domain.mob.Mob;

/**
 * Declarative description of "when and how dense" a mob type should appear.
 * Pure value object: no spawn behaviour lives here — that's the
 * {@link MobSpawnService}. Keeping the rule data-only makes new biomes/phases
 * a configuration change, not a code change.
 *
 * {@code density} is interpreted by the spawn service as "expected mobs per
 * eligible chunk". Phases are matched against {@link GameClock#phase()};
 * the empty set means "any phase".
 */
public final class SpawnRule {

    public final String mobName;
    public final Set<GameClock.Phase> phases;
    public final double density;
    public final BiFunction<GamePanel, Point, Mob> factory;

    public SpawnRule(String mobName,
                     Set<GameClock.Phase> phases,
                     double density,
                     BiFunction<GamePanel, Point, Mob> factory) {
        if (mobName == null || factory == null) {
            throw new IllegalArgumentException("mobName and factory must be non-null");
        }
        if (density < 0.0) throw new IllegalArgumentException("density must be >= 0");
        this.mobName = mobName;
        this.phases  = (phases == null || phases.isEmpty())
                       ? Collections.unmodifiableSet(EnumSet.allOf(GameClock.Phase.class))
                       : Collections.unmodifiableSet(new HashSet<>(phases));
        this.density = density;
        this.factory = factory;
    }

    public boolean matchesPhase(GameClock.Phase phase) {
        return phase != null && phases.contains(phase);
    }
}
