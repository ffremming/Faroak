package resources.testing.probes;

import java.util.List;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.MultiplayerMode;
import resources.net.multiplayer.WebSocketServerAdapter;
import resources.net.multiplayer.message.ClientInputMessage;
import resources.net.multiplayer.message.ClientJoinMessage;
import resources.net.multiplayer.message.PlayerStateMessage;
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
 * Verifies two websocket clients see each other and receive movement updates.
 */
public final class WebSocketTwoClientMovementProbe implements Probe {

    @Override public String name() { return "mp-websocket-2client-move"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        final int port = 18081;
        String previous = System.getProperty("game.multiplayer.serverUrl");
        GameServerRuntime runtime = null;
        WebSocketGatewayServer gateway = null;
        WebSocketServerAdapter a = null;
        WebSocketServerAdapter b = null;

        try {
            MultiplayerConfig hostCfg = new MultiplayerConfig(
                MultiplayerMode.HOST, "websocket", "host", 10, 30, 20, 1, 120, 2.0, 128.0, 2048.0, "test.db");
            LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
                hostCfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());
            runtime = new GameServerRuntime(hostCfg, lobby);
            gateway = new WebSocketGatewayServer(hostCfg, lobby);
            runtime.start();
            gateway.start(port);

            System.setProperty("game.multiplayer.serverUrl", "ws://127.0.0.1:" + port + "/ws");
            MultiplayerConfig aCfg = new MultiplayerConfig(
                MultiplayerMode.CLIENT, "websocket", "p-a", 10, 30, 20, 1, 120, 2.0, 128.0, 2048.0, "test.db");
            MultiplayerConfig bCfg = new MultiplayerConfig(
                MultiplayerMode.CLIENT, "websocket", "p-b", 10, 30, 20, 1, 120, 2.0, 128.0, 2048.0, "test.db");
            a = new WebSocketServerAdapter(aCfg);
            b = new WebSocketServerAdapter(bCfg);
            a.connect("p-a");
            b.connect("p-b");
            if (!waitConnected(a, 120, 20L) || !waitConnected(b, 120, 20L)) {
                return ProbeResult.fail(name() + " connect timeout",
                    "aConnected=" + (a != null && a.isConnected())
                    + ", bConnected=" + (b != null && b.isConnected()));
            }
            a.submit(new ClientJoinMessage("p-a"));
            b.submit(new ClientJoinMessage("p-b"));

            boolean aWelcome = false;
            boolean bWelcome = false;
            boolean aSeesB = false;
            boolean bSeesA = false;
            double bFirstSeenAX = Double.NaN;
            double bMaxSeenAX = Double.NaN;

            for (int i = 0; i < 180; i++) {
                Thread.sleep(20L);
                if (i == 20) a.submit(new ClientInputMessage("p-a", 1L, false, false, false, true));
                if (i == 80) a.submit(new ClientInputMessage("p-a", 2L, false, false, false, false));
                a.tick();
                b.tick();

                List<ServerMessage> am = a.poll();
                for (ServerMessage msg : am) {
                    if (msg instanceof ServerWelcomeMessage) aWelcome |= ((ServerWelcomeMessage) msg).accepted();
                    if (msg instanceof ServerSnapshotMessage) {
                        ServerSnapshotMessage snap = (ServerSnapshotMessage) msg;
                        if (containsPlayer(snap.players(), "p-b")) aSeesB = true;
                    }
                }
                List<ServerMessage> bm = b.poll();
                for (ServerMessage msg : bm) {
                    if (msg instanceof ServerWelcomeMessage) bWelcome |= ((ServerWelcomeMessage) msg).accepted();
                    if (msg instanceof ServerSnapshotMessage) {
                        ServerSnapshotMessage snap = (ServerSnapshotMessage) msg;
                        for (PlayerStateMessage ps : snap.players()) {
                            if (!"p-a".equals(ps.playerId())) continue;
                            bSeesA = true;
                            if (Double.isNaN(bFirstSeenAX)) bFirstSeenAX = ps.worldX();
                            if (Double.isNaN(bMaxSeenAX) || ps.worldX() > bMaxSeenAX) bMaxSeenAX = ps.worldX();
                        }
                    }
                }

                if (aWelcome && bWelcome && aSeesB && bSeesA && !Double.isNaN(bFirstSeenAX) && !Double.isNaN(bMaxSeenAX)
                        && (bMaxSeenAX - bFirstSeenAX) >= 2.0) {
                    break;
                }
            }

            boolean movedObserved = !Double.isNaN(bFirstSeenAX) && !Double.isNaN(bMaxSeenAX) && (bMaxSeenAX - bFirstSeenAX) >= 2.0;
            String detail = "aWelcome=" + aWelcome + ", bWelcome=" + bWelcome
                + ", aSeesB=" + aSeesB + ", bSeesA=" + bSeesA
                + ", movedObserved=" + movedObserved
                + ", dx=" + delta(bFirstSeenAX, bMaxSeenAX);
            if (!aWelcome || !bWelcome || !aSeesB || !bSeesA || !movedObserved) {
                return ProbeResult.fail(name() + " websocket replication failed", detail);
            }
            return ProbeResult.pass(name(), detail);
        } catch (Exception ex) {
            return ProbeResult.fail(name() + " exception", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } finally {
            if (a != null) a.disconnect("p-a");
            if (b != null) b.disconnect("p-b");
            if (gateway != null) gateway.close();
            if (runtime != null) runtime.close();
            if (previous == null) System.clearProperty("game.multiplayer.serverUrl");
            else System.setProperty("game.multiplayer.serverUrl", previous);
        }
    }

    private static boolean containsPlayer(List<PlayerStateMessage> players, String id) {
        if (players == null || id == null) return false;
        for (PlayerStateMessage ps : players) {
            if (ps != null && id.equals(ps.playerId())) return true;
        }
        return false;
    }

    private static String delta(double first, double max) {
        if (Double.isNaN(first) || Double.isNaN(max)) return "n/a";
        return String.format("%.2f", (max - first));
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
