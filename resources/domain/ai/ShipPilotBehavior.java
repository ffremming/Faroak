package resources.domain.ai;

import java.awt.Point;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.object.Boat;
import resources.domain.ship.Faction;
import resources.domain.ship.ShipKind;

/**
 * The single behaviour driving an NPC ship. Layered:
 *   - Goal (the purpose): emits the next waypoint; WaterNavigator sails there.
 *   - Provoked reaction: a struck neutral ship flees the hit source for a while
 *     (FLEE) or treats the attacker as a combat target (RETALIATE).
 *   - Hostile override: if armed and a target sits within detection range, the
 *     goal is suspended; the ship pursues to firing range and fires its
 *     broadside when the target is roughly abeam. When the target dies or
 *     escapes, the goal resumes from where it left off (goals are stateful).
 */
public final class ShipPilotBehavior implements AIBehavior {

    private static final double DETECT_RADIUS_TILES = 12.0;
    private static final int    TARGET_RESCAN_TICKS = 10;
    private static final int    WAYPOINT_REACH_PX   = 48;
    /** Fire when the target's bearing is within this many degrees of abeam. */
    private static final double ABEAM_TOLERANCE_DEG = 35.0;
    private static final int    FLEE_DURATION_TICKS = 180;

    private final ShipGoal goal;
    private WaterNavigator navigator;
    private Boat target;
    private int rescanCooldown;

    // Provoked-reaction state.
    private Point provokeSource;
    private int   fleeTicks;
    private Boat  retaliateTarget;

    public ShipPilotBehavior(ShipGoal goal) { this.goal = goal; }

    /** Notify the pilot that the host was hit from {@code (srcX, srcY)} by an
     *  attacker (may be null). Drives FLEE / RETALIATE reactions per kind. */
    public void onAttacked(Boat self, double srcX, double srcY, Boat attacker) {
        ShipKind kind = self.kind();
        if (kind == null) return;
        switch (kind.reaction()) {
            case FLEE:
                provokeSource = new Point((int) srcX, (int) srcY);
                fleeTicks = FLEE_DURATION_TICKS;
                break;
            case RETALIATE:
                retaliateTarget = attacker;
                break;
            case IGNORE:
            default:
                break;
        }
    }

    @Override
    public void tick(BaseEntity host, GameContext ctx) {
        if (!(host instanceof Boat)) return;
        Boat ship = (Boat) host;
        if (ship.isRidden() || ship.isDestroyed()) return; // player took the helm / sunk
        ShipKind kind = ship.kind();
        if (kind == null) return;
        if (navigator == null) navigator = new WaterNavigator(kind.speed());

        Faction faction = kind.faction();
        boolean armed = kind.loadout() != null && kind.loadout().armed();

        // --- provoked flee: sail directly away from the hit source ---
        if (fleeTicks > 0 && provokeSource != null) {
            fleeTicks--;
            double awayX = ship.getWorldX() - (provokeSource.x - ship.getWorldX());
            double awayY = ship.getWorldY() - (provokeSource.y - ship.getWorldY());
            navigator.stepToward(ship, new Point((int) awayX, (int) awayY), ctx);
            return;
        }

        // --- provoked retaliation: fight the attacker if we're armed ---
        if (retaliateTarget != null && !retaliateTarget.isDestroyed() && armed) {
            engage(ship, retaliateTarget, kind, ctx);
            return;
        }
        if (retaliateTarget != null && retaliateTarget.isDestroyed()) retaliateTarget = null;

        // --- target acquisition (throttled) ---
        if (armed) {
            if (rescanCooldown-- <= 0) {
                rescanCooldown = TARGET_RESCAN_TICKS;
                double radius = DETECT_RADIUS_TILES * ctx.tileSize();
                target = ShipTargeting.findTarget(ship, faction, radius, ctx);
            }
            if (target != null && target.isDestroyed()) target = null;
        }

        // --- hostile override ---
        if (armed && target != null) {
            engage(ship, target, kind, ctx);
            return;
        }

        // --- goal layer ---
        if (goal == null) return;
        Point wp = goal.currentWaypoint(ship, ctx);
        if (wp == null) return;
        double dx = wp.x - ship.getWorldX();
        double dy = wp.y - ship.getWorldY();
        if (Math.hypot(dx, dy) <= WAYPOINT_REACH_PX) {
            goal.onWaypointReached(ship, ctx);
            return;
        }
        navigator.stepToward(ship, wp, ctx);
    }

    /** Close to firing range, then fire when the target is abeam. */
    private void engage(Boat ship, Boat prey, ShipKind kind, GameContext ctx) {
        double fireRangePx = kind.loadout().rangeTiles() * ctx.tileSize() * 0.85;
        double dx = prey.getWorldX() - ship.getWorldX();
        double dy = prey.getWorldY() - ship.getWorldY();
        double dist = Math.hypot(dx, dy);

        if (dist > fireRangePx) {
            navigator.stepToward(ship, prey.getPoint(), ctx);
            return;
        }
        // In range: present a broadside. If already roughly abeam, fire;
        // otherwise nudge perpendicular to the bearing to line up the flank.
        if (isAbeam(ship, dx, dy)) {
            ship.fireBroadside();
        } else {
            double perpX = -dy, perpY = dx;
            double len = Math.hypot(perpX, perpY);
            if (len > 0.0001) {
                int wx = (int) (ship.getWorldX() + perpX / len * 64);
                int wy = (int) (ship.getWorldY() + perpY / len * 64);
                navigator.stepToward(ship, new Point(wx, wy), ctx);
            }
        }
    }

    /** Is the bearing to the target within tolerance of the ship's beam (±90° of facing)? */
    private boolean isAbeam(Boat ship, double dx, double dy) {
        int[] f = Boat.directionVectorForIndex(ship.facingIndexPublic());
        double fa = Math.atan2(f[1], f[0]);
        double ta = Math.atan2(dy, dx);
        double rel = Math.toDegrees(Math.abs(angleDiff(ta, fa)));
        double offBeam = Math.abs(rel - 90.0);
        return offBeam <= ABEAM_TOLERANCE_DEG;
    }

    private static double angleDiff(double a, double b) {
        double d = a - b;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return d;
    }
}
