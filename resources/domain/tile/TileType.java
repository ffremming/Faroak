package resources.domain.tile;

import java.util.function.Consumer;

import resources.core.id.Identifier;

/**
 * Data-driven definition of a tile kind. Held in {@link TileTypeRegistry}.
 *
 * Separating "what tiles exist" (type) from "tiles in the world" (Tile instance)
 * lets new tile kinds be added by registering a TileType rather than subclassing
 * Tile. Tile-specific behaviours (auto-connect rule, light emission, fertility,
 * cliff height bucket) live on the type.
 *
 * Use {@link Builder} to construct — keeps the call site readable when only a
 * few of the dozen-or-so optional fields are needed.
 */
public final class TileType {

    private final Identifier id;
    private final String spriteName;
    private final int altitudeBucket;
    private final boolean water;
    private final boolean solid;
    private final boolean animated;
    private final Consumer<Tile> onPlace;

    private TileType(Builder b) {
        this.id             = b.id;
        this.spriteName     = b.spriteName;
        this.altitudeBucket = b.altitudeBucket;
        this.water          = b.water;
        this.solid          = b.solid;
        this.animated       = b.animated;
        this.onPlace        = b.onPlace;
    }

    public Identifier id()            { return id; }
    public String     spriteName()    { return spriteName; }
    public int        altitudeBucket(){ return altitudeBucket; }
    public boolean    water()         { return water; }
    public boolean    solid()         { return solid; }
    public boolean    animated()      { return animated; }

    /** Optional hook fired when a Tile of this type is instantiated. */
    public void onPlace(Tile tile)    { if (onPlace != null) onPlace.accept(tile); }

    public static Builder builder(Identifier id, String spriteName) {
        return new Builder(id, spriteName);
    }

    public static final class Builder {
        private final Identifier id;
        private final String spriteName;
        private int altitudeBucket = 100;
        private boolean water    = false;
        private boolean solid    = false;
        private boolean animated = false;
        private Consumer<Tile> onPlace;

        private Builder(Identifier id, String spriteName) {
            this.id = id;
            this.spriteName = spriteName;
        }

        public Builder altitudeBucket(int v) { this.altitudeBucket = v; return this; }
        public Builder water(boolean v)      { this.water = v;          return this; }
        public Builder solid(boolean v)      { this.solid = v;          return this; }
        public Builder animated(boolean v)   { this.animated = v;       return this; }
        public Builder onPlace(Consumer<Tile> hook) { this.onPlace = hook; return this; }

        public TileType build() { return new TileType(this); }
    }
}
