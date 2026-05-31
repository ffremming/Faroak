package resources.net.multiplayer.server.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite persistence implementation.
 */
public final class SqlitePersistenceStore implements PersistenceStore {

    private final Connection conn;

    public SqlitePersistenceStore(String jdbcUrl) throws SQLException {
        this.conn = DriverManager.getConnection(jdbcUrl);
        this.conn.setAutoCommit(true);
        initSchema();
    }

    private void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                "player_id TEXT PRIMARY KEY, world_x REAL, world_y REAL, velocity_x REAL, velocity_y REAL, last_sequence INTEGER)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS world_chunks (" +
                "chunk_key INTEGER PRIMARY KEY, snapshot BLOB)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS server_meta (" +
                "meta_key TEXT PRIMARY KEY, meta_value TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS session_events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, server_tick INTEGER, player_id TEXT, event_type TEXT, payload TEXT)");
        }
    }

    @Override
    public synchronized Optional<PersistedPlayer> loadPlayer(String playerId) {
        String sql = "SELECT world_x, world_y, velocity_x, velocity_y, last_sequence FROM players WHERE player_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new PersistedPlayer(playerId,
                    rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4), rs.getLong(5)));
            }
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] loadPlayer failed for " + playerId + ": " + e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void savePlayer(PersistedPlayer player) {
        if (player == null || player.playerId.isBlank()) return;
        String sql = "INSERT INTO players(player_id, world_x, world_y, velocity_x, velocity_y, last_sequence) " +
            "VALUES(?,?,?,?,?,?) ON CONFLICT(player_id) DO UPDATE SET " +
            "world_x=excluded.world_x, world_y=excluded.world_y, velocity_x=excluded.velocity_x, " +
            "velocity_y=excluded.velocity_y, last_sequence=excluded.last_sequence";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.playerId);
            ps.setDouble(2, player.worldX);
            ps.setDouble(3, player.worldY);
            ps.setDouble(4, player.velocityX);
            ps.setDouble(5, player.velocityY);
            ps.setLong(6, player.lastSequence);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] savePlayer failed for " + player.playerId + ": " + e);
        }
    }

    @Override
    public synchronized Optional<byte[]> loadWorldChunk(long chunkKey) {
        String sql = "SELECT snapshot FROM world_chunks WHERE chunk_key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, chunkKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.ofNullable(rs.getBytes(1));
            }
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] loadWorldChunk failed for key " + chunkKey + ": " + e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void saveWorldChunk(long chunkKey, byte[] snapshotBytes) {
        String sql = "INSERT INTO world_chunks(chunk_key, snapshot) VALUES(?,?) " +
            "ON CONFLICT(chunk_key) DO UPDATE SET snapshot=excluded.snapshot";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, chunkKey);
            ps.setBytes(2, snapshotBytes == null ? new byte[0] : snapshotBytes);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] saveWorldChunk failed for key " + chunkKey + ": " + e);
        }
    }

    @Override
    public synchronized List<Long> listWorldChunkKeys() {
        ArrayList<Long> keys = new ArrayList<>();
        String sql = "SELECT chunk_key FROM world_chunks ORDER BY chunk_key";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) keys.add(rs.getLong(1));
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] listWorldChunkKeys failed: " + e);
        }
        return keys;
    }

    @Override
    public synchronized void putMeta(String key, String value) {
        if (key == null || key.isBlank()) return;
        String sql = "INSERT INTO server_meta(meta_key, meta_value) VALUES(?,?) " +
            "ON CONFLICT(meta_key) DO UPDATE SET meta_value=excluded.meta_value";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value == null ? "" : value);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] putMeta failed for key " + key + ": " + e);
        }
    }

    @Override
    public synchronized String getMeta(String key, String fallback) {
        String sql = "SELECT meta_value FROM server_meta WHERE meta_key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return fallback;
                return rs.getString(1);
            }
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] getMeta failed for key " + key + ": " + e);
            return fallback;
        }
    }

    @Override
    public synchronized void appendSessionEvent(long serverTick, String playerId, String eventType, String payload) {
        String sql = "INSERT INTO session_events(server_tick, player_id, event_type, payload) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, serverTick);
            ps.setString(2, playerId == null ? "" : playerId);
            ps.setString(3, eventType == null ? "" : eventType);
            ps.setString(4, payload == null ? "" : payload);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] appendSessionEvent failed for player " + playerId + " event " + eventType + ": " + e);
        }
    }

    @Override
    public synchronized void checkpoint() {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA wal_checkpoint(TRUNCATE)");
        } catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] checkpoint failed: " + e);
        }
    }

    @Override
    public synchronized void close() {
        try { conn.close(); }
        catch (SQLException e) {
            System.err.println("[SqlitePersistenceStore] close failed: " + e);
        }
    }
}
