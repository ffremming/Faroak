package resources.net.multiplayer.message;

public final class ServerCommandResultMessage implements ServerMessage {

    private final String playerId;
    private final long commandSequence;
    private final boolean accepted;
    private final String reason;
    private final long serverTick;

    public ServerCommandResultMessage(
            String playerId,
            long commandSequence,
            boolean accepted,
            String reason,
            long serverTick) {
        this.playerId = playerId == null ? "" : playerId;
        this.commandSequence = Math.max(0L, commandSequence);
        this.accepted = accepted;
        this.reason = reason == null ? "" : reason;
        this.serverTick = Math.max(0L, serverTick);
    }

    public String playerId() { return playerId; }
    public long commandSequence() { return commandSequence; }
    public boolean accepted() { return accepted; }
    public String reason() { return reason; }
    public long serverTick() { return serverTick; }
}
