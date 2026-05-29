package resources.net.multiplayer.message;

public final class ServerWelcomeMessage implements ServerMessage {

    private final String playerId;
    private final boolean accepted;
    private final String reason;
    private final long acknowledgedSequence;

    public ServerWelcomeMessage(String playerId, boolean accepted, String reason) {
        this(playerId, accepted, reason, 0L);
    }

    public ServerWelcomeMessage(String playerId, boolean accepted, String reason, long acknowledgedSequence) {
        this.playerId = playerId;
        this.accepted = accepted;
        this.reason = reason == null ? "" : reason;
        this.acknowledgedSequence = Math.max(0L, acknowledgedSequence);
    }

    public String playerId() { return playerId; }
    public boolean accepted() { return accepted; }
    public String reason() { return reason; }
    public long acknowledgedSequence() { return acknowledgedSequence; }
}
