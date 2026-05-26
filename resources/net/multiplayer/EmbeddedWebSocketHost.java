package resources.net.multiplayer;

import java.io.IOException;

import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.server.GameServerRuntime;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.gateway.WebSocketGatewayServer;
import resources.net.multiplayer.server.persistence.PersistenceStore;
import resources.net.multiplayer.server.persistence.PersistenceStoreFactory;

/**
 * Optional embedded websocket host for in-process HOST mode.
 */
final class EmbeddedWebSocketHost {

    private static final Object LOCK = new Object();

    private static GameServerRuntime runtime;
    private static WebSocketGatewayServer gateway;
    private static boolean started;

    private EmbeddedWebSocketHost() {}

    static void ensureStarted(MultiplayerConfig config, int port) {
        if (config == null) return;
        synchronized (LOCK) {
            if (started) return;
            PersistenceStore store = PersistenceStoreFactory.createDefault(config.sqlitePath());
            LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
                config, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
            GameServerRuntime serverRuntime = new GameServerRuntime(config, lobby);
            WebSocketGatewayServer gatewayServer = new WebSocketGatewayServer(config, lobby);
            serverRuntime.start();
            try {
                gatewayServer.start(port);
            } catch (IOException ex) {
                serverRuntime.close();
                throw new IllegalStateException("failed to start embedded websocket host on port " + port, ex);
            }
            runtime = serverRuntime;
            gateway = gatewayServer;
            started = true;
            Runtime.getRuntime().addShutdownHook(new Thread(EmbeddedWebSocketHost::shutdown, "embedded-ws-host-shutdown"));
        }
    }

    static void shutdown() {
        synchronized (LOCK) {
            if (!started) return;
            if (gateway != null) gateway.close();
            if (runtime != null) runtime.close();
            gateway = null;
            runtime = null;
            started = false;
        }
    }
}
