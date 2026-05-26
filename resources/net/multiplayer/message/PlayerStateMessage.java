package resources.net.multiplayer.message;

public final class PlayerStateMessage {

    private final String playerId;
    private final double worldX;
    private final double worldY;
    private final double velocityX;
    private final double velocityY;
    private final long processedSequence;

    public PlayerStateMessage(String playerId, double worldX, double worldY, double velocityX, double velocityY) {
        this(playerId, worldX, worldY, velocityX, velocityY, 0L);
    }

    public PlayerStateMessage(
            String playerId,
            double worldX,
            double worldY,
            double velocityX,
            double velocityY,
            long processedSequence) {
        this.playerId = playerId;
        this.worldX = worldX;
        this.worldY = worldY;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.processedSequence = Math.max(0L, processedSequence);
    }

    public String playerId()   { return playerId; }
    public double worldX()     { return worldX; }
    public double worldY()     { return worldY; }
    public double velocityX()  { return velocityX; }
    public double velocityY()  { return velocityY; }
    public long processedSequence() { return processedSequence; }
}
