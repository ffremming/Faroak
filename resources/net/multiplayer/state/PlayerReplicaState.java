package resources.net.multiplayer.state;

/**
 * Authoritative server-side player state and current input intent.
 */
public final class PlayerReplicaState {

    private final String playerId;
    private String dimensionId;
    private double worldX;
    private double worldY;
    private double velocityX;
    private double velocityY;
    private boolean up;
    private boolean left;
    private boolean down;
    private boolean right;
    private long processedSequence;
    private long lastChangedTick;
    private long ridingEntityId;

    public PlayerReplicaState(String playerId, String dimensionId, double worldX, double worldY, long processedSequence, long tick) {
        this.playerId = playerId == null ? "" : playerId;
        this.dimensionId = TileMutationState.normalizeDimension(dimensionId);
        this.worldX = worldX;
        this.worldY = worldY;
        this.processedSequence = Math.max(0L, processedSequence);
        this.lastChangedTick = Math.max(0L, tick);
    }

    public String playerId() { return playerId; }
    public String dimensionId() { return dimensionId; }
    public double worldX() { return worldX; }
    public double worldY() { return worldY; }
    public double velocityX() { return velocityX; }
    public double velocityY() { return velocityY; }
    public boolean up() { return up; }
    public boolean left() { return left; }
    public boolean down() { return down; }
    public boolean right() { return right; }
    public long processedSequence() { return processedSequence; }
    public long lastChangedTick() { return lastChangedTick; }
    public long ridingEntityId() { return ridingEntityId; }

    public void setInput(boolean up, boolean left, boolean down, boolean right, long sequence, long tick) {
        boolean changed = this.up != up || this.left != left || this.down != down || this.right != right;
        this.up = up;
        this.left = left;
        this.down = down;
        this.right = right;
        this.processedSequence = Math.max(processedSequence, sequence);
        if (changed) lastChangedTick = Math.max(0L, tick);
    }

    public void moveTo(String dimensionId, double x, double y, double vx, double vy, long tick) {
        boolean changed = this.worldX != x || this.worldY != y || this.velocityX != vx || this.velocityY != vy;
        this.dimensionId = TileMutationState.normalizeDimension(dimensionId);
        this.worldX = x;
        this.worldY = y;
        this.velocityX = vx;
        this.velocityY = vy;
        if (changed) lastChangedTick = Math.max(0L, tick);
    }

    public void setRidingEntityId(long ridingEntityId, long tick) {
        if (this.ridingEntityId == ridingEntityId) return;
        this.ridingEntityId = Math.max(0L, ridingEntityId);
        lastChangedTick = Math.max(0L, tick);
    }

    public boolean changedSince(long sentTick) {
        return sentTick <= 0L || lastChangedTick > sentTick;
    }
}
