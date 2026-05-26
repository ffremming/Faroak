package resources.net;

import java.util.function.Consumer;

import resources.core.event.GameEvent;

/**
 * In-process channel used as the default runtime transport.
 *
 * Keeps single-player behavior intact while allowing gameplay code to route
 * through {@link NetworkChannel}. Server/client transports can replace this
 * without changing callers.
 */
public final class LoopbackNetworkChannel implements NetworkChannel {

    private Consumer<GameEvent> receiver;
    private boolean connected = true;

    @Override
    public void send(GameEvent event) {
        if (!connected || receiver == null || event == null) return;
        receiver.accept(event);
    }

    @Override
    public void onReceive(Consumer<GameEvent> handler) {
        receiver = handler;
    }

    @Override
    public void close() {
        connected = false;
        receiver = null;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
