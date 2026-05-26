package resources.net.multiplayer;

import java.util.UUID;

import resources.net.MultiplayerSettings;

/**
 * Immutable networking configuration used to bootstrap multiplayer runtime.
 *
 * Defaults are read from system properties so hosts can switch backend or mode
 * without code changes.
 */
public final class MultiplayerConfig {

    private final MultiplayerMode mode;
    private final String backend;
    private final String playerId;
    private final int maxPlayers;
    private final int serverTickRate;
    private final int snapshotRate;
    private final int protocolVersion;
    private final int interpolationDelayMs;
    private final double serverMoveSpeedPerTick;
    private final double serverActionRange;
    private final double serverInterestRadius;
    private final String sqlitePath;

    public MultiplayerConfig(
            MultiplayerMode mode,
            String backend,
            String playerId,
            int maxPlayers,
            int serverTickRate,
            int snapshotRate,
            int protocolVersion,
            int interpolationDelayMs,
            double serverMoveSpeedPerTick,
            double serverActionRange,
            double serverInterestRadius,
            String sqlitePath) {
        this.mode = (mode == null) ? MultiplayerMode.OFFLINE : mode;
        this.backend = (backend == null || backend.isBlank()) ? "loopback" : backend;
        this.playerId = (playerId == null || playerId.isBlank()) ? newPlayerId() : playerId;
        this.maxPlayers = Math.max(1, maxPlayers);
        this.serverTickRate = Math.max(1, serverTickRate);
        this.snapshotRate = Math.max(1, snapshotRate);
        this.protocolVersion = Math.max(1, protocolVersion);
        this.interpolationDelayMs = Math.max(0, interpolationDelayMs);
        this.serverMoveSpeedPerTick = Math.max(0.1, serverMoveSpeedPerTick);
        this.serverActionRange = Math.max(1.0, serverActionRange);
        this.serverInterestRadius = Math.max(64.0, serverInterestRadius);
        this.sqlitePath = (sqlitePath == null || sqlitePath.isBlank()) ? MultiplayerSettings.SQLITE_PATH : sqlitePath;
    }

    public static MultiplayerConfig fromSystemProperties() {
        MultiplayerMode mode = MultiplayerMode.parse(System.getProperty("game.multiplayer.mode", "offline"));
        String backend = System.getProperty("game.multiplayer.backend", "loopback");
        String playerId = System.getProperty("game.multiplayer.playerId", newPlayerId());
        int maxPlayers = parseInt("game.multiplayer.maxPlayers", MultiplayerSettings.MAX_PLAYERS);
        int serverTickRate = parseInt("game.multiplayer.serverTickRate", MultiplayerSettings.SERVER_TICK_RATE);
        int snapshotRate = parseInt("game.multiplayer.snapshotRate", MultiplayerSettings.SNAPSHOT_RATE);
        int protocolVersion = parseInt("game.multiplayer.protocolVersion", MultiplayerSettings.PROTOCOL_VERSION);
        int interpolationDelayMs = parseInt("game.multiplayer.interpolationDelayMs", MultiplayerSettings.INTERPOLATION_DELAY_MS);
        double moveSpeed = parseDouble("game.multiplayer.serverMoveSpeedPerTick", MultiplayerSettings.SERVER_MOVE_SPEED_PER_TICK);
        double actionRange = parseDouble("game.multiplayer.serverActionRange", MultiplayerSettings.SERVER_ACTION_RANGE);
        double interestRadius = parseDouble("game.multiplayer.serverInterestRadius", MultiplayerSettings.SERVER_INTEREST_RADIUS);
        String sqlitePath = System.getProperty("game.multiplayer.sqlitePath", MultiplayerSettings.SQLITE_PATH);
        return new MultiplayerConfig(
            mode, backend, playerId, maxPlayers, serverTickRate, snapshotRate,
            protocolVersion, interpolationDelayMs, moveSpeed, actionRange, interestRadius, sqlitePath);
    }

    private static int parseInt(String key, int fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        try { return Integer.parseInt(raw.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static double parseDouble(String key, double fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        try { return Double.parseDouble(raw.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String newPlayerId() {
        return "p-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public MultiplayerMode mode()   { return mode; }
    public String backend()         { return backend; }
    public String playerId()        { return playerId; }
    public int maxPlayers()         { return maxPlayers; }
    public int serverTickRate()     { return serverTickRate; }
    public int snapshotRate()       { return snapshotRate; }
    public int protocolVersion()    { return protocolVersion; }
    public int interpolationDelayMs() { return interpolationDelayMs; }
    public double serverMoveSpeedPerTick() { return serverMoveSpeedPerTick; }
    public double serverActionRange() { return serverActionRange; }
    public double serverInterestRadius() { return serverInterestRadius; }
    public String sqlitePath() { return sqlitePath; }
    public boolean online()         { return mode != MultiplayerMode.OFFLINE; }
}
