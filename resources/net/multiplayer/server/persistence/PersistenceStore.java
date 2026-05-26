package resources.net.multiplayer.server.persistence;

import java.util.Optional;

/**
 * Server persistence port.
 */
public interface PersistenceStore extends AutoCloseable {

    Optional<PersistedPlayer> loadPlayer(String playerId);

    void savePlayer(PersistedPlayer player);

    Optional<byte[]> loadWorldChunk(long chunkKey);

    void saveWorldChunk(long chunkKey, byte[] snapshotBytes);

    void putMeta(String key, String value);

    String getMeta(String key, String fallback);

    void appendSessionEvent(long serverTick, String playerId, String eventType, String payload);

    void checkpoint();

    @Override
    void close();

    final class PersistedPlayer {
        public final String playerId;
        public final double worldX;
        public final double worldY;
        public final double velocityX;
        public final double velocityY;
        public final long lastSequence;

        public PersistedPlayer(
                String playerId,
                double worldX,
                double worldY,
                double velocityX,
                double velocityY,
                long lastSequence) {
            this.playerId = (playerId == null) ? "" : playerId;
            this.worldX = worldX;
            this.worldY = worldY;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.lastSequence = Math.max(0L, lastSequence);
        }
    }
}
