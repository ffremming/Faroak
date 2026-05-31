package resources.net.multiplayer.server.persistence;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Durable, pure-JDK persistence backed by plain files under a data directory.
 *
 * <p>No external dependency (the project ships no SQLite JDBC jar), so this is
 * the default durable store for a dedicated server. Layout:
 * <pre>
 *   &lt;root&gt;/players/&lt;playerId&gt;.dat   binary player record
 *   &lt;root&gt;/chunks/&lt;chunkKey&gt;.bin    raw world-chunk snapshot bytes
 *   &lt;root&gt;/meta.properties           key/value metadata
 *   &lt;root&gt;/events.log                append-only session event log
 * </pre>
 *
 * <p>All methods are synchronized. The owning lobby already serializes calls
 * under a single lock; the extra guard keeps the store safe in isolation
 * (probes, tests) too.
 */
public final class JsonFilePersistenceStore implements PersistenceStore {

    private static final int PLAYER_RECORD_VERSION = 1;

    private final Path root;
    private final Path playersDir;
    private final Path chunksDir;
    private final Path metaFile;
    private final Path eventsFile;
    private final Properties meta = new Properties();

    public JsonFilePersistenceStore(Path root) {
        this.root = root;
        this.playersDir = root.resolve("players");
        this.chunksDir = root.resolve("chunks");
        this.metaFile = root.resolve("meta.properties");
        this.eventsFile = root.resolve("events.log");
        try {
            Files.createDirectories(playersDir);
            Files.createDirectories(chunksDir);
            if (Files.exists(metaFile)) {
                try (InputStream in = Files.newInputStream(metaFile)) {
                    meta.load(in);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to initialise file persistence at " + root, e);
        }
    }

    public JsonFilePersistenceStore(String root) {
        this(Path.of(root));
    }

    @Override
    public synchronized Optional<PersistedPlayer> loadPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) return Optional.empty();
        Path file = playersDir.resolve(sanitize(playerId) + ".dat");
        if (!Files.exists(file)) return Optional.empty();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            in.readInt(); // record version, reserved for forward migration
            String id = in.readUTF();
            double x = in.readDouble();
            double y = in.readDouble();
            double vx = in.readDouble();
            double vy = in.readDouble();
            long seq = in.readLong();
            return Optional.of(new PersistedPlayer(id, x, y, vx, vy, seq));
        } catch (IOException e) {
            System.err.println("[JsonFilePersistenceStore] loadPlayer failed for " + playerId + ": " + e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void savePlayer(PersistedPlayer player) {
        if (player == null || player.playerId.isBlank()) return;
        Path file = playersDir.resolve(sanitize(player.playerId) + ".dat");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
            out.writeInt(PLAYER_RECORD_VERSION);
            out.writeUTF(player.playerId);
            out.writeDouble(player.worldX);
            out.writeDouble(player.worldY);
            out.writeDouble(player.velocityX);
            out.writeDouble(player.velocityY);
            out.writeLong(player.lastSequence);
        } catch (IOException e) {
            System.err.println("[JsonFilePersistenceStore] savePlayer failed for " + player.playerId + ": " + e);
        }
    }

    @Override
    public synchronized Optional<byte[]> loadWorldChunk(long chunkKey) {
        Path file = chunksDir.resolve(chunkKey + ".bin");
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            System.err.println("[JsonFilePersistenceStore] loadWorldChunk failed for " + chunkKey + ": " + e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void saveWorldChunk(long chunkKey, byte[] snapshotBytes) {
        Path file = chunksDir.resolve(chunkKey + ".bin");
        try (OutputStream out = Files.newOutputStream(file,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            out.write(snapshotBytes == null ? new byte[0] : snapshotBytes);
        } catch (IOException e) {
            System.err.println("[JsonFilePersistenceStore] saveWorldChunk failed for " + chunkKey + ": " + e);
        }
    }

    @Override
    public synchronized List<Long> listWorldChunkKeys() {
        List<Long> keys = new ArrayList<>();
        if (!Files.isDirectory(chunksDir)) return keys;
        try (Stream<Path> files = Files.list(chunksDir)) {
            files.forEach(p -> {
                String fn = p.getFileName().toString();
                if (fn.endsWith(".bin")) {
                    try {
                        keys.add(Long.parseLong(fn.substring(0, fn.length() - 4)));
                    } catch (NumberFormatException ignored) {
                        // expected: skip files that aren't chunk-key named
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("[JsonFilePersistenceStore] listWorldChunkKeys failed: " + e);
        }
        return keys;
    }

    @Override
    public synchronized void putMeta(String key, String value) {
        if (key == null || key.isBlank()) return;
        meta.setProperty(key, value == null ? "" : value);
        flushMeta();
    }

    @Override
    public synchronized String getMeta(String key, String fallback) {
        if (key == null) return fallback;
        return meta.getProperty(key, fallback);
    }

    @Override
    public synchronized void appendSessionEvent(long serverTick, String playerId, String eventType, String payload) {
        String line = serverTick + "|" + playerId + "|" + eventType + "|"
            + (payload == null ? "" : payload.replace('\n', ' ')) + System.lineSeparator();
        try (BufferedWriter w = Files.newBufferedWriter(eventsFile,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
            w.write(line);
        } catch (IOException e) {
            System.err.println("[JsonFilePersistenceStore] appendSessionEvent failed: " + e);
        }
    }

    @Override
    public synchronized void checkpoint() {
        flushMeta();
    }

    @Override
    public synchronized void close() {
        flushMeta();
    }

    public Path root() {
        return root;
    }

    private void flushMeta() {
        try (OutputStream out = Files.newOutputStream(metaFile,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            meta.store(out, "multiplayer meta");
        } catch (IOException e) {
            System.err.println("[JsonFilePersistenceStore] flushMeta failed: " + e);
        }
    }

    private static String sanitize(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.length() == 0 ? "_" : sb.toString();
    }
}
