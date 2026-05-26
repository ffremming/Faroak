package resources.net.multiplayer.server.transport;

import java.util.List;

import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.server.LobbyRuntime;

/**
 * In-process realtime transport used by loopback adapter.
 */
public final class InProcessRealtimeTransport implements RealtimeTransport {

    private final LobbyRuntime lobby;

    public InProcessRealtimeTransport(LobbyRuntime lobby) {
        this.lobby = lobby;
    }

    @Override
    public void connect(String playerId) {
        lobby.onConnect(playerId);
    }

    @Override
    public void disconnect(String playerId) {
        lobby.onDisconnect(playerId);
    }

    @Override
    public void sendToServer(ProtocolEnvelope envelope) {
        lobby.receive(envelope);
    }

    @Override
    public List<ProtocolEnvelope> drainForClient(String playerId) {
        return lobby.drainFor(playerId);
    }
}
