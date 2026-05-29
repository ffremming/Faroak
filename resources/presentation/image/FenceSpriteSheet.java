package resources.presentation.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Slices the fence autotile spritesheet into its 16 connection tiles.
 *
 * The sheet is a single horizontal strip of 16 equal-width tiles, indexed
 * 0..15 left-to-right. The index IS the 4-bit N/E/S/W connection mask
 * (N=1, E=2, S=4, W=8 — see {@link resources.domain.object.Fence}), so tile
 * {@code N} is exactly the fence that connects in the directions encoded by
 * mask {@code N}. No lookup table or filename guessing: the artist authored
 * the strip in mask order, so {@link #tile(int)} maps mask → sprite directly.
 *
 * Cell size is inferred from the sheet width / 16, so any strip whose width is
 * a multiple of 16 works (e.g. 512x32 → 32px tiles, 1024x64 → 64px tiles).
 */
public final class FenceSpriteSheet {

    /** Number of tiles in the strip — one per 4-bit connection mask value. */
    public static final int TILE_COUNT = 16;

    private static final String[] CANDIDATE_PATHS = {
        "resources/images/objects/structures/walls/fences/fence_sheet.png",
        "resources/images/objects/_spritesheets/fence_sheet.png",
    };

    private static boolean attemptedLoad;
    private static BufferedImage sheet;

    private FenceSpriteSheet() {}

    /** True once a sheet has been found on disk and decoded. */
    public static boolean isAvailable() {
        return loadSheet() != null;
    }

    /**
     * The tile for a 4-bit connection mask (0..15), or {@code null} if no sheet
     * is present so callers can fall back to the legacy per-file art.
     */
    public static BufferedImage tile(int mask4bit) {
        BufferedImage src = loadSheet();
        if (src == null) return null;
        int idx = mask4bit & 0xF;

        int cellW = src.getWidth() / TILE_COUNT;
        int cellH = src.getHeight();
        if (cellW <= 0 || cellH <= 0) return null;

        int x = idx * cellW;
        if (x + cellW > src.getWidth()) return null;
        return copySubImage(src, x, 0, cellW, cellH);
    }

    private static BufferedImage loadSheet() {
        if (attemptedLoad) return sheet;
        attemptedLoad = true;
        for (String path : CANDIDATE_PATHS) {
            File f = new File(path);
            if (!f.isFile()) continue;
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    sheet = img;
                    return sheet;
                }
            } catch (IOException ignored) {
                // try next candidate path
            }
        }
        return null;
    }

    private static BufferedImage copySubImage(BufferedImage src, int x, int y, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, w, h, x, y, x + w, y + h, null);
        g.dispose();
        return out;
    }
}
