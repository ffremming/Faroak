package resources.presentation.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Loads and caches the sprites for placeable game objects, inventory items,
 * and the player's directional sprite set.
 *
 * Object sprite convention: each object name maps to a folder named
 * {@code <name>/} somewhere under {@code resources/images/objects/} that
 * contains a single PNG named {@code <name>.png}. The objects tree is
 * organised into nested categories (nature/plants/trees/oak_M/oak_M.png,
 * nature/rocks/ores/rock_crystal/rock_crystal.png, …) purely for human
 * findability; the loader discovers the canonical PNG by scanning the tree
 * once at first use, so an object's name is decoupled from its folder depth.
 * Re-categorising an asset is a pure file move — no code change. Folders
 * missing their canonical PNG resolve to a shared "?" placeholder.
 *
 * Directories whose name starts with "_" (e.g. {@code _spritesheets}) hold
 * source art rather than per-object sprites and are skipped by the scan.
 *
 * Object names may carry a comma-suffix tag ("oak_M,preview") which signals
 * "draw the source object with reduced transparency"; that branch is handled
 * here so callers don't have to.
 */
public final class ObjectImageLoader {

    private static final String OBJECTS_DIR  = "resources/images/objects/";
    private static final String ITEMS_DIR    = "resources/images/items/";
    private static final String PLAYABLE_DIR = "resources/images/playable/";
    private static final String PREVIEW_SUFFIX = ",preview";

    private final Map<String, ArrayList<BufferedImage>> objectCache;
    private final Map<String, BufferedImage>            itemCache;

    public ObjectImageLoader(Map<String, ArrayList<BufferedImage>> objectCache,
                             Map<String, BufferedImage> itemCache) {
        this.objectCache = objectCache;
        this.itemCache   = itemCache;
    }

    // ---- object sprites ----

    public ArrayList<BufferedImage> objectImages(String name) {
        ArrayList<BufferedImage> cached = objectCache.get(name);
        if (cached != null) return cached;
        if (isPreviewVariant(name)) return previewFor(name);
        return loadObjectFolder(name);
    }

    private ArrayList<BufferedImage> previewFor(String previewName) {
        String original = previewSourceName(previewName);
        ArrayList<BufferedImage> source = objectImages(original);
        ArrayList<BufferedImage> faded = new ArrayList<>(source.size());
        for (BufferedImage img : source) faded.add(ImageOps.reduceTransparency(img));
        objectCache.put(previewName, faded);
        return faded;
    }

    private static boolean isPreviewVariant(String name) {
        return name.endsWith(PREVIEW_SUFFIX);
    }

    private static String previewSourceName(String previewName) {
        return previewName.substring(0, previewName.length() - PREVIEW_SUFFIX.length());
    }

    private ArrayList<BufferedImage> loadObjectFolder(String name) {
        ArrayList<BufferedImage> images = new ArrayList<>();
        BufferedImage fenceTile = fenceVariantTile(name);
        if (fenceTile != null) {
            images.add(fenceTile);
        } else {
            File png = resolveObjectPng(name);
            if (png.isFile()) tryAdd(images, png);
        }
        if (images.isEmpty()) images.add(PLACEHOLDER);
        objectCache.put(name, images);
        return images;
    }

    /**
     * Resolve a {@code "fence_v<mask>"} name to its tile sliced from the fence
     * autotile sheet, where {@code <mask>} is the integer 0..15 connection mask.
     * Returns null if the name isn't a fence variant or no sheet is on disk, so
     * the caller falls back to the legacy per-file fence art.
     */
    private BufferedImage fenceVariantTile(String name) {
        if (name == null || !name.startsWith(FENCE_VARIANT_PREFIX)) return null;
        String suffix = name.substring(FENCE_VARIANT_PREFIX.length());
        try {
            return FenceSpriteSheet.tile(Integer.parseInt(suffix));
        } catch (NumberFormatException notNumericMask) {
            return null; // expected: legacy named variant (e.g. "fence_vBfenceStandard")
        }
    }

    /**
     * Resolve an object name to its PNG file on disk. The standard convention
     * is a folder {@code <name>/} containing {@code <name>.png}, located
     * anywhere in the nested {@code objects/} category tree; its location is
     * discovered by {@link #objectIndex()}. Some asset sets (notably the fence
     * variant pack) instead share one folder across many named sprites: names
     * of the form {@code fence_v<basename>} resolve to the fence folder's
     * {@code <basename>.png} so the 20-PNG fence pack stays in one place.
     */
    private File resolveObjectPng(String name) {
        if (name != null && name.startsWith(FENCE_VARIANT_PREFIX)) {
            String remainder = name.substring(FENCE_VARIANT_PREFIX.length());
            File fenceDir = objectIndex().get(FENCE_FOLDER);
            if (fenceDir != null) return new File(fenceDir, remainder + ".png");
            return new File(OBJECTS_DIR + "structures/walls/" + FENCE_FOLDER + "/" + remainder + ".png");
        }
        File dir = name == null ? null : objectIndex().get(name);
        if (dir != null) return new File(dir, name + ".png");
        // Fall back to the flat convention so a freshly added top-level folder
        // works before the (cached) index is rebuilt.
        return new File(OBJECTS_DIR + name + "/" + name + ".png");
    }

