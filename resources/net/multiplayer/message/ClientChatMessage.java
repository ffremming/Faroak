package resources.net.multiplayer.message;

/**
 * A chat line typed by a player, sent to the server for relay to everyone.
 */
public final class ClientChatMessage implements ClientMessage {

    private final String playerId;
    private final String text;

    public ClientChatMessage(String playerId, String text) {
        this.playerId = (playerId == null) ? "" : playerId;
        this.text = (text == null) ? "" : text;
    }

    @Override public String playerId() { return playerId; }
    @Override public long sequence()    { return 0L; }

    public String text() { return text; }
}
