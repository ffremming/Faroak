package resources.net.multiplayer;

/**
 * Runtime networking role for this process.
 */
public enum MultiplayerMode {
    OFFLINE,
    CLIENT,
    HOST;

    public static MultiplayerMode parse(String raw) {
        if (raw == null) return OFFLINE;
        String value = raw.trim().toLowerCase();
        if ("client".equals(value)) return CLIENT;
        if ("host".equals(value) || "server".equals(value)) return HOST;
        return OFFLINE;
    }
}
