package resources.testing.probes;

import java.util.List;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.MultiplayerMode;
import resources.net.multiplayer.WebSocketServerAdapter;
import resources.net.multiplayer.message.ClientInputMessage;
import resources.net.multiplayer.message.ClientJoinMessage;
import resources.net.multiplayer.message.ServerAckMessage;
import resources.net.multiplayer.message.ServerMessage;
import resources.net.multiplayer.message.ServerSnapshotMessage;
import resources.net.multiplayer.message.ServerWelcomeMessage;
import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.server.GameServerRuntime;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.gateway.WebSocketGatewayServer;
import resources.net.multiplayer.server.persistence.InMemoryPersistenceStore;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * End-to-end websocket gateway integration check.
 */
public final class WebSocketGatewayProbe implements Probe {

    @Override public String name() { return "mp-websocket-gateway"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        final int port = 18080;
        String previous = System.getProperty("game.multiplayer.serverUrl");
        GameServerRuntime runtime = null;
        WebSocketGatewayServer gateway = null;
        WebSocketServerAdapter adapter = null;
        try {
            MultiplayerConfig cfg = new MultiplayerConfig(
                MultiplayerMode.HOST, "websocket", "host", 10, 30, 20, 1, 120, 2.0, 128.0, 2048.0, "test.db");
            LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
                cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
            runtime = new GameServerRuntime(cfg, lobby);
            gateway = new WebSocketGatewayServer(cfg, lobby);
            runtime.start();
            gateway.start(port);

            System.setProperty("game.multiplayer.serverUrl", "ws://127.0.0.1:" + port + "/ws");
            MultiplayerConfig ccfg = new MultiplayerConfig(
                MultiplayerMode.CLIENT, "websocket", "p-web", 10, 30, 20, 1, 120, 2.0, 128.0, 2048.0, "test.db");
            adapter = new WebSocketServerAdapter(ccfg);
            adapter.connect("p-web");
            if (!waitConnected(adapter, 120, 20L)) {
                return ProbeResult.fail(name() + " connect timeout", "connected=false");
            }
            adapter.submit(new ClientJoinMessage("p-web"));

            boolean welcomed = false;
            boolean snapSeen = false;
            boolean ackSeen = false;
            for (int i = 0; i < 120; i++) {
                Thread.sleep(20L);
                adapter.tick();
                if (i == 10) adapter.submit(new ClientInputMessage("p-web", 1L, true, false, false, false));
                List<ServerMessage> messages = adapter.poll();
                for (ServerMessage msg : messages) {
                    if (msg instanceof ServerWelcomeMessage) welcomed |= ((ServerWelcomeMessage) msg).accepted();
                    if (msg instanceof ServerSnapshotMessage) snapSeen = true;
                    if (msg instanceof ServerAckMessage) ackSeen = true;
                }
                if (welcomed && snapSeen && ackSeen) break;
            }
            String detail = "welcomed=" + welcomed + ", snapshot=" + snapSeen + ", ack=" + ackSeen;
            if (!welcomed || !snapSeen || !ackSeen) return ProbeResult.fail(name() + " websocket flow failed", detail);
            return ProbeResult.pass(name(), detail);
        } catch (Exception ex) {
            return ProbeResult.fail(name() + " exception", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } finally {
            if (adapter != null) adapter.disconnect("p-web");
            if (gateway != null) gateway.close();
            if (runtime != null) runtime.close();
            if (previous == null) System.clearProperty("game.multiplayer.serverUrl");
            else System.setProperty("game.multiplayer.serverUrl", previous);
        }
    }

    private static boolean waitConnected(WebSocketServerAdapter adapter, int attempts, long sleepMillis)
            throws InterruptedException {
        if (adapter == null) return false;
        for (int i = 0; i < Math.max(1, attempts); i++) {
            adapter.tick();
            if (adapter.isConnected()) return true;
            Thread.sleep(Math.max(1L, sleepMillis));
        }
        return adapter.isConnected();
    }
}
