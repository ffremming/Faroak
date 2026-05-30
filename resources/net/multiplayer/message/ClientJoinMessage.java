package resources.net.multiplayer.message;

public final class ClientJoinMessage implements ClientMessage {

    private final String playerId;
    private final boolean hasSpawn;
    private final double spawnX;
    private final double spawnY;

    public ClientJoinMessage(String playerId) {
        this(playerId, false, 0.0, 0.0);
    }

    public ClientJoinMessage(String playerId, boolean hasSpawn, double spawnX, double spawnY) {
        this.playerId = playerId;
        this.hasSpawn = hasSpawn;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }

    @Override public String playerId() { return playerId; }
    @Override public long sequence()    { return 0L; }
    public boolean hasSpawn()           { return hasSpawn; }
    public double spawnX()              { return spawnX; }
    public double spawnY()              { return spawnY; }
}
