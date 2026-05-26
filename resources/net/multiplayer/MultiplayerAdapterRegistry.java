package resources.net.multiplayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of adapter factories keyed by backend id.
 *
 * This is the switch-point for server replacement. Register a new backend and
 * select it via `-Dgame.multiplayer.backend=<id>`.
 */
public final class MultiplayerAdapterRegistry {

    private static final Map<String, MultiplayerServerAdapterFactory> FACTORIES = new HashMap<>();

    static {
        register("loopback", cfg -> new LoopbackServerAdapter(cfg));
        register("websocket", cfg -> new WebSocketServerAdapter(cfg));
        register("ws", cfg -> new WebSocketServerAdapter(cfg));
    }

    private MultiplayerAdapterRegistry() {}

    public static void register(String id, MultiplayerServerAdapterFactory factory) {
        if (id == null || id.isBlank() || factory == null) return;
        FACTORIES.put(id.trim().toLowerCase(), factory);
    }

    public static MultiplayerServerAdapter create(MultiplayerConfig config) {
        MultiplayerServerAdapterFactory factory = FACTORIES.get(config.backend().trim().toLowerCase());
        if (factory == null) factory = FACTORIES.get("loopback");
        return factory.create(config);
    }
}
