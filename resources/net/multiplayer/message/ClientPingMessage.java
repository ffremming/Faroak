package resources.net.multiplayer.message;

public final class ClientPingMessage implements ClientMessage {

    private final String playerId;
    private final long sequence;
    private final long clientTimeMillis;

    public ClientPingMessage(String playerId, long sequence, long clientTimeMillis) {
        this.playerId = playerId;
        this.sequence = Math.max(0L, sequence);
        this.clientTimeMillis = Math.max(0L, clientTimeMillis);
    }

    @Override public String playerId() { return playerId; }
    @Override public long sequence() { return sequence; }
    public long clientTimeMillis() { return clientTimeMillis; }
}
