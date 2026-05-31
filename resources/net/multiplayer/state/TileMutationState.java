package resources.net.multiplayer.state;

/**
 * Mutable tile overlay for server-owned world changes such as farmland and crops.
 */
public final class TileMutationState {

    private final String dimensionId;
    private final int tileX;
    private final int tileY;
    private String tileType;
    private boolean watered;
    private String cropType;
    private int cropStage;
    private long revision;
    private long lastChangedTick;

    public TileMutationState(String dimensionId, int tileX, int tileY, String tileType, long revision, long tick) {
        this.dimensionId = normalizeDimension(dimensionId);
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileType = tileType == null || tileType.isBlank() ? "tile" : tileType.trim().toLowerCase();
        this.revision = Math.max(0L, revision);
        this.lastChangedTick = Math.max(0L, tick);
    }

    public String dimensionId() { return dimensionId; }
    public int tileX() { return tileX; }
    public int tileY() { return tileY; }
    public String tileType() { return tileType; }
    public boolean watered() { return watered; }
    public String cropType() { return cropType == null ? "" : cropType; }
    public int cropStage() { return cropStage; }
    public long revision() { return revision; }
    public long lastChangedTick() { return lastChangedTick; }

    public void water(long newRevision, long tick) {
        watered = true;
        tileType = "farmland_watered";
        touch(newRevision, tick);
    }

    public void plant(String cropType, long newRevision, long tick) {
        if (cropType == null || cropType.isBlank()) return;
        this.cropType = cropType.trim().toLowerCase();
        this.cropStage = 0;
        touch(newRevision, tick);
    }

    public void advanceCrop(int stage, long newRevision, long tick) {
        cropStage = Math.max(cropStage, stage);
        touch(newRevision, tick);
    }

    public boolean changedSince(long sentTick) {
        return sentTick <= 0L || lastChangedTick > sentTick;
    }

    public String key() {
        return key(dimensionId, tileX, tileY);
    }

    public static String key(String dimensionId, int tileX, int tileY) {
        return normalizeDimension(dimensionId) + ":" + tileX + ":" + tileY;
    }

    public static String normalizeDimension(String dimensionId) {
        return dimensionId == null || dimensionId.isBlank() ? "core:overworld" : dimensionId.trim().toLowerCase();
    }

    private void touch(long newRevision, long tick) {
        revision = Math.max(revision, newRevision);
        lastChangedTick = Math.max(0L, tick);
    }
}
