package resources.net.multiplayer.server;

import java.util.Map;
import java.util.Random;

/**
 * Maintains a capped mob population near active players so the shared world feels
 * alive. Spawns goblins/spiders/deer on valid land in a ring around a random
 * active player up to {@code cap}, and despawns mobs that drift far from every
 * player. The existing {@code tickMobs} AI in {@link AuthoritativeGameHost} then
 * drives behaviour.
 */
final class ServerMobSpawner {

    private static final String[] MOB_TYPES = { "goblin", "spider", "deer", "deer" };

    private final ServerTerrainRules terrain;
    private final int cap;
    private final double spawnInnerRadius;
    private final double spawnOuterRadius;
    private final double despawnRadius;
    private final long spawnEveryTicks;
    private final Random rng;

    ServerMobSpawner(ServerTerrainRules terrain, int cap, double spawnInnerRadius,
                     double spawnOuterRadius, double despawnRadius, long spawnEveryTicks, long seed) {
        this.terrain = terrain;
        this.cap = Math.max(0, cap);
        this.spawnInnerRadius = Math.max(64.0, spawnInnerRadius);
        this.spawnOuterRadius = Math.max(this.spawnInnerRadius + 64.0, spawnOuterRadius);
        this.despawnRadius = Math.max(this.spawnOuterRadius + 128.0, despawnRadius);
        this.spawnEveryTicks = Math.max(1L, spawnEveryTicks);
        this.rng = new Random(seed ^ 0x0B5EEDL);
    }

    void tick(AuthoritativeGameHost host, Map<String, Session> sessions, long tick) {
        if (sessions.isEmpty()) return;
        host.despawnDistantMobs(sessions, despawnRadius, tick);
        if (tick % spawnEveryTicks != 0L) return;
        if (host.mobCount() >= cap) return;

        Session[] active = sessions.values().toArray(new Session[0]);
        Session anchor = active[rng.nextInt(active.length)];
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = rng.nextDouble() * Math.PI * 2.0;
            double dist = spawnInnerRadius + rng.nextDouble() * (spawnOuterRadius - spawnInnerRadius);
            double x = anchor.x + Math.cos(angle) * dist;
            double y = anchor.y + Math.sin(angle) * dist;
            if (terrain != null && terrain.isWaterAt(x, y)) continue;
            String type = MOB_TYPES[rng.nextInt(MOB_TYPES.length)];
            host.spawnMob(type, x, y, tick);
            return;
        }
    }
}
