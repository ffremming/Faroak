package resources.net.multiplayer.server.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Process-local persistence implementation for tests and local loopback.
 */
public final class InMemoryPersistenceStore implements PersistenceStore {

    private final Map<String, PersistedPlayer> players = new HashMap<>();
    private final Map<Long, byte[]> world = new HashMap<>();
    private final Map<String, String> meta = new HashMap<>();
    private final ArrayList<String> events = new ArrayList<>();

    @Override
    public synchronized Optional<PersistedPlayer> loadPlayer(String playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    @Override
    public synchronized void savePlayer(PersistedPlayer player) {
        if (player == null || player.playerId.isBlank()) return;
        players.put(player.playerId, player);
    }

    @Override
    public synchronized Optional<byte[]> loadWorldChunk(long chunkKey) {
        return Optional.ofNullable(world.get(chunkKey));
    }

    @Override
    public synchronized void saveWorldChunk(long chunkKey, byte[] snapshotBytes) {
        world.put(chunkKey, snapshotBytes == null ? new byte[0] : snapshotBytes.clone());
    }

    @Override
    public synchronized void putMeta(String key, String value) {
        if (key == null || key.isBlank()) return;
        meta.put(key, value == null ? "" : value);
    }

    @Override
    public synchronized String getMeta(String key, String fallback) {
        if (key == null) return fallback;
        return meta.getOrDefault(key, fallback);
    }

    @Override
    public synchronized void appendSessionEvent(long serverTick, String playerId, String eventType, String payload) {
        events.add(serverTick + "|" + playerId + "|" + eventType + "|" + payload);
    }

    @Override public void checkpoint() {}

    @Override public void close() {}
}
