package resources.net.multiplayer.message;

public final class WorldObjectStateMessage {

    private final long objectId;
    private final String objectType;
    private final double worldX;
    private final double worldY;
    private final boolean removed;
    private final long revision;

    public WorldObjectStateMessage(
            long objectId,
            String objectType,
            double worldX,
            double worldY,
            boolean removed,
            long revision) {
        this.objectId = Math.max(0L, objectId);
        this.objectType = (objectType == null) ? "" : objectType;
        this.worldX = worldX;
        this.worldY = worldY;
        this.removed = removed;
        this.revision = Math.max(0L, revision);
    }

    public long objectId()      { return objectId; }
    public String objectType()  { return objectType; }
    public double worldX()      { return worldX; }
    public double worldY()      { return worldY; }
    public boolean removed()    { return removed; }
    public long revision()      { return revision; }
}
