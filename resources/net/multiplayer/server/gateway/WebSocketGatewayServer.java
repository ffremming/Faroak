package resources.net.multiplayer.server.gateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.protocol.BinaryEnvelopeCodec;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.server.LobbyRuntime;

/**
 * Websocket gateway exposing LobbyRuntime over network.
 */
public final class WebSocketGatewayServer implements AutoCloseable {

    private final MultiplayerConfig config;
    private final LobbyRuntime lobby;
    private final BinaryEnvelopeCodec codec = new BinaryEnvelopeCodec();
    private final WebSocketHandshake handshake = new WebSocketHandshake();
    private final WebSocketFrameIO frames = new WebSocketFrameIO();
    private final Map<String, ClientConnection> clients = new HashMap<>();

    private volatile boolean running;
    private ServerSocket server;
    private Thread acceptThread;

    public WebSocketGatewayServer(MultiplayerConfig config, LobbyRuntime lobby) {
        this.config = config;
        this.lobby = lobby;
    }

    public void start(int port) throws IOException {
        if (running) return;
        running = true;
        server = new ServerSocket(port);
        acceptThread = new Thread(this::acceptLoop, "ws-gateway-accept");
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = server.accept();
                ClientConnection client = new ClientConnection(socket);
                client.start();
            } catch (IOException ignored) {
                if (!running) break;
            }
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        List<ClientConnection> snapshot = new ArrayList<>(clients.values());
        clients.clear();
        for (ClientConnection c : snapshot) c.close();
    }

    private final class ClientConnection implements Runnable {
        private final Socket socket;
        private Thread thread;
        private String playerId;
        private boolean active = true;

        ClientConnection(Socket socket) { this.socket = socket; }

        void start() {
            thread = new Thread(this, "ws-client-" + socket.getPort());
            thread.start();
        }

        @Override
        public void run() {
            try {
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(30);
                WebSocketHandshake.Request req = handshake.accept(socket.getInputStream(), socket.getOutputStream());
                if (!req.path.startsWith("/ws")) return;
                playerId = extractPlayerId(req.path);
                ClientConnection replaced = null;
                synchronized (WebSocketGatewayServer.this) {
                    replaced = clients.put(playerId, this);
                }
                if (replaced != null && replaced != this) replaced.close();
                lobby.onConnect(playerId);
                loop();
            } catch (Exception ignored) {
            } finally {
                close();
            }
        }

        private void loop() throws IOException {
            while (running && active && !socket.isClosed()) {
                sendPending();
                try {
                    WebSocketFrameIO.Frame frame = frames.read(socket.getInputStream());
                    if (frame == null) break;
                    if (frames.isClose(frame)) break;
                    if (frames.isPing(frame)) {
                        frames.writePong(socket.getOutputStream(), frame.payload);
                        continue;
                    }
                    if (!frames.isBinary(frame)) continue;
                    ProtocolEnvelope decoded = codec.decode(frame.payload);
                    if (decoded == null) continue;
                    if (!decoded.playerId().isBlank() && !decoded.playerId().equals(playerId)) continue;
                    ProtocolEnvelope normalized = new ProtocolEnvelope(
                        decoded.protocolVersion(), playerId, decoded.sequence(), decoded.ackSequence(),
                        decoded.serverTick(), decoded.messageType(), decoded.payload());
                    lobby.receive(normalized);
                } catch (SocketTimeoutException ignored) {
                    // heartbeat loop: send pending messages even when idle.
                }
            }
        }

        private void sendPending() throws IOException {
            List<ProtocolEnvelope> out = lobby.drainFor(playerId);
            for (ProtocolEnvelope envelope : out) {
                byte[] bytes = codec.encode(envelope);
                frames.writeBinary(socket.getOutputStream(), bytes);
            }
        }

        void close() {
            active = false;
            synchronized (WebSocketGatewayServer.this) {
                clients.remove(playerId);
            }
            if (playerId != null) lobby.onDisconnect(playerId);
            try { frames.writeClose(socket.getOutputStream()); } catch (Exception ignored) {}
            try { socket.close(); } catch (IOException ignored) {}
        }

        private String extractPlayerId(String path) {
            if (path == null) return randomPlayerId();
            int q = path.indexOf('?');
            if (q < 0 || q >= path.length() - 1) return randomPlayerId();
            String query = path.substring(q + 1);
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "playerId".equalsIgnoreCase(kv[0]) && !kv[1].isBlank()) return kv[1];
            }
            return randomPlayerId();
        }

        private String randomPlayerId() {
            return "p-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
