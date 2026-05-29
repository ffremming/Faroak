package resources.testing.probes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.MultiplayerAdapterRegistry;
import resources.net.multiplayer.MultiplayerRuntime;
import resources.net.multiplayer.MultiplayerServerAdapter;
import resources.net.multiplayer.message.ClientInputMessage;
import resources.net.multiplayer.message.ClientJoinMessage;
import resources.net.multiplayer.message.ClientLeaveMessage;
import resources.net.multiplayer.message.ClientMessage;
import resources.net.multiplayer.message.ServerMessage;
import resources.net.multiplayer.message.ServerWelcomeMessage;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies reconnect retry and join replay after a dropped connection.
 */
public final class MultiplayerReconnectProbe implements Probe {

    @Override public String name() { return "mp-reconnect"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        String prevMode = System.getProperty("game.multiplayer.mode");
        String prevBackend = System.getProperty("game.multiplayer.backend");
        String prevPlayerId = System.getProperty("game.multiplayer.playerId");
        String prevReconnectEnabled = System.getProperty("game.multiplayer.reconnect.enabled");
        String prevReconnectDelay = System.getProperty("game.multiplayer.reconnectDelayMs");

        MultiplayerRuntime runtime = null;
        final String backendId = "probe-reconnect";
        FlakyReconnectAdapter adapter = new FlakyReconnectAdapter();
        try {
            System.setProperty("game.multiplayer.mode", "client");
            System.setProperty("game.multiplayer.backend", backendId);
            System.setProperty("game.multiplayer.playerId", "probe-reconnect-player");
            System.setProperty("game.multiplayer.reconnect.enabled", "true");
            System.setProperty("game.multiplayer.reconnectDelayMs", "0");

            MultiplayerAdapterRegistry.register(backendId, cfg -> adapter);
            runtime = MultiplayerRuntime.createDefault(harness.context());
            for (int i = 0; i < 24; i++) runtime.update(1.0);
            runtime.close();

            boolean reconnect = adapter.connectCalls >= 2;
            boolean joinReplayed = adapter.joinCount >= 2;
            boolean movementRepublished = adapter.inputCount >= 2;
            boolean rebasedFromWelcome = adapter.firstInputSequence > 500L;
            boolean monotonicAfterReconnect = adapter.secondInputSequence > adapter.firstInputSequence;
            String detail = "connectCalls=" + adapter.connectCalls
                + ", joinCount=" + adapter.joinCount
                + ", inputCount=" + adapter.inputCount
                + ", firstInputSeq=" + adapter.firstInputSequence
                + ", secondInputSeq=" + adapter.secondInputSequence
                + ", disconnectCalls=" + adapter.disconnectCalls;
            if (!reconnect || !joinReplayed || !movementRepublished
                    || !rebasedFromWelcome || !monotonicAfterReconnect) {
                return ProbeResult.fail(name() + " reconnect flow failed", detail);
            }
            return ProbeResult.pass(name(), detail);
        } finally {
            if (runtime != null) runtime.close();
            restore("game.multiplayer.mode", prevMode);
            restore("game.multiplayer.backend", prevBackend);
            restore("game.multiplayer.playerId", prevPlayerId);
            restore("game.multiplayer.reconnect.enabled", prevReconnectEnabled);
            restore("game.multiplayer.reconnectDelayMs", prevReconnectDelay);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key);
        else System.setProperty(key, value);
    }

    private static final class FlakyReconnectAdapter implements MultiplayerServerAdapter {
        int connectCalls;
        int disconnectCalls;
        int joinCount;
        int inputCount;
        long firstInputSequence = -1L;
        long secondInputSequence = -1L;
        long maxInputSequence;
        boolean connected;
        boolean droppedOnce;
        final ArrayDeque<ServerMessage> inbound = new ArrayDeque<>();

        @Override
        public void connect(String playerId) {
            connectCalls++;
            connected = true;
        }

        @Override
        public void disconnect(String playerId) {
            disconnectCalls++;
            connected = false;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void submit(ClientMessage message) {
            if (!connected || message == null) return;
            if (message instanceof ClientJoinMessage) {
                joinCount++;
                long acknowledged = joinCount <= 1 ? 500L : maxInputSequence;
                inbound.addLast(new ServerWelcomeMessage(
                    message.playerId(), true, "", acknowledged));
            } else if (message instanceof ClientInputMessage) {
                inputCount++;
                long seq = ((ClientInputMessage) message).sequence();
                if (firstInputSequence < 0L) firstInputSequence = seq;
                else if (secondInputSequence < 0L) secondInputSequence = seq;
                maxInputSequence = Math.max(maxInputSequence, seq);
                if (!droppedOnce) {
                    connected = false;
                    droppedOnce = true;
                }
            }
            else if (message instanceof ClientLeaveMessage) {
                // Keep adapter behavior explicit in probe metrics.
            }
        }

        @Override
        public List<ServerMessage> poll() {
            ArrayList<ServerMessage> out = new ArrayList<>(inbound.size());
            while (!inbound.isEmpty()) out.add(inbound.removeFirst());
            return out;
        }

        @Override
        public void tick() {}
    }
}
