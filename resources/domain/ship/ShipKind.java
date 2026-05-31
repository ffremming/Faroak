package resources.domain.ship;

import java.util.Map;
import java.util.function.Function;

import resources.app.GameContext;
import resources.core.id.Identifier;
import resources.domain.ai.ShipGoal;
import resources.domain.ai.ShipReaction;

/**
 * Immutable definition of a ship type. One instance per kind, held in
 * {@link ShipKindRegistry}. {@link resources.domain.object.Boat} reads its
 * kind for footprint, speed, terrain table, health, weapon loadout, default
 * behaviour and boardability — so adding a ship type is a registry entry plus
 * sprites, not a new class.
 *
 * Built via {@link Builder} to keep the (long) parameter list readable and let
 * future fields default sanely.
 */
public final class ShipKind {

    private final String id;
    private final String displayName;
    private final String spriteDir;        // dir under resources/images/... ; null → procedural placeholder
    private final int width, height;
    private final int hitboxWidth, hitboxHeight;
    private final double speed;
    private final Map<String, Double> terrainTable;
    private final int maxHealth;
    private final WeaponLoadout loadout;
    private final Faction faction;
    private final ShipReaction reaction;
    private final Identifier interiorDimension; // null → not boardable
    private final Function<GameContext, ShipGoal> goalFactory; // null → no autonomous goal

    private ShipKind(Builder b) {
        this.id = b.id;
        this.displayName = b.displayName;
        this.spriteDir = b.spriteDir;
        this.width = b.width;
        this.height = b.height;
        this.hitboxWidth = b.hitboxWidth;
        this.hitboxHeight = b.hitboxHeight;
        this.speed = b.speed;
        this.terrainTable = b.terrainTable;
        this.maxHealth = b.maxHealth;
        this.loadout = b.loadout;
        this.faction = b.faction;
        this.reaction = b.reaction;
        this.interiorDimension = b.interiorDimension;
        this.goalFactory = b.goalFactory;
    }

    public String id()                 { return id; }
    public String displayName()        { return displayName; }
    public String spriteDir()          { return spriteDir; }
    public int    width()              { return width; }
    public int    height()             { return height; }
    public int    hitboxWidth()        { return hitboxWidth; }
    public int    hitboxHeight()       { return hitboxHeight; }
    public int    hitboxRelX()         { return (width  - hitboxWidth)  / 2; }
    public int    hitboxRelY()         { return (height - hitboxHeight) / 2; }
    public double speed()              { return speed; }
    public Map<String, Double> terrainTable() { return terrainTable; }
    public int    maxHealth()          { return maxHealth; }
    public WeaponLoadout loadout()     { return loadout; }
    public Faction faction()           { return faction; }
    public ShipReaction reaction()     { return reaction; }
    public boolean boardable()         { return interiorDimension != null; }
    public Identifier interiorDimension() { return interiorDimension; }
    public ShipGoal newGoal(GameContext ctx) {
        return goalFactory == null ? null : goalFactory.apply(ctx);
    }

    public static Builder builder(String id) { return new Builder(id); }

    /** Mutable builder; call {@link #build()} once. */
    public static final class Builder {
        private final String id;
        private String displayName;
        private String spriteDir;
        private int width = 192, height = 192;
        private int hitboxWidth = 144, hitboxHeight = 144;
        private double speed = 4.0;
        private Map<String, Double> terrainTable;
        private int maxHealth = 30;
        private WeaponLoadout loadout = WeaponLoadout.NONE;
        private Faction faction = Faction.NEUTRAL;
        private ShipReaction reaction = ShipReaction.IGNORE;
        private Identifier interiorDimension;
        private Function<GameContext, ShipGoal> goalFactory;

        private Builder(String id) { this.id = id; this.displayName = id; }

        public Builder displayName(String v)       { this.displayName = v; return this; }
        public Builder spriteDir(String v)         { this.spriteDir = v; return this; }
        public Builder size(int w, int h)          { this.width = w; this.height = h; return this; }
        public Builder hitbox(int w, int h)        { this.hitboxWidth = w; this.hitboxHeight = h; return this; }
        public Builder speed(double v)             { this.speed = v; return this; }
        public Builder terrain(Map<String, Double> v) { this.terrainTable = v; return this; }
        public Builder maxHealth(int v)            { this.maxHealth = v; return this; }
        public Builder loadout(WeaponLoadout v)    { this.loadout = v; return this; }
        public Builder faction(Faction v)          { this.faction = v; return this; }
        public Builder reaction(ShipReaction v)    { this.reaction = v; return this; }
        public Builder interior(Identifier v)      { this.interiorDimension = v; return this; }
        public Builder goal(Function<GameContext, ShipGoal> v) { this.goalFactory = v; return this; }

        public ShipKind build() {
            if (terrainTable == null) terrainTable = ShipKindRegistry.defaultWaterTerrain();
            return new ShipKind(this);
        }
    }
}
