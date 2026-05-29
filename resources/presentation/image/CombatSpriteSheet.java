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

    private static boolean attemptedLoad;
    private static BufferedImage sheet;

    private CombatSpriteSheet() {}

    public static BufferedImage itemSprite(String name) {
        if (name == null || name.isBlank()) return null;
        int idx = itemIndex(name);
        if (idx < 0) return null;
        return cell(idx);
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
            BufferedImage src = cell(idx);
            if (src == null) continue;
            out.add(ImageContainer.scaleImage(src, dim, dim));
        }
        return out;
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
