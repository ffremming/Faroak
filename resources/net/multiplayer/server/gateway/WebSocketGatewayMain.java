package resources.net.multiplayer.server.gateway;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.server.GameServerRuntime;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.PersistenceStore;
import resources.net.multiplayer.server.persistence.PersistenceStoreFactory;

/**
 * Dedicated authoritative server + websocket gateway entrypoint.
 */
public final class WebSocketGatewayMain {

    public static void main(String[] args) throws Exception {
        MultiplayerConfig cfg = MultiplayerConfig.fromSystemProperties();
        int port = parsePort(System.getProperty("game.multiplayer.gatewayPort", "8080"));
        PersistenceStore store = PersistenceStoreFactory.createDefault(cfg.sqlitePath());
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        GameServerRuntime runtime = new GameServerRuntime(cfg, lobby);
        WebSocketGatewayServer gateway = new WebSocketGatewayServer(cfg, lobby);
        runtime.start();
        gateway.start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            gateway.close();
            runtime.close();
        }));
        while (true) Thread.sleep(1000L);
    }

    private static int parsePort(String raw) {
        try { return Integer.parseInt(raw); }
        catch (Exception ignored) { return 8080; }
    }
}
