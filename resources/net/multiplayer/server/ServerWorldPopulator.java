package resources.net.multiplayer.server;

import java.util.Random;

/**
 * Deterministically seeds a fresh authoritative world with harvestable objects
 * (trees, rocks, ore) so a dedicated server hosts a shared, harvestable world
 * rather than a blank map. Positions derive from the shared world seed and skip
 * water, so they sit on the same land clients render from that seed.
 *
 * <p>Run once, only when persistence restored nothing (guarded by a meta flag in
 * {@link AuthoritativeLobbyRuntime}).
 */
final class ServerWorldPopulator {

    private static final String[] HARVESTABLE_TYPES = { "tree", "tree", "tree", "rock", "rock", "ore" };

    private final ServerTerrainRules terrain;
    private final long seed;
    private final int radiusTiles;
    private final int tileSize;
    private final int count;

    ServerWorldPopulator(ServerTerrainRules terrain, long seed, int radiusTiles, int tileSize, int count) {
        this.terrain = terrain;
        this.seed = seed;
        this.radiusTiles = Math.max(4, radiusTiles);
        this.tileSize = Math.max(1, tileSize);
        this.count = Math.max(0, count);
    }

    /**
     * Scatter up to {@code count} harvestable objects on valid land within the
     * spawn region around (0,0). Returns the number actually placed.
     */
    int populate(AuthoritativeGameHost host, long tick) {
        Random rng = new Random(seed ^ 0x5EED_0B1EL);
        int placed = 0;
        int attempts = 0;
        int maxAttempts = count * 8;
        while (placed < count && attempts < maxAttempts) {
            attempts++;
            int tx = rng.nextInt(radiusTiles * 2 + 1) - radiusTiles;
            int ty = rng.nextInt(radiusTiles * 2 + 1) - radiusTiles;
            double x = tx * (double) tileSize;
            double y = ty * (double) tileSize;
            // Keep the immediate spawn tile clear so players don't spawn inside a tree.
            if (Math.abs(tx) <= 1 && Math.abs(ty) <= 1) continue;
            if (terrain != null && terrain.isWaterAt(x, y)) continue;
            String type = HARVESTABLE_TYPES[rng.nextInt(HARVESTABLE_TYPES.length)];
            host.seedHarvestable(type, x, y, tick);
            placed++;
        }
        return placed;
    }
}
