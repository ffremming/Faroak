package resources.generation.interior;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalog of every predefined interior in the game. Mirrors the static-final
 * pattern used by {@link resources.generation.biome.BiomeRegistry}: each
 * interior is hand-authored as a char grid, no procedural generation.
 *
 * To add a new interior: create the {@link Interior} constant, then add it to
 * {@link #ALL} so {@link InteriorManager} can place it.
 */
public final class InteriorRegistry {

    /** 10x10 starter house with door on south wall (tile-local 5,9). */
    public static final Interior STARTER_HOUSE = new Interior(
        "starter_house", 5, 9,
        "##########",
        "#........#",
        "#.T....C.#",
        "#.T......#",
        "#........#",
        "#......B.#",
        "#........#",
        "#.C....T.#",
        "#........#",
        "#####D####"
    );

    /** 12x9 stone hall — stone floor, two crate piles, door on east wall. */
    public static final Interior STONE_HALL = new Interior(
        "stone_hall", 11, 4,
        "############",
        "#,,,,,,,,,,#",
        "#,B,,,,,,B,#",
        "#,B,,,,,,B,#",
        "#,,,,,,,,,,D",
        "#,B,,,,,,B,#",
        "#,B,,,,,,B,#",
        "#,,,,,,,,,,#",
        "############"
    );

    /** 14x7 long cabin — wood floor, table+chairs row, door on south wall. */
    public static final Interior LONG_CABIN = new Interior(
        "long_cabin", 6, 6,
        "##############",
        "#............#",
        "#.C.T.T.T.C..#",
        "#............#",
        "#..B......B..#",
        "#............#",
        "######D#######"
    );

    /** Stable iteration order. */
    public static final java.util.List<Interior> ALL = Collections.unmodifiableList(
        Arrays.asList(STARTER_HOUSE, STONE_HALL, LONG_CABIN));

    private static final Map<String, Interior> BY_NAME;
    static {
        Map<String, Interior> m = new LinkedHashMap<>();
        for (Interior i : ALL) m.put(i.name(), i);
        BY_NAME = Collections.unmodifiableMap(m);
    }

    private InteriorRegistry() {}

    public static Interior byName(String name) {
        return BY_NAME.get(name);
    }
}
