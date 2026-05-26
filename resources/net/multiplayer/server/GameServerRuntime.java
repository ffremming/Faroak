package resources.net.multiplayer.server;

import resources.net.multiplayer.MultiplayerConfig;

/**
 * Fixed-rate dedicated server loop wrapper for a lobby runtime.
 */
public final class GameServerRuntime implements Runnable, AutoCloseable {

    private final LobbyRuntime lobby;
    private final long tickNanos;
    private volatile boolean running;
    private Thread thread;

    public GameServerRuntime(MultiplayerConfig config, LobbyRuntime lobby) {
        this.lobby = lobby;
        this.tickNanos = 1_000_000_000L / Math.max(1, config.serverTickRate());
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "authoritative-server");
        thread.start();
    }

    public void step() {
        lobby.tick();
    }

    @Override
    public void run() {
        long next = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            if (now >= next) {
                lobby.tick();
                next += tickNanos;
            } else {
                long sleepMs = Math.max(0L, (next - now) / 1_000_000L);
                if (sleepMs > 0) {
                    try { Thread.sleep(sleepMs); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        if (thread != null) thread.interrupt();
        lobby.close();
    }
}
