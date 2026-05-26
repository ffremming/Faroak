package resources.generation.factory;

import resources.domain.entity.BaseEntity;
import resources.domain.object.GameObject;
import resources.domain.tile.Tile;
import resources.app.GamePanel;
import resources.generation.biome.Biome;
import resources.generation.noise.ProceduralGen;
import resources.generation.biome.VegetationRule;

/**
 * Bridges the noise/biome layer (ProceduralGen + BiomeRegistry) to the chunk loader.
 *
 * Per-coord contract used by Chunk:
 *   getTile(x, y)   -> ground tile for that world-coordinate
 *   getEntity(x, y) -> optional vegetation / structure entity placed on that tile, or null
 *
 * Generation is deterministic in (seed, x, y): reloading the same chunk yields the same world.
 */
public class EntityFactory implements resources.generation.WorldGenerator {

    /** Salt namespaces for ProceduralGen.rollAt — keeps independent decisions independent. */
    private static final long SALT_SPAWN = 1;
    private static final long SALT_PICK  = 2;
    private static final long SALT_JITTER_X = 3;
    private static final long SALT_JITTER_Y = 4;

    public final GamePanel panel;
    public final ProceduralGen proceduralGen;

    public EntityFactory(GamePanel panel) {
        this(panel, new ProceduralGen());
    }

    public EntityFactory(GamePanel panel, ProceduralGen proceduralGen) {
        this.panel = panel;
        this.proceduralGen = proceduralGen;
    }

    /** Ground tile for the given world coordinate. Never returns null. */
    public Tile getTile(int worldX, int worldY) {
        Biome biome = proceduralGen.biomeAt(worldX, worldY);
        int altitude = altitudeFor(biome, worldX, worldY);
        return new Tile(panel, biome.tileName, worldX, worldY, altitude);
    }

    /** Optional decorative / interactive entity on the given tile. Returns null on empty tiles. */
    public BaseEntity getEntity(int worldX, int worldY) {
        Biome biome = proceduralGen.biomeAt(worldX, worldY);
        if (biome.vegetation.isEmpty()) return null;

        VegetationRule rule = pickRule(biome, worldX, worldY);
        if (rule == null) return null;

        int x = worldX + jitter(worldX, worldY, SALT_JITTER_X, panel.tileSize - rule.width);
        int y = worldY + jitter(worldX, worldY, SALT_JITTER_Y, panel.tileSize - rule.height);

        return new GameObject(panel, rule.objectName,
            x, y,
            rule.width, rule.height,
            rule.hitBoxWidth, rule.hitBoxHeight,
            0, 0,
            rule.solid);
    }

    /**
     * Sum the densities of the biome's rules; if a roll falls inside that combined budget, return
     * the rule whose density slice it lands in. Otherwise the tile stays empty.
     */
    private VegetationRule pickRule(Biome biome, int worldX, int worldY) {
        double totalDensity = 0;
        for (VegetationRule r : biome.vegetation) totalDensity += r.density;
        if (totalDensity <= 0) return null;

        double spawnRoll = proceduralGen.rollAt(worldX, worldY, SALT_SPAWN);
        if (spawnRoll >= totalDensity) return null;

        double pick = proceduralGen.rollAt(worldX, worldY, SALT_PICK) * totalDensity;
        double cumulative = 0;
        for (VegetationRule r : biome.vegetation) {
            cumulative += r.density;
            if (pick <= cumulative) return r;
        }
        return biome.vegetation.get(biome.vegetation.size() - 1);
    }

    /** Centered random offset inside the tile cell for the given salt; 0 if there's no slack. */
    private int jitter(int worldX, int worldY, long salt, int slack) {
        if (slack <= 0) return 0;
        double roll = proceduralGen.rollAt(worldX, worldY, salt);
        return (int) (roll * slack) - slack / 2;
    }

    /** Map biome to an altitude bucket so TileManager's higher/lower comparisons stay meaningful. */
    private int altitudeFor(Biome biome, int worldX, int worldY) {
        if (biome.water) return 0;
        if ("beach".equals(biome.tileName) || "riverbank".equals(biome.id)) return 50;
        if ("mountain".equals(biome.tileName)) return 600;
        double h = proceduralGen.height(worldX, worldY);
        return 100 + (int) ((h + 1.0) * 150);
    }
}
