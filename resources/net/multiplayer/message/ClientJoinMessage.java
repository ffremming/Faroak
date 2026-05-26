package resources.net.multiplayer.message;

public final class ClientJoinMessage implements ClientMessage {

    private final String playerId;

    public ClientJoinMessage(String playerId) {
        this.playerId = playerId;
    }

    @Override public String playerId() { return playerId; }
    @Override public long sequence()    { return 0L; }
}
