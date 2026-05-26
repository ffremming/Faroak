package resources.domain.entity;

/**
 * Anything whose state advances on the simulation tick. Implemented by
 * {@link BaseEntity}; lets the simulator iterate without caring whether
 * a particular thing is an Entity, a controllable Playable, or something else
 * we add later (projectiles, effects, NPC AI).
 */
public interface Tickable {
    void update();
}
