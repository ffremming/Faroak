package resources.net.multiplayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;

import resources.net.multiplayer.message.ClientMessage;
import resources.net.multiplayer.message.ServerMessage;
import resources.net.multiplayer.protocol.BinaryEnvelopeCodec;
import resources.net.multiplayer.protocol.ProtocolEnvelope;

/**
 * Client adapter for remote websocket authoritative servers.
 */
public final class WebSocketServerAdapter implements MultiplayerServerAdapter {

    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 15_000L;

    private final MultiplayerConfig config;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ProtocolMessageTranslator translator;
    private final BinaryEnvelopeCodec codec = new BinaryEnvelopeCodec();
    private final ConcurrentLinkedQueue<ServerMessage> inbound = new ConcurrentLinkedQueue<>();
    private final ByteArrayOutputStream partial = new ByteArrayOutputStream();
    private final long connectionTimeoutMs;

    private volatile WebSocket socket;
    private volatile boolean connected;
    private volatile boolean connecting;
    private volatile long lastInboundAtMs;
    private volatile long activeConnectionId;
    private volatile CompletableFuture<WebSocket> connectFuture;
    private long connectionSerial;

    public WebSocketServerAdapter(MultiplayerConfig config) {
        this.config = config;
        this.translator = new ProtocolMessageTranslator(config.protocolVersion());
        this.connectionTimeoutMs = parseLong(
            "game.multiplayer.connectionTimeoutMs", DEFAULT_CONNECTION_TIMEOUT_MS, 0L, 120_000L);
    }

    @Override
    public synchronized void connect(String playerId) {
        if (connected || connecting) return;
        String base = System.getProperty("game.multiplayer.serverUrl", "ws://127.0.0.1:8080/ws");
        String url = base + (base.contains("?") ? "&" : "?") + "playerId=" + config.playerId();
        long connectionId = nextConnectionId();
        activeConnectionId = connectionId;
        connecting = true;
        connected = false;
        lastInboundAtMs = 0L;
        try {
            CompletableFuture<WebSocket> future = httpClient
                .newWebSocketBuilder()
                .buildAsync(URI.create(url), new Listener(connectionId));
            connectFuture = future;
            future.whenComplete((ws, error) -> onConnectResolved(connectionId, ws, error));
        } catch (Exception ex) {
            connecting = false;
            connected = false;
            lastInboundAtMs = 0L;
            connectFuture = null;
            socket = null;
        }
    }

    @Override
    public synchronized void disconnect(String playerId) {
        connected = false;
        connecting = false;
        lastInboundAtMs = 0L;
        activeConnectionId = nextConnectionId();
        CompletableFuture<WebSocket> future = connectFuture;
        connectFuture = null;
        if (future != null && !future.isDone()) future.cancel(true);
        WebSocket previous = socket;
        socket = null;
        resetPartial();
        safeClose(previous, "bye");
    }

    @Override public boolean isConnected() { return connected; }

    @Override
    public void submit(ClientMessage message) {
        if (!connected || socket == null) return;
        ProtocolEnvelope envelope = translator.toEnvelope(message);
        if (envelope == null) return;
        byte[] bytes = codec.encode(envelope);
        try {
            socket.sendBinary(ByteBuffer.wrap(bytes), true);
        } catch (Exception ex) {
            disconnect(config.playerId());
        }
    }

    @Override
    public List<ServerMessage> poll() {
        ArrayList<ServerMessage> out = new ArrayList<>();
        while (!inbound.isEmpty()) out.add(inbound.poll());
        return out;
    }

    @Override
    public void tick() {
        if (!connected || connectionTimeoutMs <= 0L) return;
        long now = System.currentTimeMillis();
        if ((now - lastInboundAtMs) > connectionTimeoutMs) {
            disconnect(config.playerId());
        }
    }

    private final class Listener implements WebSocket.Listener {
        private final long connectionId;

        Listener(long connectionId) {
            this.connectionId = connectionId;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            if (!isActive(connectionId)) {
                safeClose(webSocket, "stale");
                return null;
            }
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            synchronized (partial) {
                partial.write(bytes, 0, bytes.length);
                if (last) {
                    ProtocolEnvelope envelope = codec.decode(partial.toByteArray());
                    partial.reset();
                    ServerMessage message = translator.fromEnvelope(envelope);
                    if (message != null) inbound.add(message);
                }
            }
            lastInboundAtMs = System.currentTimeMillis();
            webSocket.request(1);
            return null;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            if (!isActive(connectionId)) {
                safeClose(webSocket, "stale");
                return;
            }
            lastInboundAtMs = System.currentTimeMillis();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (isActive(connectionId)) clearActiveConnection(webSocket);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (isActive(connectionId)) clearActiveConnection(webSocket);
        }
    }

    private synchronized void onConnectResolved(long connectionId, WebSocket ws, Throwable error) {
        if (!isActive(connectionId)) {
            safeClose(ws, "stale");
            return;
        }
        connectFuture = null;
        connecting = false;
        if (error != null || ws == null) {
            connected = false;
            socket = null;
            lastInboundAtMs = 0L;
            return;
        }
        socket = ws;
        connected = true;
        lastInboundAtMs = System.currentTimeMillis();
    }

    private synchronized void clearActiveConnection(WebSocket source) {
        if (source != null && socket != source) return;
        connected = false;
        connecting = false;
        socket = null;
        connectFuture = null;
        lastInboundAtMs = 0L;
        resetPartial();
    }

    private synchronized long nextConnectionId() {
        connectionSerial++;
        return connectionSerial;
    }

    private boolean isActive(long connectionId) {
        return connectionId == activeConnectionId;
    }

    private void resetPartial() {
        synchronized (partial) {
            partial.reset();
        }
    }

    private void safeClose(WebSocket ws, String reason) {
        if (ws == null) return;
        try { ws.sendClose(WebSocket.NORMAL_CLOSURE, reason == null ? "bye" : reason); }
        catch (Exception ignored) {}
    }

    private static long parseLong(String key, long fallback, long min, long max) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            long value = Long.parseLong(raw.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
