package resources.domain.object;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import resources.domain.ship.ShipKind;
import resources.presentation.image.LazyImageCache;

/**
 * Directional sprite loading/caching for {@link Boat}. Extracted verbatim from
 * Boat's static image methods — pure, no instance state. All 8 directional
 * sprites (N, NE, E, SE, S, SW, W, NW) are decoded once and shared across every
 * Boat instance via a {@link LazyImageCache}.
 *
 * We first try the artist-provided pirate ship set under {@link #SHIP_DIR};
 * entries with no dedicated PNG (south and southwest) are derived by vertically
 * flipping their north counterparts. Anything still missing falls back to a
 * procedurally drawn arrow placeholder.
 */
final class BoatSprites {

    // Sprite footprint, mirrored from Boat for the procedural placeholder. Kept
    // local so the placeholder geometry is identical to the original.
    private static final short WIDTH  = 192;
    private static final short HEIGHT = 192;

    private static final String[] DIR_NAMES = {"e","se","s","sw","w","nw","n","ne"};

    /**
     * Mapping from compass direction → starter-ship filename in
     * resources/images/objects/vehicles/watercraft/ships/starterShip/. Numbers correspond to the
     * "(N).png" suffix on the ChatGPT-generated sprites. South and SW have no
     * dedicated art, so the loader derives them by vertically flipping the
     * north / NE variants.
     */
    private static final String SHIP_DIR = "resources/images/objects/vehicles/watercraft/ships/starterShip/";
    private static final String[] SHIP_FILES = {
        "ChatGPT Image 26. mai 2026, 20_35_01 (3).png", // 0 e
        "ChatGPT Image 26. mai 2026, 20_35_04 (7).png", // 1 se
        null,                                            // 2 s  — flip north vertically
        null,                                            // 3 sw — flip ne vertically
        "ChatGPT Image 26. mai 2026, 20_35_01 (4).png", // 4 w
        "ChatGPT Image 26. mai 2026, 20_35_04 (6).png", // 5 nw
        "ChatGPT Image 26. mai 2026, 20_35_00 (1).png", // 6 n
        "ChatGPT Image 26. mai 2026, 20_35_03 (5).png"  // 7 ne
    };

    /** Shared sprite cache — all Boat instances reuse the same 8 images. */
    private static final LazyImageCache<ArrayList<BufferedImage>> SHARED_DIRECTIONAL_IMAGES =
        new LazyImageCache<>(BoatSprites::loadDirectionalImages);

    private BoatSprites() {}

    /**
     * Load the 8 directional sprites once, then hand out the same list to every
     * Boat instance. Decoding the artist PNGs costs ~190 ms per Boat, so
     * spawning N boats was paying N× that cost; now it's paid once. The images
     * are treated as immutable — render code only reads them.
     */
    static ArrayList<BufferedImage> directionalImages() {
        return SHARED_DIRECTIONAL_IMAGES.get();
    }

    /** Per-kind directional sprite cache, keyed by kind id. */
    private static final Map<String, ArrayList<BufferedImage>> BY_KIND = new HashMap<>();

    /**
     * Directional sprites for a specific ship kind. The bundled starter-ship
     * (the player sloop) keeps using the legacy filename map; other kinds load
     * {@code <spriteDir>/<dir>.png}. Anything missing falls back to a procedural
     * placeholder sized to the kind.
     */
    static synchronized ArrayList<BufferedImage> directionalImagesFor(ShipKind kind) {
        // Preserve the exact legacy path for the player sloop so its visuals
        // are byte-for-byte identical after the refactor.
        if (kind.spriteDir() != null && kind.spriteDir().endsWith("starterShip/")
                && kind.width() == WIDTH && kind.height() == HEIGHT) {
            return directionalImages();
        }
        return BY_KIND.computeIfAbsent(kind.id(), k -> loadForKind(kind));
    }

    private static ArrayList<BufferedImage> loadForKind(ShipKind kind) {
        ArrayList<BufferedImage> out = new ArrayList<>(8);
        for (int i = 0; i < DIR_NAMES.length; i++) {
            BufferedImage img = null;
            if (kind.spriteDir() != null) img = readKindSprite(kind.spriteDir(), i);
            if (img == null) img = placeholderBoat(i, kind.width(), kind.height());
            out.add(img);
        }
        return out;
    }

    /**
     * Maps the engine's facing slot (index into {@link #DIR_NAMES} =
     * {e,se,s,sw,w,nw,n,ne}) onto the numbered art frame in the new ship sets.
     * Those sets are authored as {@code 0.png..7.png} starting at North and
     * rotating clockwise (0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW). So the
     * engine's E slot (index 0) wants art frame 2, SE wants 3, … N wants 0.
     */
    private static final int[] ENGINE_TO_ART_FRAME = { 2, 3, 4, 5, 6, 7, 0, 1 };

