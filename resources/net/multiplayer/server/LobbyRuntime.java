package resources.net.multiplayer.server;

import java.util.List;

import resources.net.multiplayer.protocol.ProtocolEnvelope;

/**
 * Single authoritative lobby runtime.
 */
public interface LobbyRuntime {

    void onConnect(String playerId);

    void onDisconnect(String playerId);

    void receive(ProtocolEnvelope envelope);

    void tick();

    List<ProtocolEnvelope> drainFor(String playerId);

    void close();
}
