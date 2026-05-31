package resources.domain.ai;

import resources.app.GameContext;
import resources.domain.entity.BaseEntity;
import resources.domain.object.Boat;
import resources.domain.ship.Faction;
import resources.domain.ship.FactionRelations;

/**
 * Finds the nearest thing a hostile ship should attack: the player's currently
 * ridden boat, or any rival-faction boat, within a detection radius. Returns
 * the target Boat (we damage boats, not the player directly — sinking the boat
 * deals with the rider). Null when nothing qualifies.
 */
public final class ShipTargeting {

    private ShipTargeting() {}

    public static Boat findTarget(Boat self, Faction selfFaction,
                                  double detectRadiusPx, GameContext ctx) {
        double r2 = detectRadiusPx * detectRadiusPx;
        Boat best = null;
        double bestD2 = r2;
        double sx = self.getWorldX(), sy = self.getWorldY();
        for (BaseEntity e : ctx.world().getEntities()) {
            if (!(e instanceof Boat) || e == self) continue;
            Boat other = (Boat) e;
            if (other.isDestroyed()) continue;
            if (!isEnemy(self, selfFaction, other)) continue;
            double d2 = sq(other.getWorldX() - sx) + sq(other.getWorldY() - sy);
            if (d2 <= bestD2) { bestD2 = d2; best = other; }
        }
        return best;
    }

    private static boolean isEnemy(Boat self, Faction selfFaction, Boat other) {
        // The player's ridden boat is a target for anyone hostile-to-player.
        if (other.isRidden() && FactionRelations.isHostileToPlayer(selfFaction)) return true;
        Faction otherFaction = other.kind() != null ? other.kind().faction() : Faction.NEUTRAL;
        return FactionRelations.isHostile(selfFaction, otherFaction);
    }

    private static double sq(double v) { return v * v; }
}
