package resources.generation.noise;

import resources.generation.biome.Biome;
import resources.generation.biome.BiomeRegistry;

import java.awt.image.BufferedImage;

/**
 * Stateless (per-seed) terrain sampler. Returns biome / height / temperature / humidity
 * for any world coordinate via octave-summed OpenSimplex noise.
 *
 * Frequencies are tuned for world-space pixel coordinates (one tile = 64px in GamePanel),
 * so a typical biome spans roughly 30-80 tiles.
 */
public class ProceduralGen {

    public static final long DEFAULT_SEED = 800L;

    // Frequencies are in cycles-per-pixel. 1 tile = 64px, so a wavelength of 1/FREQ
    // pixels corresponds to (1 / FREQ) / 64 tiles between similar values. Climate maps
    // run at very low frequency with a single octave so biomes form smooth, continent-
    // scale gradients instead of noisy patches.
    //
    // Archipelago height layering — three nested scales:
    //   CONTINENT_FREQ: very low-frequency mask. Drives large continents/islands
    //                   (hundreds of tiles across) and decides where open ocean is.
    //   MID_FREQ:       medium-scale layer that produces medium islands (roughly
    //                   30-60 tiles across) sprinkled in the waters around continents.
    //   ISLET_FREQ:     small-scale layer that produces tiny islands (~5-20 tiles
    //                   across) scattered through deep ocean.
    // Each smaller layer is masked so it only surfaces in the appropriate "depth"
    // band of the continent field, keeping land scales visually separated.
    private static final double CONTINENT_FREQ   = 0.000020; // ~780 tiles per feature
    private static final double MID_FREQ         = 0.00012;  // ~130 tiles per feature
    private static final double ISLET_FREQ       = 0.00045;  // ~35 tiles per feature
    private static final double TEMPERATURE_FREQ = 0.0000125;// ~1250 tiles per feature
    private static final double HUMIDITY_FREQ    = 0.0000175;// ~890 tiles per feature
    private static final double RIVER_FREQ       = 0.000065; // ~240 tiles between river meanders

    private static final int CONTINENT_OCTAVES   = 4;
    private static final int MID_OCTAVES         = 2;
    private static final int ISLET_OCTAVES       = 2;
    private static final int TEMPERATURE_OCTAVES = 1;
    private static final int HUMIDITY_OCTAVES    = 1;
    private static final int RIVER_OCTAVES       = 3;

    private static final long HEIGHT_SALT      = 0;
    private static final long MID_SALT         = 110;
    private static final long ISLET_SALT       = 70;
    private static final long TEMPERATURE_SALT = 30;
    private static final long HUMIDITY_SALT    = 50;
    private static final long RIVER_SALT       = 10;

    // How strongly to bias the continent layer toward "ocean". Higher = more open
    // ocean and tighter island clusters. Slightly reduced so the (now larger-scale)
    // continents end up a bit bigger on the surface.
    private static final double CONTINENT_BIAS = 0.14;

    // Continent-height window where medium islands surface. Just below shore so
    // they form a ring of mid-sized landmasses around the continents.
    private static final double MID_MIN = -0.45;
    private static final double MID_MAX = -0.10;
    private static final double MID_AMPLITUDE = 0.75;

    // Continent-height window where small islets surface. Deeper than the medium
    // band, so tiny islands sprinkle the open ocean between continents/mid-islands.
    private static final double ISLET_MIN = -0.70;
    private static final double ISLET_MAX = -0.40;
    private static final double ISLET_AMPLITUDE = 0.70;

    private final long seed;

    public ProceduralGen() { this(DEFAULT_SEED); }

    public ProceduralGen(long seed) { this.seed = seed; }

    public long seed() { return seed; }

    public double height(int worldX, int worldY) {
        double continent = octaves(seed + HEIGHT_SALT, worldX, worldY,
                CONTINENT_FREQ, CONTINENT_OCTAVES) - CONTINENT_BIAS;

        // Medium islands: surface in the shallow-ocean band just off the continents.
        double midMask = bandMask(continent, MID_MIN, MID_MAX);
        if (midMask > 0.0) {
            double mid = octaves(seed + MID_SALT, worldX, worldY,
                    MID_FREQ, MID_OCTAVES);
            continent += Math.max(0.0, mid) * midMask * MID_AMPLITUDE;
        }

        // Small islets: surface deeper out, sprinkling open ocean between continents.
        double isletMask = bandMask(continent, ISLET_MIN, ISLET_MAX);
        if (isletMask > 0.0) {
            double islet = octaves(seed + ISLET_SALT, worldX, worldY,
                    ISLET_FREQ, ISLET_OCTAVES);
            continent += Math.max(0.0, islet) * isletMask * ISLET_AMPLITUDE;
        }

        if (continent < -1.0) continent = -1.0;
        if (continent >  1.0) continent =  1.0;
        return continent;
    }

    /** Smooth sine hump: 0 at the edges of [lo, hi], 1 at the midpoint, 0 outside. */
    private static double bandMask(double value, double lo, double hi) {
        if (value <= lo || value >= hi) return 0.0;
        double t = (value - lo) / (hi - lo);
        return Math.sin(t * Math.PI);
    }

    public double temperature(int worldX, int worldY) {
        return octaves(seed + TEMPERATURE_SALT, worldX, worldY, TEMPERATURE_FREQ, TEMPERATURE_OCTAVES);
    }

    public double humidity(int worldX, int worldY) {
        return octaves(seed + HUMIDITY_SALT, worldX, worldY, HUMIDITY_FREQ, HUMIDITY_OCTAVES);
    }

    public double river(int worldX, int worldY) {
        return Math.abs(octaves(seed + RIVER_SALT, worldX, worldY, RIVER_FREQ, RIVER_OCTAVES));
    }

    public Biome biomeAt(int worldX, int worldY) {
        return BiomeRegistry.classify(
            height(worldX, worldY),
            temperature(worldX, worldY),
            humidity(worldX, worldY),
            river(worldX, worldY));
    }

    /**
     * Smooth low-frequency noise for tile-variant patches in [-1, 1]. The default
     * {@code freq} of ~0.04 produces blobs ~10-20 tiles across — large enough that
     * a chunk usually sits inside a single patch but with organic biome-shaped edges.
     * Use a distinct {@code salt} per decision so e.g. mud and grass don't realign.
     */
    public double patchNoise(int worldX, int worldY, long salt, double freq) {
        return OpenSimplex2S.noise2(seed + salt, worldX * freq, worldY * freq);
    }

    /** Deterministic [0,1) pseudo-random for (x,y) under given salt. Same seed + coords always returns the same value. */
    public double rollAt(int worldX, int worldY, long salt) {
        long h = (long) worldX * 73856093L
               ^ (long) worldY * 19349663L
               ^ seed         * 83492791L
               ^ salt         * 2654435761L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return (h & 0x7fffffffffffffffL) / (double) Long.MAX_VALUE;
    }

    /** Placeholder thumbnail; only TreeNode.paintMap() consumes this and it's debug-only. */
    public BufferedImage getImage(int x, int y, int width, int height) {
        int w = Math.max(1, width  / 64);
        int h = Math.max(1, height / 64);
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private double octaves(long s, int x, int y, double baseFreq, int iterations) {
        double noise = 0;
        double amp = 1;
        double maxAmp = 0;
        double freq = baseFreq;
        for (int i = 0; i < iterations; i++) {
            noise  += OpenSimplex2S.noise2_ImproveX(s, x * freq, y * freq) * amp;
            maxAmp += amp;
            amp  /= 2;
            freq *= 2;
        }
        return noise / maxAmp;
    }
}
