package resources.domain.ai;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.object.Boat;
import resources.domain.tile.Tile;
import resources.world.placement.TileRules;

/**
 * Turns "sail toward point P" into a single water-legal step. Heads straight at
 * the goal when that keeps the whole hull on water; otherwise it follows the
 * coastline. The key is <b>detour hysteresis</b>: a purely greedy fan (re-pick
 * the best heading from scratch every tick) oscillates violently at concave
 * shorelines — +45° one tick, −45° the next — so the ship jitters in place and
 * never rounds the obstacle. Instead, once we're forced to turn we <i>commit</i>
 * to that turn direction (port or starboard) for a short window and sweep that
 * side, so the ship wall-follows around the land instead of flip-flopping.
 *
 * Stateful (commit sign + remaining commit ticks). No A*; this is local
 * coastline-hugging steering shared by every ShipGoal. Genuinely unreachable
 * goals are the pilot's problem — it abandons a waypoint that makes no progress.
 */
public final class WaterNavigator {

    /** Detour offsets swept (degrees off the straight bearing), nearest first. */
    private static final double[] DETOUR_MAGNITUDES = { 45, 60, 30, 90, 75, 120, 105, 150 };
    /** How long (ticks) to keep following the committed turn side once we turn. */
    private static final int COMMIT_TICKS = 90;

    private final double stepPixels;
    /** +1 = keep turning one way, -1 the other, 0 = no active detour. */
    private int commitSign;
    private int commitTicks;

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

        // 1. Straight ahead is clear → take it and drop any detour commitment.
        if (tryMove(host, cx, cy, baseAngle, ctx)) {
            commitSign = 0;
            commitTicks = 0;
            return true;
        }

        // 2. Blocked. If we're mid-detour, keep sweeping the SAME side first so
        //    we wall-follow around the obstacle instead of oscillating.
        if (commitTicks > 0 && commitSign != 0) {
            commitTicks--;
            for (double mag : DETOUR_MAGNITUDES) {
                double a = baseAngle + Math.toRadians(commitSign * mag);
                if (tryMove(host, cx, cy, a, ctx)) return true;
            }
        }

        // 3. No commitment (or the committed side is fully blocked) — probe both
        //    sides, nearest offsets first, and commit to the first that's legal.
        for (double mag : DETOUR_MAGNITUDES) {
            for (int sign : new int[] { +1, -1 }) {
                double a = baseAngle + Math.toRadians(sign * mag);
                if (tryMove(host, cx, cy, a, ctx)) {
                    commitSign = sign;
                    commitTicks = COMMIT_TICKS;
                    return true;
                }
            }
        }
        return false; // penned in on all sides — hold this tick.
    }

    /** Try one heading; if the hull stays on water, move + face and return true. */
    private boolean tryMove(BaseEntity host, double cx, double cy, double angle, GameContext ctx) {
        double nx = cx + Math.cos(angle) * stepPixels;
        double ny = cy + Math.sin(angle) * stepPixels;
        if (!hullOnWater(host, nx, ny, ctx)) return false;
        host.setWorldX(nx);
        host.setWorldY(ny);
        host.getHitBox().updateCoords();
        if (host instanceof Boat) ((Boat) host).faceToward(Math.cos(angle), Math.sin(angle));
        return true;
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
