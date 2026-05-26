package resources.domain.entity.component;

import resources.domain.entity.BaseEntity;

/**
 * Pluggable behaviour attached to a {@link BaseEntity}. Allows new entity
 * capabilities to be added without growing an inheritance pyramid.
 *
 * Lifecycle hooks let the component initialise (e.g. subscribe to events,
 * register with a system) and tear down (e.g. unsubscribe) when attached/
 * detached.
 *
 * Implementations typically also implement one or more of:
 *   - {@link resources.domain.entity.Tickable}     — receives the simulation tick
 *   - {@link resources.domain.entity.Drawable}     — drawn separately from the host entity (rare)
 *   - any feature-specific interface (LightEmitter, Harvestable, AIControlled)
 *
 * Components are looked up on the host entity by their concrete class:
 *   {@code entity.get(LightSourceComponent.class)}
 */
public interface EntityComponent {

    /** Called once when the component is attached to {@code owner}. */
    default void onAttach(BaseEntity owner) {}

    /** Called once when the component is removed (or the owner is destroyed). */
    default void onDetach(BaseEntity owner) {}
}
