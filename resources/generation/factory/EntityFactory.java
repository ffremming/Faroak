package resources.generation.factory;

import resources.domain.entity.BaseEntity;
import resources.domain.tile.Tile;
import resources.app.GamePanel;
import resources.generation.biome.Biome;
import resources.generation.factory.ObjectCatalog.ObjectSpec;
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
    private static final long SALT_TILE_VARIANT = 5;

    /** Global scale applied to every biome's combined vegetation density. Lower = sparser world. */
    private static final double VEGETATION_DENSITY_SCALE = 0.5;

    public final GamePanel panel;
    public final ProceduralGen proceduralGen;

    public EntityFactory(GamePanel panel) {
        this(panel, new ProceduralGen());
    }

    public EntityFactory(GamePanel panel, ProceduralGen proceduralGen) {
        this.panel = panel;
        this.proceduralGen = proceduralGen;
    }

    /** Patch frequency for variant selection — ~0.003 gives ~200-400 tile patches; each biome
     *  region typically contains only a couple of variant sections rather than a checkerboard. */
    private static final double VARIANT_PATCH_FREQ = 0.003;

    /** Ground tile for the given world coordinate. Never returns null. */
    public Tile getTile(int worldX, int worldY) {
        Biome biome = proceduralGen.biomeAt(worldX, worldY);
        int variantIdx = pickVariantIndex(biome, worldX, worldY);
        String tileName = variantIdx < 0 ? biome.tileName : biome.tileVariants.get(variantIdx);
        int altitude = altitudeFor(biome, worldX, worldY);
        return new Tile(panel, tileName, worldX, worldY, altitude);
    }

    /**
     * Winner-take-all over N independent low-frequency noise fields, one per
     * variant. The variant whose field samples highest at (x,y) wins. This
     * gives every variant roughly equal area (no tail-slivers like a
     * single-field threshold would), and patch boundaries follow the
     * smooth contours where two noise fields cross — large, organic, and
     * never tiny.
     *
     * Returns -1 when the biome has no variants — caller uses {@code biome.tileName}.
     */
    private int pickVariantIndex(Biome biome, int worldX, int worldY) {
        int n = biome.tileVariants.size();
        if (n == 0) return -1;
        if (n == 1) return 0;
        int bestIdx = 0;
        double bestVal = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double v = proceduralGen.patchNoise(worldX, worldY,
                    SALT_TILE_VARIANT + (long) i * 977L, VARIANT_PATCH_FREQ);
            if (v > bestVal) { bestVal = v; bestIdx = i; }
        }
        return bestIdx;
    }

    /** Optional decorative / interactive entity on the given tile. Returns null on empty tiles. */
    public BaseEntity getEntity(int worldX, int worldY) {
        Biome biome = proceduralGen.biomeAt(worldX, worldY);
        if (biome.vegetation.isEmpty()) return null;

        VegetationRule rule = pickRule(biome, worldX, worldY);
        if (rule == null) return null;

        ObjectSpec spec = ObjectFactory.spec(rule.objectName);
        int x = worldX + jitter(worldX, worldY, SALT_JITTER_X, panel.tileSize - spec.width);
        int y = worldY + jitter(worldX, worldY, SALT_JITTER_Y, panel.tileSize - spec.height);

        return ObjectFactory.create(panel, rule.objectName, x, y);
    }

    /**
     * Sum the densities of the biome's rules; if a roll falls inside that combined budget, return
     * the rule whose density slice it lands in. Otherwise the tile stays empty.
     */
    private VegetationRule pickRule(Biome biome, int worldX, int worldY) {
        double totalDensity = 0;
        for (VegetationRule r : biome.vegetation) totalDensity += r.density;
        totalDensity *= VEGETATION_DENSITY_SCALE;
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
        if ("beach".equals(biome.tileName) || "wetBeach".equals(biome.tileName) || "tidalSand".equals(biome.tileName) || "riverbank".equals(biome.id)) return 50;
        if ("mountain".equals(biome.tileName)) return 600;
        double h = proceduralGen.height(worldX, worldY);
        return 100 + (int) ((h + 1.0) * 150);
    }
}
