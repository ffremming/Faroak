package resources.domain.spawn;

import java.awt.Point;
import java.util.List;
import java.util.Random;

import resources.app.GameContext;
import resources.app.GamePanel;
import resources.core.time.GameClock;
import resources.domain.mob.Mob;
import resources.domain.tile.Tile;
import resources.geometry.HitBox;

/**
 * Orchestrates mob spawning for a chunk-sized neighbourhood. Asks each
 * {@link SpawnRule} whether the current clock phase qualifies, rolls density,
 * picks a candidate tile, validates it (solid/water rejected), and hands the
 * mob to {@link resources.world.WorldRuntime#placeEntity(resources.domain.entity.BaseEntity)}.
 *
 * Deliberately stateless — gameplay code decides how often to invoke it
 * (typically every N real-time seconds, or on chunk activation). Mobs persist
 * via the chunk's entity list, so we don't keep our own bookkeeping.
 */
public final class MobSpawnService {

    /** Radius (in pixels) around chunkCenter we'll consider when placing mobs. */
    private static final int SPAWN_RADIUS_PX = 12 * 64;
    /** How many candidate tiles to sample before giving up on a single roll. */
    private static final int PLACEMENT_ATTEMPTS = 12;

    public void tryPopulateChunk(GameContext ctx, Point chunkCenter, List<SpawnRule> rules) {
        if (ctx == null || chunkCenter == null || rules == null || rules.isEmpty()) return;
        GameClock.Phase phase = ctx.clock().phase();
        GamePanel panel = (GamePanel) ctx; // GamePanel implements GameContext

        for (SpawnRule rule : rules) {
            if (!rule.matchesPhase(phase)) continue;
            Random rng = rngFor(chunkCenter, phase, rule);
            if (rng.nextDouble() >= rule.density) continue;
            Point placement = findPlacementTile(ctx, chunkCenter, rng);
            if (placement == null) continue;
            Mob mob = rule.factory.apply(panel, placement);
            if (mob == null) continue;
            ctx.world().placeEntity(mob);
        }
    }

    /** Hashed seed: same chunk/phase/rule combo rolls reproducibly per tick boundary. */
    private static Random rngFor(Point chunkCenter, GameClock.Phase phase, SpawnRule rule) {
        long seed = ((long) chunkCenter.x * 73856093L) ^ ((long) chunkCenter.y * 19349663L)
                  ^ ((long) phase.ordinal() * 83492791L) ^ rule.mobName.hashCode()
                  ^ System.nanoTime();
        return new Random(seed);
    }

    private Point findPlacementTile(GameContext ctx, Point center, Random rng) {
        for (int i = 0; i < PLACEMENT_ATTEMPTS; i++) {
            int dx = rng.nextInt(SPAWN_RADIUS_PX * 2) - SPAWN_RADIUS_PX;
            int dy = rng.nextInt(SPAWN_RADIUS_PX * 2) - SPAWN_RADIUS_PX;
            Point candidate = new Point(center.x + dx, center.y + dy);
            if (isValidPlacement(ctx, candidate)) return candidate;
        }
        return null;
    }

    private boolean isValidPlacement(GameContext ctx, Point worldPoint) {
        Tile tile = ctx.world().getTile(worldPoint);
        if (tile == null) return false;
        if (tile.isSolid()) return false;
        String name = tile.getName();
        // Treat any water-flavored tile as off-limits to land mobs.
        if (name != null && (name.contains("ocean") || name.contains("river") || name.contains("water"))) {
            return false;
        }
        // Probe a small footprint so we don't drop a mob inside a tree.
        HitBox probe = new HitBox(worldPoint.x, worldPoint.y, 32, 48);
        return !ctx.world().solidCollision(probe);
    }
}