    /** Load the sprite for engine direction slot {@code i} from {@code dir}.
     *  Tries, in order:
     *    1. The new numbered set: {@code <frame>.png} via {@link #ENGINE_TO_ART_FRAME}.
     *    2. The named set: {@code <dir-name>.png} (e.g. e.png) with s/sw flip fallback.
     *    3. The starterShip legacy filename map. */
    private static BufferedImage readKindSprite(String dir, int i) {
        // New numbered art (0..7, 0=N clockwise) — used by the ships/<name>/ sets.
        BufferedImage numbered = readAbs(dir + ENGINE_TO_ART_FRAME[i] + ".png");
        if (numbered != null) return numbered;

        BufferedImage direct = readAbs(dir + DIR_NAMES[i] + ".png");
        if (direct != null) return direct;
        if (dir.endsWith("starterShip/")) {
            BufferedImage legacy = loadShipSprite(i);
            if (legacy != null) return legacy;
        }
        if (i == 2) { BufferedImage s = readAbs(dir + "n.png");  return s == null ? null : flipVertical(s); }
        if (i == 3) { BufferedImage s = readAbs(dir + "ne.png"); return s == null ? null : flipVertical(s); }
        return null;
    }

    private static ArrayList<BufferedImage> loadDirectionalImages() {
        ArrayList<BufferedImage> out = new ArrayList<>(8);
        for (int i = 0; i < DIR_NAMES.length; i++) {
            BufferedImage img = loadShipSprite(i);
            if (img == null) img = readBoatImage(DIR_NAMES[i]);
            if (img == null) img = placeholderBoat(i, WIDTH, HEIGHT);
            out.add(img);
        }
        return out;
    }

    /** Look up the starter-ship file for direction index {@code i}, deriving south/SW by vertical flip. */
    private static BufferedImage loadShipSprite(int i) {
        String file = SHIP_FILES[i];
        if (file != null) return readAbs(SHIP_DIR + file);
        // i==2 (south) ← flip north (i=6); i==3 (sw) ← flip ne (i=7)
        if (i == 2) {
            BufferedImage src = readAbs(SHIP_DIR + SHIP_FILES[6]);
            return src == null ? null : flipVertical(src);
        }
        if (i == 3) {
            BufferedImage src = readAbs(SHIP_DIR + SHIP_FILES[7]);
            return src == null ? null : flipVertical(src);
        }
        return null;
    }

    private static BufferedImage readAbs(String path) {
        File f = new File(path);
        if (!f.exists()) return null;
        try { return ImageIO.read(f); }
        catch (Exception e) { return null; }
    }

    private static BufferedImage flipVertical(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, src.getHeight(), src.getWidth(), -src.getHeight(), null);
        g.dispose();
        return out;
    }

    private static BufferedImage readBoatImage(String dir) {
        // Optional legacy fallback set: resources/images/objects/.../boat/<dir>.png.
        // No such folder ships today, so this resolves to null and the
        // procedural placeholder is used — left in place for future hand art.
        File f = new File("resources/images/objects/vehicles/watercraft/boat/" + dir + ".png");
        if (!f.exists()) return null;
        try { return ImageIO.read(f); }
        catch (Exception e) { return null; }
    }

    /** Procedurally-painted boat that visually points along the given direction
     *  index, sized {@code w} x {@code h} so any ship kind gets a proportionate
     *  placeholder until real art ships. */
    private static BufferedImage placeholderBoat(int dirIndex, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Hull (brown) + deck (lighter)
        g.setColor(new Color(110, 70, 35));
        Polygon hull = new Polygon();
        hull.addPoint(8, h/2);
        hull.addPoint(20, h - 6);
        hull.addPoint(w - 20, h - 6);
        hull.addPoint(w - 8, h/2);
        hull.addPoint(w - 20, 10);
        hull.addPoint(20, 10);
        g.fillPolygon(hull);
        g.setColor(new Color(165, 120, 75));
        g.fillRect(w/2 - 14, h/2 - 8, 28, 16);

        // Direction arrow pointing along DIR_NAMES[dirIndex]
        double angle = -Math.PI / 4.0 * dirIndex; // e=0, ccw
        // DIR_NAMES uses screen y-down so for "e" arrow points right (+x).
        double dx = Math.cos(angle);
        double dy = -Math.sin(angle);

        int cx = w / 2;
        int cy = h / 2;
        int len = Math.max(24, Math.min(w, h) / 4);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(240, 230, 200));
        int tipX = (int) (cx + dx * len);
        int tipY = (int) (cy + dy * len);
        g.drawLine(cx, cy, tipX, tipY);
        // Arrowhead
        double perpX = -dy;
        double perpY = dx;
        int barbX1 = (int) (tipX - dx * 8 + perpX * 5);
        int barbY1 = (int) (tipY - dy * 8 + perpY * 5);
        int barbX2 = (int) (tipX - dx * 8 - perpX * 5);
        int barbY2 = (int) (tipY - dy * 8 - perpY * 5);
        g.drawLine(tipX, tipY, barbX1, barbY1);
        g.drawLine(tipX, tipY, barbX2, barbY2);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g.setColor(new Color(60, 40, 20));
        g.setStroke(new BasicStroke(1.5f));
        g.drawPolygon(hull);

        g.dispose();
        return img;
    }
}
