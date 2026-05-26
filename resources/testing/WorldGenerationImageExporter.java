package resources.testing;

import resources.generation.biome.Biome;
import resources.generation.noise.ProceduralGen;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Exports a top-down biome color map from the procedural generator.
 *
 * Args:
 *   [0] seed (optional, random if omitted)
 *   [1] width in tiles (optional, default 1800)
 *   [2] height in tiles (optional, default 1200)
 *   [3] output path (optional, default perf-results/worldgen/world_<seed>_<wxh>.png)
 *   [4] tile scale in pixels (optional, default 1)
 *   [5] feature scale (optional, default 1.0; >1 makes larger biome regions)
 */
public final class WorldGenerationImageExporter {

    private static final int DEFAULT_WIDTH_TILES = 1800;
    private static final int DEFAULT_HEIGHT_TILES = 1200;
    private static final int TILE_SIZE_PIXELS = 64;
    private static final int DEFAULT_TILE_SCALE = 1;
    private static final double DEFAULT_FEATURE_SCALE = 1.0;

    private WorldGenerationImageExporter() {}

    public static void main(String[] args) throws IOException {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : new SecureRandom().nextLong();
        int widthTiles = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_WIDTH_TILES;
        int heightTiles = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_HEIGHT_TILES;
        int tileScale = args.length > 4 ? Integer.parseInt(args[4]) : DEFAULT_TILE_SCALE;
        double featureScale = args.length > 5 ? Double.parseDouble(args[5]) : DEFAULT_FEATURE_SCALE;
        String featureScaleTag = String.valueOf(featureScale).replace('.', '_');

        String defaultOut = String.format(
            "perf-results/worldgen/world_%d_%dx%d_s%d_f%s.png",
            seed, widthTiles, heightTiles, tileScale, featureScaleTag);
        Path output = Path.of(args.length > 3 ? args[3] : defaultOut);
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        BufferedImage image = generate(seed, widthTiles, heightTiles, tileScale, featureScale);
        ImageIO.write(image, "png", output.toFile());

        System.out.println("seed=" + seed);
        System.out.println("width_tiles=" + widthTiles);
        System.out.println("height_tiles=" + heightTiles);
        System.out.println("tile_scale=" + tileScale);
        System.out.println("feature_scale=" + featureScale);
        System.out.println("output=" + output.toAbsolutePath());
    }

    public static BufferedImage generate(
        long seed,
        int widthTiles,
        int heightTiles,
        int tileScale,
        double featureScale) {
        if (widthTiles <= 0 || heightTiles <= 0) {
            throw new IllegalArgumentException("width/height must be > 0");
        }
        if (tileScale <= 0) {
            throw new IllegalArgumentException("tileScale must be > 0");
        }
        if (featureScale <= 0.0) {
            throw new IllegalArgumentException("featureScale must be > 0");
        }

        ProceduralGen gen = new ProceduralGen(seed);
        BufferedImage image = new BufferedImage(
            widthTiles * tileScale,
            heightTiles * tileScale,
            BufferedImage.TYPE_INT_RGB);

        for (int tileY = 0; tileY < heightTiles; tileY++) {
            int worldY = (int) (((tileY * TILE_SIZE_PIXELS) + (TILE_SIZE_PIXELS / 2.0)) / featureScale);
            for (int tileX = 0; tileX < widthTiles; tileX++) {
                int worldX = (int) (((tileX * TILE_SIZE_PIXELS) + (TILE_SIZE_PIXELS / 2.0)) / featureScale);
                Biome biome = gen.biomeAt(worldX, worldY);
                int rgb = colorForBiome(biome).getRGB();
                int px0 = tileX * tileScale;
                int py0 = tileY * tileScale;
                for (int dy = 0; dy < tileScale; dy++) {
                    for (int dx = 0; dx < tileScale; dx++) {
                        image.setRGB(px0 + dx, py0 + dy, rgb);
                    }
                }
            }
        }
        return image;
    }

    private static Color colorForBiome(Biome biome) {
        switch (biome.id) {
            case "ocean": return new Color(22, 73, 138);
            case "river": return new Color(45, 115, 187);
            case "beach": return new Color(224, 209, 143);
            case "riverbank": return new Color(201, 188, 132);
            case "mountain": return new Color(120, 120, 124);
            case "icy plains": return new Color(233, 241, 246);
            case "snowy taiga": return new Color(205, 228, 233);
            case "plains": return new Color(113, 183, 86);
            case "forest": return new Color(56, 130, 62);
            case "seasonal forest": return new Color(86, 149, 68);
            case "swamp": return new Color(74, 108, 72);
            case "savanna": return new Color(182, 173, 86);
            case "desert": return new Color(224, 194, 114);
            case "rain forest": return new Color(34, 108, 52);
            default: return new Color(255, 0, 255);
        }
    }
}
