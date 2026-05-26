package resources.net.multiplayer.message;

public final class ServerPlayerPresenceMessage implements ServerMessage {

    private final String playerId;
    private final boolean joined;
    private final long serverTick;

    public ServerPlayerPresenceMessage(String playerId, boolean joined, long serverTick) {
        this.playerId = playerId == null ? "" : playerId;
        this.joined = joined;
        this.serverTick = Math.max(0L, serverTick);
    }

    public String playerId() { return playerId; }
    public boolean joined() { return joined; }
    public long serverTick() { return serverTick; }
}
