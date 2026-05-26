package resources.net.multiplayer.message;

public final class ServerAckMessage implements ServerMessage {

    private final String playerId;
    private final long acknowledgedSequence;
    private final long serverTick;

    public ServerAckMessage(String playerId, long acknowledgedSequence, long serverTick) {
        this.playerId = playerId == null ? "" : playerId;
        this.acknowledgedSequence = Math.max(0L, acknowledgedSequence);
        this.serverTick = Math.max(0L, serverTick);
    }

    public String playerId() { return playerId; }
    public long acknowledgedSequence() { return acknowledgedSequence; }
    public long serverTick() { return serverTick; }
}
