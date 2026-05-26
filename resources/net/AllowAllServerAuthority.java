package resources.net;

import resources.core.event.GameEvent;
import resources.domain.entity.BaseEntity;

/**
 * Single-player default authority. Accepts all intents and mutations.
 *
 * Multiplayer server mode should replace this with a validating authority
 * that checks ownership, cooldowns, reach, and world rules.
 */
public final class AllowAllServerAuthority implements ServerAuthority {

    @Override
    public boolean authorize(GameEvent intent) {
        return true;
    }

    @Override
    public boolean canPlace(BaseEntity entity) {
        return true;
    }

    @Override
    public boolean canRemove(BaseEntity entity) {
        return true;
    }
}
