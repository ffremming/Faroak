package resources.net.multiplayer.server.persistence;

/**
 * Chooses a persistence backend from config.
 */
public final class PersistenceStoreFactory {

    private PersistenceStoreFactory() {}

    public static PersistenceStore createDefault(String sqlitePath) {
        String required = System.getProperty("game.multiplayer.sqlite.required", "false");
        boolean hardFail = "true".equalsIgnoreCase(required);
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + (sqlitePath == null || sqlitePath.isBlank() ? "multiplayer.db" : sqlitePath);
            return new SqlitePersistenceStore(url);
        } catch (Exception ex) {
            if (hardFail) throw new IllegalStateException("SQLite driver unavailable", ex);
            return new InMemoryPersistenceStore();
        }
    }
}
