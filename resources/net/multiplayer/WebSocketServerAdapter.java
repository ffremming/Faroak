package resources.net.multiplayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
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

    private final MultiplayerConfig config;
    private final ProtocolMessageTranslator translator;
    private final BinaryEnvelopeCodec codec = new BinaryEnvelopeCodec();
    private final ConcurrentLinkedQueue<ServerMessage> inbound = new ConcurrentLinkedQueue<>();
    private final ByteArrayOutputStream partial = new ByteArrayOutputStream();

    private WebSocket socket;
    private boolean connected;

    public WebSocketServerAdapter(MultiplayerConfig config) {
        this.config = config;
        this.translator = new ProtocolMessageTranslator(config.protocolVersion());
    }

    @Override
    public void connect(String playerId) {
        if (connected) return;
        String base = System.getProperty("game.multiplayer.serverUrl", "ws://127.0.0.1:8080/ws");
        String url = base + (base.contains("?") ? "&" : "?") + "playerId=" + config.playerId();
        try {
            socket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(url), new Listener())
                .join();
            connected = true;
        } catch (Exception ex) {
            connected = false;
        }
    }

    @Override
    public void disconnect(String playerId) {
        connected = false;
        if (socket != null) {
            try { socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join(); }
            catch (Exception ignored) {}
        }
        socket = null;
    }

    @Override public boolean isConnected() { return connected; }

    @Override
    public void submit(ClientMessage message) {
        if (!connected || socket == null) return;
        ProtocolEnvelope envelope = translator.toEnvelope(message);
        if (envelope == null) return;
        byte[] bytes = codec.encode(envelope);
        socket.sendBinary(ByteBuffer.wrap(bytes), true);
    }

    @Override
    public List<ServerMessage> poll() {
        ArrayList<ServerMessage> out = new ArrayList<>();
        while (!inbound.isEmpty()) out.add(inbound.poll());
        return out;
    }

    @Override public void tick() {}

    private final class Listener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            partial.write(bytes, 0, bytes.length);
            if (last) {
                ProtocolEnvelope envelope = codec.decode(partial.toByteArray());
                partial.reset();
                ServerMessage message = translator.fromEnvelope(envelope);
                if (message != null) inbound.add(message);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected = false;
            partial.reset();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            connected = false;
            partial.reset();
        }
    }
}
