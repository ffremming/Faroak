package resources.net.multiplayer;

import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.message.ClientMessage;
import resources.net.multiplayer.message.ServerMessage;

/**
 * Offline adapter.
 */
final class NoopServerAdapter implements MultiplayerServerAdapter {

    @Override public void connect(String playerId) {}
    @Override public void disconnect(String playerId) {}
    @Override public boolean isConnected() { return false; }
    @Override public void submit(ClientMessage message) {}
    @Override public List<ServerMessage> poll() { return new ArrayList<>(); }
    @Override public void tick() {}
}
