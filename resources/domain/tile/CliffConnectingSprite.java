package resources.domain.tile;

import java.util.List;

import resources.domain.tile.connect.ConnectingSprite;
import resources.domain.tile.connect.ConnectionBitmask;

/**
 * {@link ConnectingSprite} for the cliff atlas: maps an N/E/S/W connection
 * bitmask to one of nine "cliff<n>" pieces (corners, edges, inner).
 *
 * The variant table mirrors the legacy hand-rolled selector — the value is
 * that this logic is now reusable: fences, walls, bushes can plug in their
 * own {@link ConnectingSprite} without re-implementing the bitmask plumbing.
 */
public final class CliffConnectingSprite implements ConnectingSprite {

    public static final CliffConnectingSprite INSTANCE = new CliffConnectingSprite();

    private CliffConnectingSprite() {}

    @Override
    public void appendLayers(int mask, List<String> sink) {
        sink.add("cliff" + variant(mask));
    }

    /**
     * Variant index for {@code mask} (N=1, E=2, S=4, W=8). 0 is the default
     * filler; 1-9 are corner/edge pieces.
     */
    private int variant(int mask) {
        boolean n = ConnectionBitmask.has(mask, ConnectionBitmask.N);
        boolean e = ConnectionBitmask.has(mask, ConnectionBitmask.E);
        boolean s = ConnectionBitmask.has(mask, ConnectionBitmask.S);
        boolean w = ConnectionBitmask.has(mask, ConnectionBitmask.W);

        if (!n && w  && e)         return 2;  // top edge
        if ( n && e  && !s && !w)  return 7;  // bottom-left corner
        if ( n && w  && !s && !e)  return 9;  // bottom-right corner
        if ( s && e  && !n && !w)  return 1;  // top-left corner
        if ( s && w  && !n && !e)  return 3;  // top-right corner
        if (!s && e  && w)         return 8;  // bottom edge
        if (!w && n  && s)         return 4;  // left edge
        if (!e && n  && s)         return 6;  // right edge
        return 0;
    }
}
