package resources.net.multiplayer.server.persistence;

import java.nio.file.Path;

/**
 * Chooses a persistence backend from config.
 *
 * <p>Backend is selected by {@code -Dgame.multiplayer.persistence}:
 * <ul>
 *   <li>{@code file} (default) — durable, pure-JDK {@link JsonFilePersistenceStore}.</li>
 *   <li>{@code memory} — volatile {@link InMemoryPersistenceStore} (tests/loopback).</li>
 *   <li>{@code sqlite} — {@link SqlitePersistenceStore} if a JDBC driver is on the
 *       classpath; otherwise falls back to file (or throws if
 *       {@code -Dgame.multiplayer.sqlite.required=true}).</li>
 * </ul>
 * The data directory for the file store comes from {@code -Dgame.multiplayer.dataDir},
 * else it is derived from {@code sqlitePath} (its sibling, with any {@code .db} suffix
 * replaced by {@code -data}), else {@code multiplayer-data/}.
 */
public final class PersistenceStoreFactory {

    private PersistenceStoreFactory() {}

    public static PersistenceStore createDefault(String sqlitePath) {
        String backend = System.getProperty("game.multiplayer.persistence", "file").trim().toLowerCase();
        switch (backend) {
            case "memory":
                return new InMemoryPersistenceStore();
            case "sqlite":
                return createSqliteOrFallback(sqlitePath);
            case "file":
            default:
                return new JsonFilePersistenceStore(dataDir(sqlitePath));
        }
    }

    private static PersistenceStore createSqliteOrFallback(String sqlitePath) {
        boolean hardFail = "true".equalsIgnoreCase(
            System.getProperty("game.multiplayer.sqlite.required", "false"));
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + (sqlitePath == null || sqlitePath.isBlank() ? "multiplayer.db" : sqlitePath);
            return new SqlitePersistenceStore(url);
        } catch (Exception ex) {
            if (hardFail) throw new IllegalStateException("SQLite driver unavailable", ex);
            return new JsonFilePersistenceStore(dataDir(sqlitePath));
        }
    }

    private static Path dataDir(String sqlitePath) {
        String configured = System.getProperty("game.multiplayer.dataDir", "").trim();
        if (!configured.isEmpty()) return Path.of(configured);
        if (sqlitePath == null || sqlitePath.isBlank()) return Path.of("multiplayer-data");
        String base = sqlitePath.endsWith(".db")
            ? sqlitePath.substring(0, sqlitePath.length() - 3) + "-data"
            : sqlitePath + "-data";
        return Path.of(base);
    }
}
