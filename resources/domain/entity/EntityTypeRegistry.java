package resources.domain.entity;

import resources.core.registry.Registry;

/**
 * Central registry of {@link EntityType} definitions (vegetation, structures,
 * mobs, placed objects).
 *
 * Intentionally empty by default — built-in entities are registered by their
 * owning subsystem at bootstrap (e.g. {@code VegetationContent} registers oak,
 * birch, shrub, etc.). Keeping registration outside this file means new entity
 * packs can be added without touching the registry itself.
 */
public final class EntityTypeRegistry {

    private static final Registry<EntityType> REG = new Registry<>("entity_type");

    private EntityTypeRegistry() {}

    public static Registry<EntityType> instance() { return REG; }
}