    private static final String FENCE_VARIANT_PREFIX = "fence_v";
    /** Folder name for the shared fence variant pack (under structures/walls/). */
    private static final String FENCE_FOLDER = "fences";

    /**
     * Lazily-built map from object-folder name → that folder on disk, found by
     * walking the {@code objects/} category tree once. Directories starting
     * with "_" (source-art holders like {@code _spritesheets}) are skipped.
     * The first matching folder for a given name wins; names are unique in
     * practice so depth doesn't matter.
     */
    private static volatile Map<String, File> OBJECT_INDEX;

    private static Map<String, File> objectIndex() {
        Map<String, File> idx = OBJECT_INDEX;
        if (idx == null) {
            synchronized (ObjectImageLoader.class) {
                idx = OBJECT_INDEX;
                if (idx == null) {
                    idx = new java.util.HashMap<>();
                    indexFolders(new File(OBJECTS_DIR), idx);
                    OBJECT_INDEX = idx;
                }
            }
        }
        return idx;
    }

    private static void indexFolders(File dir, Map<String, File> sink) {
        File[] children = dir.listFiles(File::isDirectory);
        if (children == null) return;
        for (File child : children) {
            if (child.getName().startsWith("_")) continue;
            sink.putIfAbsent(child.getName(), child);
            indexFolders(child, sink);
        }
    }

    /**
     * Single shared neutral placeholder for any asset name with no PNG on
     * disk. A dark-grey "?" tile is visually unobtrusive and immediately
     * recognisable as "art pending".
     */
    private static BufferedImage fallbackSwatch() {
        return PLACEHOLDER;
    }

    private static final BufferedImage PLACEHOLDER = buildPlaceholder();

    private static BufferedImage buildPlaceholder() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(60, 60, 60, 200));
        g.fillRect(2, 2, 60, 60);
        g.setColor(new Color(120, 120, 120));
        g.drawRect(2, 2, 60, 60);
        g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 32f));
        g.setColor(new Color(170, 170, 170));
        g.drawString("?", 24, 42);
        g.dispose();
        return img;
    }

    private void tryAdd(ArrayList<BufferedImage> sink, File f) {
        try {
            BufferedImage img = ImageIO.read(f);
            if (img != null) sink.add(img);
        } catch (IOException e) { System.err.println("[ObjectImageLoader] skipping unreadable file " + f + ": " + e); }
    }

    // ---- item sprites ----

    public BufferedImage itemImage(String name) {
        BufferedImage cached = itemCache.get(name);
        if (cached != null) return cached;
        return loadItem(name);
    }

    private BufferedImage loadItem(String name) {
        BufferedImage fromSheet = CombatSpriteSheet.itemSprite(name);
        if (fromSheet != null) {
            itemCache.put(name, fromSheet);
            return fromSheet;
        }

        File file = new File(ITEMS_DIR + name + ".png");
        BufferedImage image = null;
        if (file.exists()) {
            try { image = ImageIO.read(file); }
            catch (IOException e) { System.err.println("[ObjectImageLoader] error reading item " + name + " (" + file + "): " + e); }
        } else {
            String alias = fallbackAlias(name);
            if (alias != null && !alias.equals(name)) {
                image = itemImage(alias);
            }
            ArrayList<BufferedImage> potential = objectImages(name);
            if ((image == null) && !potential.isEmpty()) image = potential.get(0);
        }
        if (image == null) image = fallbackSwatch();
        itemCache.put(name, image);
        return image;
    }

    private static String fallbackAlias(String name) {
        if (name == null) return null;
        switch (name) {
            case "sword":       return "axe";
            case "pickaxe":     return "axe";
            case "hoe":         return "hammer";
            case "shovel":      return "hammer";
            case "combat_bolt":
            case "projectile_bolt":
            case "bolt":        return "block";
            default:            return null;
        }
    }

    // ---- player sprite set ----

    /** Loads the 12 directional frames for a playable colour preset. */
    public ArrayList<BufferedImage> playableImages(String name) {
        ArrayList<BufferedImage> out = new ArrayList<>();
        String[] directions = {"up", "right", "down", "left"};
        for (String dir : directions) {
            for (int frame = 1; frame <= 3; frame++) {
                try { out.add(ImageIO.read(new File(PLAYABLE_DIR + name + "/" + dir + frame + ".png"))); }
                catch (IOException e) { System.err.println("[ObjectImageLoader] " + name + " missing frame " + dir + frame + ": " + e); }
            }
        }
        return out;
    }
}
