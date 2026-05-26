package resources.net.multiplayer;

/**
 * Factory for adapter construction from immutable runtime config.
 */
public interface MultiplayerServerAdapterFactory {

    MultiplayerServerAdapter create(MultiplayerConfig config);
}
