package resources.domain.spawn;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import resources.app.GameContext;
import resources.domain.ai.ShipPilotBehavior;
import resources.domain.entity.component.AIComponent;
import resources.domain.object.Boat;
import resources.domain.ship.ShipKind;
import resources.domain.ship.ShipKindRegistry;
import resources.domain.tile.Tile;
import resources.world.placement.TileRules;

/**
 * Scatters a small mixed fleet of NPC ships over open water near a point. Each
 * ship is built from its {@link ShipKind}, given that kind's default goal and a
 * {@link ShipPilotBehavior}, then placed via {@code placeShipOnWater} (water
 * tiles are solid, so the generic placeEntity gate would reject them). A no-op
 * when no water is within range; larger hulls that don't fit are skipped.
 */
public final class ShipSpawner {

    private static final int SCAN_RADIUS_TILES = 20;
    private static final int MIN_SEPARATION_PX = 6 * 64;

    private ShipSpawner() {}

    /** Spawn one pirate, one fisher, and one galleon near {@code center} when
     *  water allows. */
    public static void spawnFleetNear(GameContext ctx, Point center) {
        if (ctx == null || center == null) return;
        List<Point> water = findWater(ctx, center);
        if (water.isEmpty()) return;
        Collections.shuffle(water, new Random(center.x * 73856093L ^ center.y * 19349663L));

        // A varied mix: civilians, a fisher, a merchant, a pirate hunter, and a
        // boardable flagship — one of each so the world reads as populated.
        ShipKind[] fleet = {
            ShipKindRegistry.DINGHY,
            ShipKindRegistry.ROWBOAT,
            ShipKindRegistry.FISHER,
            ShipKindRegistry.CREWBOAT,
            ShipKindRegistry.PIRATE_BRIG,
            ShipKindRegistry.GALLEON
        };
        List<Point> used = new ArrayList<>();
        for (ShipKind kind : fleet) {
            Point spot = pickSpaced(water, used);
            if (spot == null) break;
            Boat ship = new Boat(ctx.player().panel, kind, spot.x, spot.y, false);
            ship.addComponent(new AIComponent(ctx,
                new ShipPilotBehavior(kind.newGoal(ctx))));
            if (ctx.world().placeShipOnWater(ship)) {
                used.add(spot);
            }
        }
    }

    private static Point pickSpaced(List<Point> candidates, List<Point> used) {
        for (Point p : candidates) {
            boolean ok = true;
            for (Point u : used) {
                if (p.distance(u) < MIN_SEPARATION_PX) { ok = false; break; }
            }
            if (ok && !used.contains(p)) return p;
        }
        return null;
    }

    private static List<Point> findWater(GameContext ctx, Point center) {
        int ts = ctx.tileSize();
        int radius = SCAN_RADIUS_TILES * ts;
        List<Point> out = new ArrayList<>();
        for (int dy = -radius; dy <= radius; dy += ts)
            for (int dx = -radius; dx <= radius; dx += ts) {
                Point w = new Point(center.x + dx, center.y + dy);
                Tile t = ctx.world().getTile(w);
                if (t != null && TileRules.isWater(t.getName())) out.add(w);
            }
        return out;
    }
}
