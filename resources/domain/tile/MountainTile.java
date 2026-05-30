package resources.domain.tile;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.tile.connect.ConnectionBitmask;

/**
 * Solid mountain / cliff tile that auto-tiles against its 8-neighbour mask
 * (cardinals + diagonals). Picks the right rockCliff variant for outer
 * corners, edges, peninsulas, and lays inner-corner overlays where a
 * concave pocket meets the rock body.
 *
 * Distinct from {@link CliffTile} (4-bit, used by the overworld for the
 * altitude cliff transitions): mountains are a first-class solid-region
 * tile and need the full 8-bit to read correctly. Distinct from
 * {@link CaveWallTile} (4-bit predecessor): kept for backward compatibility
 * while this richer version takes over the cave walls.
 *
 * Connection rule: another MountainTile counts as a mountain, and so does
 * a legacy {@link CaveWallTile} so old caves intermix cleanly. Floors never
 * count, so the open/wall classification is unambiguous.
 */
public class MountainTile extends Tile {

    /** Opaque base body — shows through anywhere the cliff overlay is
     *  transparent. slateRock is uniform stone with no directional shading,
     *  so it tiles seamlessly between adjacent wall tiles. */
    private static final String BODY = "slateRock";

    public MountainTile(GamePanel panel, int worldX, int worldY, int altitude) {
        super(panel, BODY, worldX, worldY, altitude);
        this.solid = true;
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        if (images.isEmpty()) populateImages();
        return images;
    }

    private void populateImages() {
        images.clear();
        // Body first — fully opaque so transparent regions of the cliff
        // overlay never reveal the void underneath.
        images.add(panel.images().getTileImage(BODY));

        int mask = eightBitMask();
        ArrayList<String> keys = new ArrayList<>(2);
        MountainConnectingSprite.INSTANCE.appendLayers(mask, keys, (int) worldX, (int) worldY);
        for (String key : keys) {
            // "cliff0" isn't a real sprite — emitted by the picker for fully
            // enclosed walls (no diagonals open) to mean "no overlay needed,
            // body is enough".
            if ("cliff0".equals(key)) continue;
            images.add(panel.images().getTileImage(key));
        }
    }

    /**
     * 8-bit cardinal + diagonal mask. Unknown neighbours (null — chunk not
     * yet loaded, or world edge) are treated as mountains: assuming wall
     * when we don't know stops phantom cliff overlays from rendering at
     * chunk borders deep inside a wall mass.
     */
    private int eightBitMask() {
        Tile[] n = getNeighbors();
        int mask = 0;
        if (isMountainOrUnknown(n.length > 0 ? n[0] : null)) mask |= ConnectionBitmask.N;
        if (isMountainOrUnknown(n.length > 1 ? n[1] : null)) mask |= ConnectionBitmask.E;
        if (isMountainOrUnknown(n.length > 2 ? n[2] : null)) mask |= ConnectionBitmask.S;
        if (isMountainOrUnknown(n.length > 3 ? n[3] : null)) mask |= ConnectionBitmask.W;
        if (isMountainAt(diagonalPoint(+1, -1))) mask |= ConnectionBitmask.NE;
        if (isMountainAt(diagonalPoint(+1, +1))) mask |= ConnectionBitmask.SE;
        if (isMountainAt(diagonalPoint(-1, +1))) mask |= ConnectionBitmask.SW;
        if (isMountainAt(diagonalPoint(-1, -1))) mask |= ConnectionBitmask.NW;
        return mask;
    }

    private static boolean isMountainOrUnknown(Tile t) {
        if (t == null) return true;
        return t instanceof MountainTile || t instanceof CaveWallTile;
    }

    private java.awt.Point diagonalPoint(int dx, int dy) {
        int half = width / 2;
        int x = (dx > 0) ? ((int) worldX + width * 3 / 2) : ((int) worldX - half);
        int y = (dy > 0) ? ((int) worldY + width * 3 / 2) : ((int) worldY - half);
        return new java.awt.Point(x, y);
    }

    private boolean isMountainAt(java.awt.Point p) {
        Tile t = panel.world().getTile(p);
        if (t == null) return true;
        return t instanceof MountainTile || t instanceof CaveWallTile;
    }
}
