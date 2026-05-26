package resources.presentation.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import resources.domain.entity.Entity;

/**
 * Asset cache + retrieval facade. Owns the three sprite caches (tile / object /
 * item) and delegates the actual loading to focused loaders ({@link TileImageLoader},
 * {@link ObjectImageLoader}). Image processing (outline, transparency,
 * intersection) is delegated to {@link ImageOps}.
 *
 * Game-side callers go through here for everything image-related; everything
 * else in this package is implementation detail.
 */
public class ImageContainer {

    public final HashMap<String, BufferedImage>            images       = new HashMap<>();
    public final HashMap<String, ArrayList<BufferedImage>> objectImages = new HashMap<>();
    public final HashMap<String, BufferedImage>            itemImages   = new HashMap<>();

    private final TileImageLoader   tileLoader   = new TileImageLoader(images);
    private final ObjectImageLoader objectLoader = new ObjectImageLoader(objectImages, itemImages);

    private final Map<BufferedImage, BufferedImage> outlineCache = new IdentityHashMap<>();

    public ImageContainer() {}

    // ---- tile cache ----

    public BufferedImage getTileImage(String name) { return tileLoader.get(name); }

    public boolean containsImage(String name) { return images.containsKey(name); }

    /**
     * Fast, cached check for "does this PNG actually live on disk?" Used in the
     * tile border / corner key lookup to decide between frame-stamped and
     * unstamped sprite variants. Uses {@link File#exists()} (no image decoding)
     * with a process-wide cache so it's effectively free after warm-up.
     */
    public static boolean doesPNGFileExist(String relativePath) {
        Boolean cached = EXISTS_CACHE.get(relativePath);
        if (cached != null) return cached;
        boolean exists = new File("resources/images/" + relativePath + ".png").exists();
        EXISTS_CACHE.put(relativePath, exists);
        return exists;
    }

    private static final ConcurrentMap<String, Boolean> EXISTS_CACHE = new ConcurrentHashMap<>();

    // ---- object + item caches ----

    public ArrayList<BufferedImage> getObjectImages(String name) { return objectLoader.objectImages(name); }
    public ArrayList<BufferedImage> getObjectImagesFromFile(String name) { return objectLoader.objectImages(name); }
    public BufferedImage            getItemImage(String name)    { return objectLoader.itemImage(name); }
    public ArrayList<BufferedImage> setPlayableImages(String name) { return objectLoader.playableImages(name); }

    // ---- image processing (thin pass-through to ImageOps) ----

    public BufferedImage getOutline(BufferedImage src) {
        BufferedImage cached = outlineCache.get(src);
        if (cached != null) return cached;
        BufferedImage outline = ImageOps.outline(src);
        outlineCache.put(src, outline);
        return outline;
    }
    public BufferedImage reduceTransparency(BufferedImage src){ return ImageOps.reduceTransparency(src); }
    public boolean checkIntersection(Entity a, Entity b)      { return ImageOps.pixelIntersection(a, b); }

    public static BufferedImage scaleImage(BufferedImage src, int w, int h) { return ImageOps.scale(src, w, h); }
    public static BufferedImage rotateImage(BufferedImage src, double deg)  { return ImageOps.rotate(src, deg); }
}
