package resources.domain.ai;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.object.Boat;
import resources.domain.tile.Tile;
import resources.world.placement.TileRules;

/**
 * Turns "sail toward point P" into a single water-legal step. Greedy and
 * stateless beyond its configured speed: head straight at the goal; if that
 * cell isn't water, fan the heading outward (±45°, ±90°, ±135°) and take the
 * first candidate that keeps the whole hull on water. If nothing is legal the
 * host holds position this tick (penned in — try again next tick). No A*; this
 * is coastline-hugging local steering, shared by every ShipGoal.
 */
public final class WaterNavigator {

    private static final double[] FAN_DEGREES = { 0, 45, -45, 90, -90, 135, -135 };

    private final double stepPixels;

    public WaterNavigator(double stepPixels) {
        this.stepPixels = Math.max(0.1, stepPixels);
    }

    /** Advance {@code host} one step toward {@code goal}. Returns true if it moved. */
    public boolean stepToward(BaseEntity host, Point goal, GameContext ctx) {
        if (goal == null) return false;
        double cx = host.getWorldX();
        double cy = host.getWorldY();
        double toX = goal.x - cx;
        double toY = goal.y - cy;
        double len = Math.hypot(toX, toY);
        if (len < stepPixels) return false; // close enough; let goal advance
        double baseAngle = Math.atan2(toY, toX);

        for (double deg : FAN_DEGREES) {
            double a = baseAngle + Math.toRadians(deg);
            double nx = cx + Math.cos(a) * stepPixels;
            double ny = cy + Math.sin(a) * stepPixels;
            if (hullOnWater(host, nx, ny, ctx)) {
                host.setWorldX(nx);
                host.setWorldY(ny);
                host.getHitBox().updateCoords();
                if (host instanceof Boat) ((Boat) host).faceToward(Math.cos(a), Math.sin(a));
                return true;
            }
        }
        return false;
    }

    /** All four hitbox corners of {@code host} at the candidate origin sit on water. */
    private static boolean hullOnWater(BaseEntity host, double originX, double originY, GameContext ctx) {
        int relX = host.getHitBox().x - (int) host.getWorldX();
        int relY = host.getHitBox().y - (int) host.getWorldY();
        int w = host.getHitBox().width;
        int h = host.getHitBox().height;
        int x0 = (int) (originX + relX);
        int y0 = (int) (originY + relY);
        int[] xs = { x0, x0 + w - 1 };
        int[] ys = { y0, y0 + h - 1 };
        for (int x : xs) for (int y : ys) {
            Tile t = ctx.world().getTile(new Point(x, y));
            if (t == null || !TileRules.isWater(t.getName())) return false;
        }
        return true;
    }
}
