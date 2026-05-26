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
        File folder = new File(OBJECTS_DIR + name);
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) tryAdd(images, f);
                }
            }
        }
        if (images.isEmpty()) images.add(fallbackSwatch(name));
        objectCache.put(name, images);
        return images;
    }

    /**
     * Single shared neutral placeholder for any asset name with no PNG on
     * disk. Previously we hashed the name into RGB; that produced rainbow
     * swatches (often blueish) scattered across the inventory and the world
     * for every missing asset. A single dark-grey "?" tile is visually
     * unobtrusive and immediately recognisable as "art pending".
     *
     * Cached: the same BufferedImage is returned for every miss so we don't
     * re-allocate on each call.
     */
    private static BufferedImage fallbackSwatch(String name) {
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
        } catch (IOException ignored) { /* skip unreadable file */ }
    }

    // ---- item sprites ----

    public BufferedImage itemImage(String name) {
        BufferedImage cached = itemCache.get(name);
        if (cached != null) return cached;
        return loadItem(name);
    }

    private BufferedImage loadItem(String name) {
        File file = new File(ITEMS_DIR + name + ".png");
        BufferedImage image = null;
        if (file.exists()) {
            try { image = ImageIO.read(file); }
            catch (IOException e) { System.out.println("error reading item " + name); }
        } else {
            ArrayList<BufferedImage> potential = objectImages(name);
            if (!potential.isEmpty()) image = potential.get(0);
        }
        if (image == null) image = fallbackSwatch(name);
        itemCache.put(name, image);
        return image;
    }

    // ---- player sprite set ----

    /** Loads the 12 directional frames for a playable colour preset. */
    public ArrayList<BufferedImage> playableImages(String name) {
        ArrayList<BufferedImage> out = new ArrayList<>();
        String[] directions = {"up", "right", "down", "left"};
        for (String dir : directions) {
            for (int frame = 1; frame <= 3; frame++) {
                try { out.add(ImageIO.read(new File(PLAYABLE_DIR + name + "/" + dir + frame + ".png"))); }
                catch (IOException e) { System.out.println(name + " missing " + dir + frame); }
            }
        }
        return out;
    }
}
