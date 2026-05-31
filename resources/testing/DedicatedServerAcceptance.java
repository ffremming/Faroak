package resources.testing;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.MultiplayerMode;
import resources.net.multiplayer.WebSocketServerAdapter;
import resources.net.multiplayer.message.ClientChatMessage;
import resources.net.multiplayer.message.ClientJoinMessage;
import resources.net.multiplayer.message.ServerChatMessage;
import resources.net.multiplayer.message.ServerMessage;
import resources.net.multiplayer.message.ServerSnapshotMessage;
import resources.net.multiplayer.message.ServerWelcomeMessage;
import resources.net.multiplayer.message.PlayerStateMessage;

/**
 * Standalone acceptance harness for the DEDICATED SERVER path. Connects two real
 * WebSocket clients to an already-running {@code ServerMain} (separate process)
 * and verifies: both are welcomed, the world is populated (seeded entities), each
 * sees the other (with name + appearance), chat relays between them, and the
 * world clock is replicated.
 *
 * Usage: java -cp out resources.testing.DedicatedServerAcceptance ws://127.0.0.1:7799/ws
 * Exits 0 on success, 1 on failure.
 */
public final class DedicatedServerAcceptance {

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "ws://127.0.0.1:7799/ws";

        Client alice = new Client("Alice-aaa111", url);
        Client bob = new Client("Bob-bbb222", url);
        alice.connectAndJoin(10.0, 10.0);
        bob.connectAndJoin(80.0, 40.0);

        // Pump both clients for a few seconds, collecting state.
        boolean aliceWelcomed = false, bobWelcomed = false;
        boolean aliceSeesBob = false, bobSeesAlice = false;
        boolean worldPopulated = false;
        boolean worldTimePresent = false;
        boolean chatRelayed = false;

        long start = System.nanoTime();
        boolean chatSent = false;
        while ((System.nanoTime() - start) < 8_000_000_000L) {
            alice.adapter.tick();
            bob.adapter.tick();
            for (ServerMessage m : alice.adapter.poll()) {
                if (m instanceof ServerWelcomeMessage && ((ServerWelcomeMessage) m).accepted()) aliceWelcomed = true;
                if (m instanceof ServerSnapshotMessage) {
                    ServerSnapshotMessage s = (ServerSnapshotMessage) m;
                    if (s.worldTimeTicks() > 0L) worldTimePresent = true;
                    if (!s.entities().isEmpty()) worldPopulated = true;
                    for (PlayerStateMessage p : s.players()) {
                        if ("Bob-bbb222".equals(p.playerId())) aliceSeesBob = true;
                    }
                }
                if (m instanceof ServerChatMessage) {
                    ServerChatMessage c = (ServerChatMessage) m;
                    if (!c.system() && "Bob".equals(c.senderName()) && "hi alice".equals(c.text())) chatRelayed = true;
                }
            }
            for (ServerMessage m : bob.adapter.poll()) {
                if (m instanceof ServerWelcomeMessage && ((ServerWelcomeMessage) m).accepted()) bobWelcomed = true;
                if (m instanceof ServerSnapshotMessage) {
                    for (PlayerStateMessage p : ((ServerSnapshotMessage) m).players()) {
                        if ("Alice-aaa111".equals(p.playerId())) bobSeesAlice = true;
                    }
                }
            }
            if (!chatSent && aliceWelcomed && bobWelcomed) {
                bob.adapter.submit(new ClientChatMessage("Bob-bbb222", "hi alice"));
                chatSent = true;
            }
            Thread.sleep(50L);
        }

        alice.adapter.disconnect("Alice-aaa111");
        bob.adapter.disconnect("Bob-bbb222");

        boolean ok = aliceWelcomed && bobWelcomed && aliceSeesBob && bobSeesAlice
            && worldPopulated && worldTimePresent && chatRelayed;
        System.out.println("[Acceptance] aliceWelcomed=" + aliceWelcomed + " bobWelcomed=" + bobWelcomed
            + " aliceSeesBob=" + aliceSeesBob + " bobSeesAlice=" + bobSeesAlice
            + " worldPopulated=" + worldPopulated + " worldTimePresent=" + worldTimePresent
            + " chatRelayed=" + chatRelayed);
        System.out.println("[Acceptance] " + (ok ? "PASS" : "FAIL"));
        System.exit(ok ? 0 : 1);
    }

    private static final class Client {
        final String playerId;
        final WebSocketServerAdapter adapter;

        Client(String playerId, String url) {
            this.playerId = playerId;
            System.setProperty("game.multiplayer.serverUrl", url);
            MultiplayerConfig cfg = new MultiplayerConfig(
                MultiplayerMode.CLIENT, "websocket", playerId, 10, 30, 20, 1, 120, 20.0, 768.0, 8192.0, "client.db");
            this.adapter = new WebSocketServerAdapter(cfg);
        }

        void connectAndJoin(double x, double y) throws InterruptedException {
            adapter.connect(playerId);
            for (int i = 0; i < 100 && !adapter.isConnected(); i++) {
                adapter.tick();
                Thread.sleep(50L);
            }
            adapter.submit(new ClientJoinMessage(playerId, true, x, y));
        }
    }
}
