package resources.domain.ai;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;

/**
 * Strategy that mutates a host entity once per tick. Implementations are
 * stateful (a Wander remembers its current step target; a Chase tracks its
 * prey) but should keep that state purely on themselves — never on the host
 * — so a behavior can be swapped without leaking stale data.
 *
 * Keep behaviors small and composable; complex actors should be modeled as
 * a behavior tree built from these primitives rather than a single mega-AI.
 */
public interface AIBehavior {
    void tick(BaseEntity host, GameContext ctx);
}
