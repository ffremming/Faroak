package resources.net;

/**
 * Shared baseline settings for online play.
 */
public final class MultiplayerSettings {

    public static final int MAX_PLAYERS = 10;
    public static final int SERVER_TICK_RATE = 30;
    public static final int SNAPSHOT_RATE = 20;
    public static final int PROTOCOL_VERSION = 1;
    public static final int INTERPOLATION_DELAY_MS = 120;
    public static final double SERVER_MOVE_SPEED_PER_TICK = 2.0;
    public static final double SERVER_ACTION_RANGE = 128.0;
    public static final double SERVER_INTEREST_RADIUS = 2048.0;
    public static final String SQLITE_PATH = "multiplayer.db";

    private MultiplayerSettings() {}
}
