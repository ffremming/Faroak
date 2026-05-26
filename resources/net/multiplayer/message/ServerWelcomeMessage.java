package resources.net.multiplayer.message;

public final class ServerWelcomeMessage implements ServerMessage {

    private final String playerId;
    private final boolean accepted;
    private final String reason;

    public ServerWelcomeMessage(String playerId, boolean accepted, String reason) {
        this.playerId = playerId;
        this.accepted = accepted;
        this.reason = reason == null ? "" : reason;
    }

    public String playerId() { return playerId; }
    public boolean accepted() { return accepted; }
    public String reason() { return reason; }
}
