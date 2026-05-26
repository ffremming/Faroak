package resources.presentation.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

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

    public ImageContainer() {}

    // ---- tile cache ----

    public BufferedImage getTileImage(String name) { return tileLoader.get(name); }

    public boolean containsImage(String name) { return images.containsKey(name); }

    public static boolean doesPNGFileExist(String relativePath) {
        return new TileImageLoader(new HashMap<>()).spriteExists(relativePath);
    }

    // ---- object + item caches ----

    public ArrayList<BufferedImage> getObjectImages(String name) { return objectLoader.objectImages(name); }
    public ArrayList<BufferedImage> getObjectImagesFromFile(String name) { return objectLoader.objectImages(name); }
    public BufferedImage            getItemImage(String name)    { return objectLoader.itemImage(name); }
    public ArrayList<BufferedImage> setPlayableImages(String name) { return objectLoader.playableImages(name); }

    // ---- image processing (thin pass-through to ImageOps) ----

    public BufferedImage getOutline(BufferedImage src)        { return ImageOps.outline(src); }
    public BufferedImage reduceTransparency(BufferedImage src){ return ImageOps.reduceTransparency(src); }
    public boolean checkIntersection(Entity a, Entity b)      { return ImageOps.pixelIntersection(a, b); }

    public static BufferedImage scaleImage(BufferedImage src, int w, int h) { return ImageOps.scale(src, w, h); }
    public static BufferedImage rotateImage(BufferedImage src, double deg)  { return ImageOps.rotate(src, deg); }
}
