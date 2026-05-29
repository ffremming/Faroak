package resources.world;

import java.awt.Point;

/**
 * Tile-grid math used by placement and any code that needs to translate
 * between continuous world-pixel coordinates and the 64-pixel tile grid.
 *
 * Pure functions, no state — safe to call from any thread, trivially testable.
 */
public final class WorldCoord {

    public static int snapToTileFloor(double worldCoord, int tileSize) {
        return (int) Math.floor(worldCoord / tileSize) * tileSize;
    }

    public static Point snapToTile(double worldX, double worldY, int tileSize) {
        return new Point(snapToTileFloor(worldX, tileSize),
                         snapToTileFloor(worldY, tileSize));
    }

    public static Point tileIndex(double worldX, double worldY, int tileSize) {
        return new Point((int) Math.floor(worldX / tileSize),
                         (int) Math.floor(worldY / tileSize));
    }

    public static Point centerOfTile(int tileX, int tileY, int tileSize) {
        return new Point(tileX * tileSize + tileSize / 2,
                         tileY * tileSize + tileSize / 2);
    }

    private WorldCoord() {}
}
