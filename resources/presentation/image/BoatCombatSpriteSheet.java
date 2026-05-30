package resources.presentation.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.imageio.ImageIO;

/**
 * Helper for the 8x8 boat-combat VFX sprite sheet.
 *
 * Expected row mapping:
 *   row 0: cannonball travel
 *   row 1: muzzle flash
 *   row 2: hit spark
 *   row 3: fire burst
 *   row 4: water splash
 *   row 5: water ripple
 *   row 6: smoke puff
 *   row 7: sink debris splash
 */
public final class BoatCombatSpriteSheet {

    private static final String[] CANDIDATE_PATHS = {
        "resources/images/effects/boat_combat_sheet.png",
        "resources/images/effects/boat_combat_spritesheet.png",
        "resources/images/effects/boatCombatSheet.png",
        "resources/images/effects/boat_fx_sheet.png"
    };

    private static final int SHEET_COLS = 8;
    private static final int SHEET_ROWS = 8;
    private static final int OPAQUE_ALPHA_THRESHOLD = 8;

    private static final ConcurrentMap<String, ArrayList<BufferedImage>> FRAME_CACHE =
        new ConcurrentHashMap<>();

    private static boolean attemptedLoad;
    private static BufferedImage sheet;

    private BoatCombatSpriteSheet() {}

    public static ArrayList<BufferedImage> projectileFrames(int sizePx) {
        return rowFrames(0, 0, 7, sizePx, sizePx);
    }

    public static ArrayList<BufferedImage> muzzleFrames(int sizePx) {
        return rowFrames(1, 0, 7, sizePx, sizePx);
    }

    public static ArrayList<BufferedImage> hitSparkFrames(int sizePx) {
        return rowFrames(2, 0, 7, sizePx, sizePx);
    }

    public static ArrayList<BufferedImage> explosionFrames(int sizePx) {
        return rowFrames(3, 0, 7, sizePx, sizePx);
    }

    public static ArrayList<BufferedImage> splashFrames(int sizePx) {
        return rowFrames(4, 0, 7, sizePx, sizePx);
    }

    public static ArrayList<BufferedImage> rippleFrames(int sizePx) {
        return rowFrames(5, 0, 7, sizePx, sizePx);
    }

    public static ArrayList<BufferedImage> smokeFrames(int sizePx) {
        return rowFrames(6, 0, 7, sizePx, sizePx);
    }

    public static ArrayList<BufferedImage> sinkDebrisFrames(int sizePx) {
        return rowFrames(7, 0, 7, sizePx, sizePx);
    }

    private static ArrayList<BufferedImage> rowFrames(int row, int fromCol, int toCol, int widthPx, int heightPx) {
        int w = Math.max(4, widthPx);
        int h = Math.max(4, heightPx);
        String key = row + ":" + fromCol + ":" + toCol + ":" + w + ":" + h;
        ArrayList<BufferedImage> cached = FRAME_CACHE.get(key);
        if (cached != null) return cached;

        ArrayList<BufferedImage> built = buildRowFrames(row, fromCol, toCol, w, h);
        FRAME_CACHE.put(key, built);
        return built;
    }

    private static ArrayList<BufferedImage> buildRowFrames(int row, int fromCol, int toCol, int widthPx, int heightPx) {
        ArrayList<BufferedImage> out = new ArrayList<>();
        BufferedImage src = loadSheet();
        if (src == null) return out;

        int cellW = src.getWidth() / SHEET_COLS;
        int cellH = src.getHeight() / SHEET_ROWS;
        if (cellW <= 0 || cellH <= 0) return out;
        if (row < 0 || row >= SHEET_ROWS) return out;

        int start = Math.max(0, Math.min(SHEET_COLS - 1, fromCol));
        int end = Math.max(0, Math.min(SHEET_COLS - 1, toCol));
        int step = start <= end ? 1 : -1;

        for (int col = start; col != end + step; col += step) {
            int x = col * cellW;
            int y = row * cellH;
            BufferedImage cell = copySubImage(src, x, y, cellW, cellH);
            BufferedImage trimmed = trimTransparent(cell);
            if (trimmed == null) continue;
            out.add(ImageContainer.scaleImage(trimmed, widthPx, heightPx));
        }
        return out;
    }

    private static BufferedImage loadSheet() {
        if (attemptedLoad) return sheet;
        attemptedLoad = true;
        for (String path : CANDIDATE_PATHS) {
            File file = new File(path);
            if (!file.isFile()) continue;
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    sheet = img;
                    return sheet;
                }
            } catch (IOException e) {
                System.err.println("[BoatCombatSpriteSheet] failed reading sheet candidate " + file + ": " + e);
                // Try next path.
            }
        }
        return null;
    }

    private static BufferedImage trimTransparent(BufferedImage src) {
        int minX = src.getWidth();
        int minY = src.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int alpha = (src.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha <= OPAQUE_ALPHA_THRESHOLD) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < minX || maxY < minY) return null;
        int pad = 1;
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(src.getWidth() - 1, maxX + pad);
        maxY = Math.min(src.getHeight() - 1, maxY + pad);
        return copySubImage(src, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage copySubImage(BufferedImage src, int x, int y, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, w, h, x, y, x + w, y + h, null);
        g.dispose();
        return out;
    }
}
