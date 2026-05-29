package resources.domain.tile;

import java.util.List;

import resources.domain.tile.connect.ConnectingSprite;
import resources.domain.tile.connect.ConnectionBitmask;

/**
 * 4-bit cliff sprite picker for mountain / cave walls. Emits ONE cliff
 * overlay per tile — the same one-sprite-per-tile pattern the overworld's
 * {@link CliffConnectingSprite} uses successfully.
 *
 * The cliff atlas is a border atlas, not a body atlas: each sprite is
 * partially transparent and is meant to draw over the underlying tile (the
 * stone body shows through). Trying to layer multiple overlays per tile,
 * or to draw a separate body sprite underneath, produces the doubled /
 * stacked-ridge artifact we saw — the overlays were designed to stand on
 * their own.
 *
 * "open" = neighbour is NOT a mountain (floor or void). The variant table
 * mirrors {@link CliffConnectingSprite} exactly.
 */
public final class MountainConnectingSprite implements ConnectingSprite {

    public static final MountainConnectingSprite INSTANCE = new MountainConnectingSprite();

    private MountainConnectingSprite() {}

    @Override
    public void appendLayers(int mask, List<String> sink) {
        appendLayers(mask, sink, 0, 0);
    }

    /**
     * Full 8-bit lookup. Cardinal mask picks the primary cliff sprite (one
     * per tile, matching the overworld's atlas convention). When the tile
     * is fully cardinally enclosed but a diagonal neighbour is open (a
     * concave floor pocket), an additional outer-corner overlay is added
     * to round the pocket — without it, the inside of an L-bend reads as
     * a hard 90° angle instead of a proper rocky lip.
     */
    public void appendLayers(int mask, List<String> sink, int worldX, int worldY) {
        sink.add("cliff" + variant(mask));

        boolean nWall  = ConnectionBitmask.has(mask, ConnectionBitmask.N);
        boolean eWall  = ConnectionBitmask.has(mask, ConnectionBitmask.E);
        boolean sWall  = ConnectionBitmask.has(mask, ConnectionBitmask.S);
        boolean wWall  = ConnectionBitmask.has(mask, ConnectionBitmask.W);
        boolean neWall = ConnectionBitmask.has(mask, ConnectionBitmask.NE);
        boolean seWall = ConnectionBitmask.has(mask, ConnectionBitmask.SE);
        boolean swWall = ConnectionBitmask.has(mask, ConnectionBitmask.SW);
        boolean nwWall = ConnectionBitmask.has(mask, ConnectionBitmask.NW);

        // Inner-corner pockets: fully cardinally enclosed but a diagonal is
        // open. The opposite outer-corner sprite drawn on top frames the
        // pocket with the correct rocky lip.
        if (nWall && eWall && sWall && wWall) {
            if (!nwWall) sink.add("cliff9"); // pocket NW → SE-style lip
            if (!neWall) sink.add("cliff7"); // pocket NE → SW-style lip
            if (!swWall) sink.add("cliff3"); // pocket SW → NE-style lip
            if (!seWall) sink.add("cliff1"); // pocket SE → NW-style lip
        }
    }

    private int variant(int mask) {
        boolean nWall = ConnectionBitmask.has(mask, ConnectionBitmask.N);
        boolean eWall = ConnectionBitmask.has(mask, ConnectionBitmask.E);
        boolean sWall = ConnectionBitmask.has(mask, ConnectionBitmask.S);
        boolean wWall = ConnectionBitmask.has(mask, ConnectionBitmask.W);

        // Mirrors CliffConnectingSprite — wall means "neighbour is connected".
        if (!nWall && wWall && eWall)              return 2;  // open N: top edge
        if ( nWall && eWall && !sWall && !wWall)   return 7;  // open S+W: SW corner
        if ( nWall && wWall && !sWall && !eWall)   return 9;  // open S+E: SE corner
        if ( sWall && eWall && !nWall && !wWall)   return 1;  // open N+W: NW corner
        if ( sWall && wWall && !nWall && !eWall)   return 3;  // open N+E: NE corner
        if (!sWall && eWall && wWall)              return 8;  // open S: bottom edge
        if (!wWall && nWall && sWall)              return 4;  // open W: left edge
        if (!eWall && nWall && sWall)              return 6;  // open E: right edge
        return 0; // fully enclosed (no cliff overlay; body shows through)
    }
}
