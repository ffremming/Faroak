package resources.domain.tile;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.app.GamePanel;

/**
 * Tile variant whose sprite is picked from a fixed atlas of cliff edge / corner
 * pieces based on which of its four neighbours are also cliff tiles.
 *
 * The variant table mirrors the legacy hand-rolled selector; it'll be replaced
 * by the auto-connect rule engine when Phase 3.2 lands so cliffs, fences, walls
 * and bushes all share one bitmask-driven strategy.
 */
public class CliffTile extends Tile {

    public CliffTile(GamePanel panel, String name, int worldX, int worldY, int altitude) {
        super(panel, name, worldX, worldY, altitude);
    }

    @Override
    public ArrayList<BufferedImage> getImages() {
        if (images.isEmpty()) populateImages();
        return images;
    }

    private void populateImages() {
        images.clear();
        images.add(getImage());
        images.add(panel.imageContainer.getTileImage("cliff" + cliffVariant()));
    }

    /** Pick a variant index based on which neighbours are CliffTiles (N=0, E=1, S=2, W=3). */
    private int cliffVariant() {
        Tile[] n = getNeighbors();
        boolean north = n[0] instanceof CliffTile;
        boolean east  = n[1] instanceof CliffTile;
        boolean south = n[2] instanceof CliffTile;
        boolean west  = n[3] instanceof CliffTile;

        if (!north && west  && east)  return 2;  // UP edge
        if ( north && east  && !south && !west) return 7;  // corner down-left
        if ( north && west  && !south && !east) return 9;  // corner down-right
        if ( south && east  && !north && !west) return 1;  // corner upper-left
        if ( south && west  && !north && !east) return 3;  // corner upper-right
        if (!south && east  && west)  return 8;  // DOWN edge
        if (!west  && north && south) return 4;  // LEFT edge
        if (!east  && north && south) return 6;  // RIGHT edge
        return 0;
    }
}
