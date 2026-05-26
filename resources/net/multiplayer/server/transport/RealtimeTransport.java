package resources.net.multiplayer.server.transport;

import java.util.List;

import resources.net.multiplayer.protocol.ProtocolEnvelope;

/**
 * Server transport port for realtime envelopes.
 */
public interface RealtimeTransport {

    void connect(String playerId);

    void disconnect(String playerId);

    void sendToServer(ProtocolEnvelope envelope);

    List<ProtocolEnvelope> drainForClient(String playerId);
}
