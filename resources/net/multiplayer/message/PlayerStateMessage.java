package resources.net.multiplayer.message;

public final class PlayerStateMessage {

    private final String playerId;
    private final double worldX;
    private final double worldY;
    private final double velocityX;
    private final double velocityY;
    private final long processedSequence;
    private final int health;
    private final int maxHealth;
    private final int facing;          // 0=up,1=right,2=down,3=left
    private final boolean moving;
    private final String spriteName;
    private final String displayName;
    private final boolean alive;

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
        this(playerId, worldX, worldY, velocityX, velocityY, processedSequence, 20, 20);
    }

    public PlayerStateMessage(
            String playerId,
            double worldX,
            double worldY,
            double velocityX,
            double velocityY,
            long processedSequence,
            int health,
            int maxHealth) {
        this(playerId, worldX, worldY, velocityX, velocityY, processedSequence, health, maxHealth,
            2, false, "red", "", true);
    }

    public PlayerStateMessage(
            String playerId,
            double worldX,
            double worldY,
            double velocityX,
            double velocityY,
            long processedSequence,
            int health,
            int maxHealth,
            int facing,
            boolean moving,
            String spriteName,
            String displayName,
            boolean alive) {
        this.playerId = playerId;
        this.worldX = worldX;
        this.worldY = worldY;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.processedSequence = Math.max(0L, processedSequence);
        this.maxHealth = Math.max(1, maxHealth);
        this.health = Math.max(0, Math.min(this.maxHealth, health));
        this.facing = (facing < 0 || facing > 3) ? 2 : facing;
        this.moving = moving;
        this.spriteName = (spriteName == null || spriteName.isBlank()) ? "red" : spriteName;
        this.displayName = (displayName == null) ? "" : displayName;
        this.alive = alive;
    }

    public String playerId()   { return playerId; }
    public double worldX()     { return worldX; }
    public double worldY()     { return worldY; }
    public double velocityX()  { return velocityX; }
    public double velocityY()  { return velocityY; }
    public long processedSequence() { return processedSequence; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int facing() { return facing; }
    public boolean moving() { return moving; }
    public String spriteName() { return spriteName; }
    public String displayName() { return displayName; }
    public boolean alive() { return alive; }
}
