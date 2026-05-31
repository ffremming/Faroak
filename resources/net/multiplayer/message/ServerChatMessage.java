package resources.net.multiplayer.message;

/**
 * A chat line broadcast by the server to all clients. {@code system} lines (join,
 * leave, death) have an empty sender and are rendered distinctly.
 */
public final class ServerChatMessage implements ServerMessage {

    private final String senderName;
    private final String text;
    private final boolean system;

    public ServerChatMessage(String senderName, String text, boolean system) {
        this.senderName = (senderName == null) ? "" : senderName;
        this.text = (text == null) ? "" : text;
        this.system = system;
    }

    public String senderName() { return senderName; }
    public String text()       { return text; }
    public boolean system()    { return system; }
}
