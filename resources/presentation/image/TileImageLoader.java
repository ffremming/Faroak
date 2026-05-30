package resources.presentation.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Loads and caches tile sprites. Owns the canonical biome ↔ file mappings and
 * provides graceful fallbacks (colour swatch) when a sprite is missing.
 *
 * Kept separate from ImageContainer so the loading policy (paths, fallbacks,
 * rotation variants) can change without touching the cache surface that
 * everything else uses.
 */
public final class TileImageLoader {

    private static final String TILE_DIR = "resources/images/tile/";
    private static final int    TILE_PX  = 64;

    private final Map<String, BufferedImage> cache;

    public TileImageLoader(Map<String, BufferedImage> cache) {
        this.cache = cache;
        registerCanonicalBiomes();
    }

    /** Look up by tile name; loads or generates a fallback on miss. */
    public BufferedImage get(String name) {
        BufferedImage cached = cache.get(name);
        if (cached != null) return cached;
        BufferedImage loaded = loadFromDisk(name);
        if (loaded != null) return cache(name, loaded);
        return cache(name, fallback(name));
    }

    public boolean spriteExists(String relativePath) {
        try { return ImageIO.read(new File("resources/images/" + relativePath + ".png")) != null; }
        catch (IOException e) { return false; }
    }

    // ---- bootstrap: canonical biome tiles ----

    private void registerCanonicalBiomes() {
        loadTile("plains",          "plains.png");
        loadTile("swamp",           "mud.png");
        loadTile("seasonal forest", "seasonal forest.png");
        loadTile("ocean",           "ocean0.png");
        loadTile("shallowWater",    "shallowWater0.png");
        loadTile("mediumWater",     "mediumWater0.png");
        loadTile("savanna",         "savanna.png");
        loadTile("desert",          "desert.png");
        loadTile("forest",          "forest.png");
        loadTile("beach",           "beach.png");
        loadTile("wetBeach",        "wetBeach.png");
        loadTile("tidalSand",       "tidalSand.png");
        loadTile("mountain",        "rockCliff0.png");

        alias("snowy Tundra", "plains");
        alias("snowy taiga",  "forest");
        alias("rain_forest",  "forest");
        alias("rain forest",  "forest");
        alias("riverbank",    "beach");
    }

    private void loadTile(String key, String fileName) {
        File file = new File(TILE_DIR + fileName);
        if (file.exists()) {
            try {
                cache.put(key, ImageOps.scale(ImageIO.read(file), TILE_PX, TILE_PX));
                return;
            } catch (IOException e) {
                System.out.println("failed to read tile " + fileName + ": " + e.getMessage());
            }
        } else {
            System.out.println("tile sprite missing: " + fileName + " (using fallback for '" + key + "')");
        }
        cache.put(key, fallback(key));
    }

    private void alias(String alias, String existingKey) {
        BufferedImage img = cache.get(existingKey);
        cache.put(alias, img != null ? img : fallback(alias));
    }

    // ---- on-demand load (B / C border variants etc.) ----

    private BufferedImage loadFromDisk(String name) {
        try {
            return ImageOps.scale(ImageIO.read(new File(TILE_DIR + name + ".png")), 32, 32);
        } catch (IOException e) {
            return loadRotatedVariant(name);
        }
    }

    /** "<base>B<n>" or "<base>C<n>" derived by rotating a canonical sprite. */
    private BufferedImage loadRotatedVariant(String name) {
        try {
            int trailing = trailingNumber(name);
            if (trailing < 0) return null;
            int degrees = trailing * 90 - 90;
            String stripped = stripTrailingNumber(name);
            char kind = stripped.charAt(stripped.length() - 1);
            String sourceSuffix = (kind == 'C') ? "0" : (kind == 'B') ? "1" : null;
            if (sourceSuffix == null) return null;
            String sourceKey = stripped + sourceSuffix;
            if (!spriteExists("tile/" + sourceKey)) return null;
            return ImageOps.scale(ImageOps.rotate(get(sourceKey), degrees), 32, 32);
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage cache(String key, BufferedImage img) {
        cache.put(key, img);
        return img;
    }

    // ---- fallback colour swatch ----

    private static final Map<String, Color> COLOR_HINTS = buildColorHints();

    private BufferedImage fallback(String name) {
        BufferedImage img = new BufferedImage(TILE_PX, TILE_PX, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(colorFor(name));
        g.fillRect(0, 0, TILE_PX, TILE_PX);
        g.dispose();
        return img;
    }

    private Color colorFor(String name) {
        if (name == null) return Color.GRAY;
        String n = name.toLowerCase();
        for (Map.Entry<String, Color> e : COLOR_HINTS.entrySet()) {
            if (n.contains(e.getKey())) return e.getValue();
        }
        return new Color(0xA0, 0xC0, 0x80);
    }

    private static Map<String, Color> buildColorHints() {
        Map<String, Color> m = new HashMap<>();
        m.put("ocean",    new Color(0x2E, 0x6B, 0xB5));
        m.put("shallowwater", new Color(0x5F, 0x9C, 0xE2));
        m.put("water",    new Color(0x2E, 0x6B, 0xB5));
        m.put("river",    new Color(0x2E, 0x6B, 0xB5));
        m.put("beach",    new Color(0xE6, 0xD2, 0x9C));
        m.put("sand",     new Color(0xE6, 0xD2, 0x9C));
        m.put("desert",   new Color(0xD9, 0xB8, 0x76));
        m.put("snow",     new Color(0xDC, 0xE6, 0xEE));
        m.put("icy",      new Color(0xDC, 0xE6, 0xEE));
        m.put("ice",      new Color(0xDC, 0xE6, 0xEE));
        m.put("mountain", new Color(0x80, 0x80, 0x80));
        m.put("cliff",    new Color(0x80, 0x80, 0x80));
        m.put("rock",     new Color(0x80, 0x80, 0x80));
        m.put("savanna",  new Color(0xCD, 0xBE, 0x70));
        m.put("swamp",    new Color(0x4E, 0x4E, 0x35));
        m.put("moss",     new Color(0x4E, 0x4E, 0x35));
        m.put("mud",      new Color(0x4E, 0x4E, 0x35));
        m.put("forest",   new Color(0x40, 0x80, 0x00));
        return m;
    }

    static int trailingNumber(String s) {
        try { return Integer.parseInt(s.replaceAll(".*[^\\d](\\d+)$", "$1")); }
        catch (NumberFormatException e) { return -1; }
    }

    static String stripTrailingNumber(String s) { return s.replaceAll("\\d+$", ""); }
}
