package resources.net;

import resources.core.event.GameEvent;

/**
 * Bidirectional transport for game events. Implementations bridge the local
 * {@link resources.core.event.EventBus} to a remote peer (server or client).
 *
 * Declared as an interface only at this stage; concrete implementations
 * (WebSocket, KCP, in-process loopback for tests) plug in once the gameplay
 * code is multiplayer-aware. The point of having the type now is so that
 * subsystems don't accidentally bake in single-player assumptions —
 * mutations that touch shared state should route through a channel rather
 * than calling world.placeEntity directly.
 */
public interface NetworkChannel {

    void send(GameEvent event);

    void onReceive(java.util.function.Consumer<GameEvent> handler);

    void close();

    boolean isConnected();
}
