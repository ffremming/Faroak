package resources.net.multiplayer.server;

import resources.generation.biome.Biome;
import resources.generation.noise.ProceduralGen;
import resources.world.placement.TileRules;

/**
 * Lightweight terrain classifier used by the authoritative multiplayer server.
 *
 * The current server loop does not host the full world/chunk simulation, so we
 * resolve water/land directly from the shared procedural seed to keep movement
 * and placement from diverging from client rules.
 */
public final class ServerTerrainRules {

    private static final long DEFAULT_MULTIPLAYER_SEED = 424242L;
    private static final int TILE_SIZE = 64;

    // Playable hitbox constants mirror GenerationManager#initiate() + Playable
    // constructor wiring:
    // width=48, height=96, hitbox=36x32, relative=(6,64)
    private static final int PLAYER_HITBOX_W = 36;
    private static final int PLAYER_HITBOX_H = 32;
    private static final int PLAYER_HITBOX_RX = 6;
    private static final int PLAYER_HITBOX_RY = 64;

    // Boat footprint mirrors Boat constants (center-based authoritative state):
    // sprite=192x192, hitbox=144x144 centered in sprite -> rel=(24,24)
    private static final int BOAT_W = 192;
    private static final int BOAT_H = 192;
    private static final int BOAT_HITBOX_W = 144;
    private static final int BOAT_HITBOX_H = 144;
    private static final int BOAT_HITBOX_RX = (BOAT_W - BOAT_HITBOX_W) / 2;
    private static final int BOAT_HITBOX_RY = (BOAT_H - BOAT_HITBOX_H) / 2;

    private final ProceduralGen terrain;

    public ServerTerrainRules() {
        this.terrain = new ProceduralGen(resolveWorldSeed());
    }

    public boolean canPlayerOccupy(double playerWorldX, double playerWorldY) {
        int minX = (int) Math.floor(playerWorldX + PLAYER_HITBOX_RX);
        int minY = (int) Math.floor(playerWorldY + PLAYER_HITBOX_RY);
        int maxX = minX + PLAYER_HITBOX_W - 1;
        int maxY = minY + PLAYER_HITBOX_H - 1;
        return !isWaterAtPixel(minX, minY)
            && !isWaterAtPixel(minX, maxY)
            && !isWaterAtPixel(maxX, minY)
            && !isWaterAtPixel(maxX, maxY);
    }

    public boolean canPlaceObject(String objectType, double centerX, double centerY) {
        if (objectType == null || objectType.isBlank()) return false;
        if ("boat".equalsIgnoreCase(objectType)) {
            return canPlaceBoat(centerX, centerY);
        }
        return !isWaterAt(centerX, centerY);
    }

    public boolean isWaterAt(double worldX, double worldY) {
        return isWaterAtPixel((int) Math.floor(worldX), (int) Math.floor(worldY));
    }

    private boolean canPlaceBoat(double centerX, double centerY) {
        double worldX = centerX - (BOAT_W / 2.0);
        double worldY = centerY - (BOAT_H / 2.0);
        int minX = (int) Math.floor(worldX + BOAT_HITBOX_RX);
        int minY = (int) Math.floor(worldY + BOAT_HITBOX_RY);
        int maxX = minX + BOAT_HITBOX_W - 1;
        int maxY = minY + BOAT_HITBOX_H - 1;
        return isWaterAtPixel(minX, minY)
            && isWaterAtPixel(minX, maxY)
            && isWaterAtPixel(maxX, minY)
            && isWaterAtPixel(maxX, maxY);
    }

    private boolean isWaterAtPixel(int worldX, int worldY) {
        // Match client chunk generation semantics: classify terrain at tile-grid
        // origins, not arbitrary pixel positions. Sampling per pixel can classify
        // differently near shoreline boundaries and causes online/offline drift.
        int tileWorldX = Math.floorDiv(worldX, TILE_SIZE) * TILE_SIZE;
        int tileWorldY = Math.floorDiv(worldY, TILE_SIZE) * TILE_SIZE;
        Biome biome = terrain.biomeAt(tileWorldX, tileWorldY);
        String tileName = biome == null ? null : biome.tileName;
        return TileRules.isWater(tileName);
    }

    private static long resolveWorldSeed() {
        String configured = System.getProperty("game.world.seed", "").trim();
        if (!configured.isBlank()) {
            try { return Long.parseLong(configured); }
            catch (NumberFormatException ignored) {}
        }
        return DEFAULT_MULTIPLAYER_SEED;
    }
}
