package resources.generation.interior;

import resources.core.id.Identifier;
import resources.domain.tile.TileType;
import resources.domain.tile.TileTypeRegistry;

/**
 * Registers tile types used by the interior dimension. Call {@link #bootstrap()}
 * exactly once during startup before the {@link InteriorGenerator} tries to
 * resolve any tile.
 *
 * Tile names match the strings emitted by {@link InteriorGenerator} so sprite
 * lookup (or auto-generated fallback swatches) wires up by convention.
 */
public final class InteriorTileTypes {

    public static final String WALL_INDOOR = "wall_indoor";
    public static final String FLOOR_WOOD  = "floor_wood";
    public static final String FLOOR_STONE = "floor_stone";
    public static final String VOID_FLOOR  = "void_floor";

    private static boolean registered = false;

    private InteriorTileTypes() {}

    /** Idempotent: safe to call multiple times. */
    public static synchronized void bootstrap() {
        if (registered) return;
        define(WALL_INDOOR, 300, false, true,  false);
        define(FLOOR_WOOD,  100, false, false, false);
        define(FLOOR_STONE, 100, false, false, false);
        define(VOID_FLOOR,  0,   false, false, false);
        registered = true;
    }

    private static TileType define(String name, int altitudeBucket,
                                   boolean water, boolean solid, boolean animated) {
        Identifier id = Identifier.of(name);
        TileType type = TileType.builder(id, name)
            .altitudeBucket(altitudeBucket)
            .water(water)
            .solid(solid)
            .animated(animated)
            .build();
        TileTypeRegistry.instance().register(id, type);
        return type;
    }
}
