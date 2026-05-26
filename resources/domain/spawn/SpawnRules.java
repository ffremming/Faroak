package resources.domain.spawn;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import resources.app.GameContext;
import resources.core.time.GameClock;
import resources.domain.mob.MobFactory;

/**
 * Pre-baked spawn-rule catalogue. The defaults express a simple day/night
 * pressure curve: peaceful wildlife during the day, hostile creatures at
 * night. New biomes or events should add their own list rather than editing
 * this one — the spawn service composes rule lists.
 *
 * Biome filtering is currently coarse (rule-level density only). Per-biome
 * gating happens implicitly through {@link MobSpawnService}'s tile validation
 * — landlocked mobs simply reject water tiles. Sharper biome rules can live
 * in a future {@code SpawnRule.biomes} field without breaking callers.
 */
public final class SpawnRules {

    private SpawnRules() {}

    public static List<SpawnRule> defaults(GameContext ctx) {
        List<SpawnRule> rules = new ArrayList<>();

        rules.add(new SpawnRule(
            "deer",
            EnumSet.of(GameClock.Phase.DAY),
            0.10,
            (panel, point) -> MobFactory.peacefulDeer(panel, point.x, point.y, ctx)));

        rules.add(new SpawnRule(
            "goblin",
            EnumSet.of(GameClock.Phase.NIGHT),
            0.15,
            (panel, point) -> MobFactory.hostileGoblin(panel, point.x, point.y, ctx)));

        rules.add(new SpawnRule(
            "spider",
            EnumSet.of(GameClock.Phase.NIGHT),
            0.10,
            (panel, point) -> MobFactory.hostileSpider(panel, point.x, point.y, ctx)));

        return rules;
    }
}
