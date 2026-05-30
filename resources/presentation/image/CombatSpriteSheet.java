package resources.presentation.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

/**
 * Optional 4x3 combat sprite sheet helper.
 *
 * Expected logical layout (left->right, top->bottom):
 *   0 sword, 1 pickaxe, 2 hoe, 3 shovel
 *   4 projectile bolt, 5..8 slash frames, 9..11 hit frames
 */
public final class CombatSpriteSheet {

    private static final String[] CANDIDATE_PATHS = {
        "resources/images/objects/_spritesheets/equipment.png",
        "resources/images/items/combat_sheet.png",
        "resources/images/items/combat_spritesheet.png",
        "resources/images/items/combatSpriteSheet.png",
        "resources/images/items/combat.png"
    };

    private static final int SHEET_COLS = 4;
    private static final int SHEET_ROWS = 3;

    /**
     * Fraction of fully-transparent border trimmed off each extracted cell. The
     * source art is a single 1448x1086 sheet whose icons (weapons, crescent
     * slash arcs, spark bursts) sit inside generously padded cells, so a raw
     * width/COLS x height/ROWS slice is mostly empty space with the icon off to
     * one side. Naively scaling that slice to a small sprite turns the sparse
     * icon into a blurry coloured smear. We instead crop to the cell's opaque
     * bounding box before scaling so the actual artwork fills the sprite.
     */
    private static final int MIN_OPAQUE_ALPHA = 16;

    private static boolean attemptedLoad;
    private static BufferedImage sheet;

    private CombatSpriteSheet() {}

    public static BufferedImage itemSprite(String name) {
        if (name == null || name.isBlank()) return null;
        int idx = itemIndex(name);
        if (idx < 0) return null;
        BufferedImage cropped = cropToOpaque(cell(idx));
        return cropped != null ? cropped : cell(idx);
    }

    public static ArrayList<BufferedImage> slashFrames(int sizePx) {
        return scaledFrames(sizePx, 5, 6, 7, 8);
    }

    public static ArrayList<BufferedImage> hitFrames(int sizePx) {
        return scaledFrames(sizePx, 9, 10, 11);
    }

    private static ArrayList<BufferedImage> scaledFrames(int sizePx, int... indices) {
        ArrayList<BufferedImage> out = new ArrayList<>();
        int dim = Math.max(8, sizePx);
        for (int idx : indices) {
            BufferedImage src = cropToOpaque(cell(idx));
            if (src == null) continue;
            out.add(ImageContainer.scaleImage(src, dim, dim));
        }
        return out;
    }

    /**
     * Crop a cell to the bounding box of its non-transparent pixels so the
     * icon fills the frame. Returns null if the cell is null or fully
     * transparent; returns the input unchanged if it's already tight.
     */
    private static BufferedImage cropToOpaque(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = src.getRGB(x, y) >>> 24;
                if (alpha < MIN_OPAQUE_ALPHA) continue;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
        if (maxX < minX || maxY < minY) return null; // fully transparent
        int cw = maxX - minX + 1;
        int ch = maxY - minY + 1;
        if (minX == 0 && minY == 0 && cw == w && ch == h) return src;
        return src.getSubimage(minX, minY, cw, ch);
    }

    private static int itemIndex(String name) {
        switch (name) {
            case "sword":            return 0;
            case "pickaxe":          return 1;
            case "hoe":              return 2;
            case "shovel":           return 3;
            case "combat_bolt":
            case "projectile_bolt":
            case "bolt":             return 4;
            default:                 return -1;
        }
    }

    private static BufferedImage cell(int idx) {
        BufferedImage src = loadSheet();
        if (src == null || idx < 0) return null;
        int col = idx % SHEET_COLS;
        int row = idx / SHEET_COLS;
        if (row >= SHEET_ROWS) return null;

        int cellW = src.getWidth() / SHEET_COLS;
        int cellH = src.getHeight() / SHEET_ROWS;
        if (cellW <= 0 || cellH <= 0) return null;

        int x = col * cellW;
        int y = row * cellH;
        if (x + cellW > src.getWidth() || y + cellH > src.getHeight()) return null;
        return copySubImage(src, x, y, cellW, cellH);
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
            } catch (IOException e) {
                System.err.println("[CombatSpriteSheet] failed reading sheet candidate " + f + ": " + e);
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
