package resources.domain.tile.connect;

import java.util.List;

/**
 * Strategy that maps a {@link ConnectionBitmask} to one or more sprite keys,
 * appended to a caller-owned sink (no per-frame allocation in the common path).
 *
 * Implementations typically own a static variant table (e.g. cliffs' 9 corner
 * pieces) or compute the key by composition (fences with separate post + rail
 * sprites).
 */
public interface ConnectingSprite {

    /** Append sprite keys to layer (in draw order) for {@code bitmask}. */
    void appendLayers(int bitmask, List<String> sink);
}
