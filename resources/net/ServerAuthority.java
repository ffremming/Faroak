package resources.net;

import resources.core.event.GameEvent;
import resources.domain.entity.BaseEntity;

/**
 * Decides whether a proposed mutation may proceed. In single-player the
 * default impl returns true for everything; in multiplayer, the server's
 * impl validates the client's intent against the canonical world state.
 *
 * Interface only at this stage. The intent is to make every place that
 * could be exploited if it stayed client-side ({@code placeEntity},
 * {@code harvest}, inventory mutations) take an authority and ask it
 * before acting. That turns "we forgot to gate this" into a compile-time
 * error once the implementation arrives.
 */
public interface ServerAuthority {

    boolean authorize(GameEvent intent);

    boolean canPlace(BaseEntity entity);

    boolean canRemove(BaseEntity entity);
}
