package resources.net.multiplayer.server;

/** Small parsing helpers shared by the authoritative server runtime. */
final class ServerParse {

    private ServerParse() {}

    static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return Long.parseLong(value.trim()); }
        catch (NumberFormatException ignored) { return fallback; } // expected: non-numeric config value falls back to default
    }
}
