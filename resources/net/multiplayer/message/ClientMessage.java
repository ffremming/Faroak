package resources.net.multiplayer.message;

/**
 * Base marker for client->server messages.
 */
public interface ClientMessage {
    String playerId();
    long sequence();
}
