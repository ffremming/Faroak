package resources.domain.spawn;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import resources.app.GameContext;
import resources.domain.object.Boat;
import resources.domain.tile.Tile;
import resources.world.placement.TileRules;

/**
 * One-shot static helper that scatters boats over water near a center point.
 * Scans a square area around the center, collects water tiles, shuffles them,
 * and places up to {@code count} boats on distinct cells. A no-op if no water
 * is found within the scan radius — better than silently spawning a boat in a
 * grass field.
 *
 * The radius/step constants are tuned for the current 64px tile grid; if the
 * tile size shifts, this should read it from {@code ctx.tileSize()}.
 */
public final class BoatSpawner {

    private static final int SCAN_RADIUS_TILES = 16;

    private BoatSpawner() {}

    /**
     * Place up to {@code count} boats on water tiles within
     * {@code SCAN_RADIUS_TILES} of {@code center}. Returns silently if no
     * water is found in range.
     */
    public static void spawnBoatsNear(GameContext ctx, Point center, int count) {
        if (ctx == null || center == null || count <= 0) return;

        List<Point> waterTiles = findWaterTiles(ctx, center);
        if (waterTiles.isEmpty()) return;

        Collections.shuffle(waterTiles, new Random(center.x * 73856093L ^ center.y * 19349663L));

        int placed = 0;
        for (Point p : waterTiles) {
            if (placed >= count) break;
            // Pre-flight the placement so we don't build a Boat that will be
            // rejected — under crowded shorelines this used to construct ~30×
            // more boats than it kept.
            if (!Boat.canPlaceAt(ctx, p.x, p.y)) continue;
            Boat boat = new Boat(ctx.player().panel, p.x, p.y);
            if (ctx.world().placeEntity(boat)) placed++;
        }
    }

    /**
     * Walk the tile grid around {@code center} and collect every "ocean" or
     * "river" cell. Returns world-space tile-origin points usable as boat
     * spawn coordinates.
     */
    private static List<Point> findWaterTiles(GameContext ctx, Point center) {
        int tileSize = ctx.tileSize();
        int radiusPx = SCAN_RADIUS_TILES * tileSize;
        List<Point> out = new ArrayList<>();

        for (int dy = -radiusPx; dy <= radiusPx; dy += tileSize) {
            for (int dx = -radiusPx; dx <= radiusPx; dx += tileSize) {
                Point world = new Point(center.x + dx, center.y + dy);
                Tile tile = ctx.world().getTile(world);
                if (tile == null) continue;
                String name = tile.getName();
                if (TileRules.isWater(name)) out.add(world);
            }
        }
        return out;
    }
}
