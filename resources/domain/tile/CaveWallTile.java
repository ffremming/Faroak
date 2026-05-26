package resources.domain.tile;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;
import resources.domain.tile.connect.ConnectionBitmask;
import resources.domain.tile.connect.ConnectionRule;

/**
 * Solid cave wall tile that picks its sprite variant from the N/E/S/W
 * connection bitmask of adjacent cave walls. Reuses the existing
 * {@code rockCliff0..rockCliff9} atlas so the cave gets visually distinct
 * rock walls with proper corners without any new art.
 *
 * The connection rule asks the neighbour "are you also a CaveWallTile?" so
 * floor tiles never count as wall connections, and conversely the floor tiles
 * never trigger cliff borders against this tile (they look at altitude, which
 * we deliberately keep different).
 *
 * Variant table mirrors {@link CliffConnectingSprite}'s mapping but is
 * inlined here so we can swap the atlas (cliff → rockCliff) without changing
 * the cliff renderer used by the overworld.
 */
public class CaveWallTile extends Tile {

    private static final ConnectionRule WALL_RULE = (self, n) -> n instanceof CaveWallTile;

    public CaveWallTile(GamePanel panel, int worldX, int worldY, int altitude) {
        super(panel, "rockCliff0", worldX, worldY, altitude);
        this.solid = true;
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        if (images.isEmpty()) populateImages();
        return images;
    }

    /** Deep-interior variants — rockCliff0/11/12/13/14 all exist as PNGs and
     *  read as "chunks of rock" so we cycle through them by world-coord hash
     *  to break up the visual monotony in big wall regions. */
    private static final String[] INTERIOR_VARIANTS = {
        "rockCliff0", "rockCliff11", "rockCliff12", "rockCliff13", "rockCliff14"
    };

    private void populateImages() {
        images.clear();
        int mask = ConnectionBitmask.cardinal(this, getNeighbors(), WALL_RULE);
        images.add(panel.imageContainer.getTileImage(variantSprite(mask, (int) worldX, (int) worldY)));
    }

    /**
     * Choose the rockCliff variant. The overworld cliff atlas is keyed on
     * "which direction is the cliff edge facing" — for caves we want the
     * opposite: "which direction is the OPEN side" (i.e. neighbours that are
     * NOT walls). Inverting the mask before lookup gives us the right corner
     * pieces while keeping the atlas filenames stable.
     *
     * Fully-enclosed walls (the common case in big rock regions) get a hashed
     * pick from {@link #INTERIOR_VARIANTS} so the deep wall doesn't show as
     * the same sprite for every tile — visual variety without per-tile state.
     */
    private static String variantSprite(int mask, int worldX, int worldY) {
        boolean n = !ConnectionBitmask.has(mask, ConnectionBitmask.N);
        boolean e = !ConnectionBitmask.has(mask, ConnectionBitmask.E);
        boolean s = !ConnectionBitmask.has(mask, ConnectionBitmask.S);
        boolean w = !ConnectionBitmask.has(mask, ConnectionBitmask.W);

        // No open sides: fully enclosed wall — pick from the interior pool.
        if (!n && !e && !s && !w) return INTERIOR_VARIANTS[interiorIndex(worldX, worldY)];
        // Single open side → straight edge facing that direction.
        if (n && !e && !s && !w)  return "rockCliff2"; // top edge open (floor to the north)
        if (s && !e && !n && !w)  return "rockCliff8"; // bottom edge open
        if (w && !n && !e && !s)  return "rockCliff4"; // left edge open
        if (e && !n && !s && !w)  return "rockCliff6"; // right edge open
        // Two adjacent open sides → outer corner.
        if (n && e && !s && !w)   return "rockCliff3"; // open N+E → NE outer corner
        if (n && w && !s && !e)   return "rockCliff1"; // open N+W → NW outer corner
        if (s && e && !n && !w)   return "rockCliff9"; // open S+E → SE outer corner
        if (s && w && !n && !e)   return "rockCliff7"; // open S+W → SW outer corner
        // Three or four open sides (isolated peninsulas / pillars): still
        // pick from the interior pool so isolated pillars also vary.
        return INTERIOR_VARIANTS[interiorIndex(worldX, worldY)];
    }

    /** Deterministic [0, INTERIOR_VARIANTS.length) hash of (worldX, worldY). */
    private static int interiorIndex(int worldX, int worldY) {
        int h = worldX * 73856093 ^ worldY * 19349663;
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        return Math.floorMod(h, INTERIOR_VARIANTS.length);
    }
}
