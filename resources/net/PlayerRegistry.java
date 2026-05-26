package resources.net;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks connected players and enforces a maximum player cap.
 */
public final class PlayerRegistry {

    private final int maxPlayers;
    private final Set<String> players = new HashSet<>();

    public PlayerRegistry() {
        this(MultiplayerSettings.MAX_PLAYERS);
    }

    public PlayerRegistry(int maxPlayers) {
        this.maxPlayers = Math.max(1, maxPlayers);
    }

    public synchronized boolean join(String playerId) {
        if (playerId == null || playerId.isBlank()) return false;
        if (players.contains(playerId)) return true;
        if (players.size() >= maxPlayers) return false;
        players.add(playerId);
        return true;
    }

    public synchronized void leave(String playerId) {
        players.remove(playerId);
    }

    public synchronized boolean contains(String playerId) {
        return players.contains(playerId);
    }

    public synchronized boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public synchronized int count() {
        return players.size();
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public synchronized Set<String> snapshot() {
        return Collections.unmodifiableSet(new HashSet<>(players));
    }
}
