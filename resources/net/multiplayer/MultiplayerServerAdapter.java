package resources.net.multiplayer;

import java.util.List;

import resources.net.multiplayer.message.ClientMessage;
import resources.net.multiplayer.message.ServerMessage;

/**
 * Client-facing port to any authoritative server implementation.
 *
 * Adapters implement this contract for loopback, dedicated UDP/TCP, relay, or
 * third-party backends. Game runtime only depends on this interface.
 */
public interface MultiplayerServerAdapter {

    void connect(String playerId);

    void disconnect(String playerId);

    boolean isConnected();

    void submit(ClientMessage message);

    List<ServerMessage> poll();

    /** Drives adapter-owned background work (optional no-op). */
    void tick();
}
