package resources.net.multiplayer.server;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.gateway.WebSocketGatewayServer;
import resources.net.multiplayer.server.persistence.PersistenceStore;
import resources.net.multiplayer.server.persistence.PersistenceStoreFactory;

/**
 * Standalone dedicated server entrypoint.
 */
public final class ServerMain {

    public static void main(String[] args) {
        MultiplayerConfig cfg = MultiplayerConfig.fromSystemProperties();
        PersistenceStore store = PersistenceStoreFactory.createDefault(cfg.sqlitePath());
        LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
            cfg, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        GameServerRuntime runtime = new GameServerRuntime(cfg, lobby);
        WebSocketGatewayServer gateway = null;
        runtime.start();
        if (gatewayEnabled()) {
            try {
                gateway = new WebSocketGatewayServer(cfg, lobby);
                gateway.start(gatewayPort());
            } catch (Exception ex) {
                System.out.println("gateway failed to start: " + ex.getMessage());
            }
        }
        final WebSocketGatewayServer finalGateway = gateway;
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));
        if (finalGateway != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(finalGateway::close));
        }
        while (true) {
            try { Thread.sleep(1000L); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        if (gateway != null) gateway.close();
        runtime.close();
    }

    private static boolean gatewayEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("game.multiplayer.gateway.enabled", "true"));
    }

    private static int gatewayPort() {
        try { return Integer.parseInt(System.getProperty("game.multiplayer.gatewayPort", "8080")); }
        catch (Exception ignored) { return 8080; }
    }
}
