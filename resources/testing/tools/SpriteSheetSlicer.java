package resources.testing.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Offline tool (run by hand, NOT part of the game loop): slices every object
 * sprite out of mega_spritesheet.png into the per-object folder convention the
 * ObjectImageLoader discovers (objects/&lt;category&gt;/&lt;name&gt;/&lt;name&gt;.png).
 *
 * Slicing is FIXED-GRID: the cell rectangles below were measured directly from
 * the sheet (7 rows, 96 cells) rather than detected at runtime, so the result is
 * deterministic and matches {@link resources.domain.object.ObjectCatalog} by
 * name. Within each cell the bright-magenta background is chroma-keyed to
 * transparent and the opaque content is tightly cropped.
 *
 * Run: java -cp out resources.testing.tools.SpriteSheetSlicer
 */
public final class SpriteSheetSlicer {

    private static final String SHEET    = "resources/images/objects/_spritesheets/mega_spritesheet.png";
    private static final String OUT_ROOT = "resources/images/objects/";
    private static final int PAD = 2; // transparent margin around each tight crop

    /**
     * One row per cell: {row, col, x0, x1, y0, y1, solid, wTiles, name, category}.
     * x/y are inclusive pixel bounds of the cell on the sheet. Measured offline;
     * see resources/testing/tools/mega_grid.json for the raw geometry.
     */
    private static final Object[][] CELLS = {
        {0,0,22,95,17,104,0,1,"bush","nature/misc"},
        {0,1,131,204,17,104,0,1,"shrub","nature/misc"},
        {0,2,243,303,17,104,0,1,"fern","nature/misc"},
        {0,3,346,415,17,104,0,1,"tall_grass","nature/misc"},
        {0,4,457,515,17,104,1,1,"flower","nature/misc"},
        {0,5,554,635,17,104,1,1,"log","nature/misc"},
        {0,6,667,746,17,104,1,1,"wood_pile","nature/misc"},
        {0,7,779,852,17,104,1,1,"tree_stump","nature/misc"},
        {0,8,878,980,17,104,1,2,"driftwood","nature/misc"},
        {0,9,1005,1083,17,104,1,1,"rock","nature/misc"},
        {0,10,1113,1192,17,104,0,1,"stone_pile","nature/misc"},
        {0,11,1226,1305,17,104,1,1,"boulder","nature/misc"},
        {0,12,1339,1396,17,104,1,1,"ore_rock","nature/misc"},
        {0,13,1443,1506,17,104,1,1,"cactus","nature/misc"},
        {1,0,22,94,120,213,1,1,"crate","structures/misc"},
        {1,1,134,200,120,213,1,1,"misc_barrel","structures/misc"},
        {1,2,239,310,120,213,1,1,"basket","structures/misc"},
        {1,3,347,414,120,213,1,1,"clay_pot","structures/misc"},
        {1,4,450,527,120,213,1,1,"sack","structures/misc"},
        {1,5,554,633,120,213,1,1,"brick_wall","structures/misc"},
        {1,6,666,747,120,213,1,1,"plank_wall","structures/misc"},
        {1,7,782,851,120,213,1,1,"log_wall","structures/misc"},
        {1,8,878,966,120,213,1,1,"picket_fence","structures/misc"},
        {1,9,1005,1076,120,213,1,1,"stone_post","structures/misc"},
        {1,10,1115,1186,120,213,1,1,"signpost","structures/misc"},
        {1,11,1227,1291,120,213,1,1,"torii_gate","structures/misc"},
        {1,12,1333,1394,120,213,1,1,"totem_pole","structures/misc"},
        {1,13,1445,1511,120,213,1,1,"crate_2","structures/misc"},
        {2,0,22,95,231,322,1,1,"furnace","structures/crafting"},
        {2,1,138,200,231,322,1,1,"kiln","structures/crafting"},
        {2,2,243,306,231,322,1,1,"anvil","structures/crafting"},
        {2,3,346,415,231,322,1,1,"loom","structures/crafting"},
        {2,4,450,527,231,322,1,1,"spinning_wheel","structures/crafting"},
        {2,5,547,645,231,322,1,1,"forge","structures/crafting"},
        {2,6,670,743,231,322,1,1,"cauldron","structures/crafting"},
        {2,7,780,851,231,322,1,1,"workbench","structures/crafting"},
        {2,8,891,966,231,322,1,1,"table","structures/crafting"},
        {2,9,1002,1086,231,322,1,1,"stool","structures/crafting"},
        {2,10,1100,1185,231,322,1,1,"bookshelf","structures/crafting"},
        {2,11,1223,1291,231,322,1,1,"cabinet","structures/crafting"},
        {2,12,1333,1394,231,322,1,1,"barrel_rack","structures/crafting"},
        {2,13,1435,1524,231,322,1,1,"shelf","structures/crafting"},
        {3,0,34,82,340,438,0,1,"candle","structures/lights"},
        {3,1,141,191,340,438,0,1,"wall_torch","structures/lights"},
        {3,2,245,302,340,438,0,1,"lantern","structures/lights"},
        {3,3,348,415,340,438,0,1,"standing_lamp","structures/lights"},
        {3,4,454,520,340,438,0,1,"brazier","structures/lights"},
        {3,5,547,645,340,438,0,1,"hanging_lantern","structures/lights"},
        {3,6,669,743,340,438,0,1,"lamp_post","structures/lights"},
        {3,7,778,852,340,438,0,1,"red_banner","structures/lights"},
        {3,8,889,967,340,438,0,1,"blue_banner","structures/lights"},
        {3,9,1007,1080,340,438,0,1,"pennant","structures/lights"},
        {3,10,1100,1185,340,438,0,1,"wall_scroll","structures/lights"},
        {3,11,1221,1288,340,438,0,1,"hanging_sign","structures/lights"},
        {3,12,1339,1390,340,438,0,1,"chime","structures/lights"},
        {3,13,1440,1504,340,438,0,1,"mobile","structures/lights"},
        {4,0,31,87,462,530,0,1,"lettuce","nature/food"},
        {4,1,137,193,462,530,0,1,"cabbage","nature/food"},
        {4,2,242,298,462,530,0,1,"tomato","nature/food"},
        {4,3,339,402,462,530,0,1,"corn","nature/food"},
        {4,4,438,554,462,530,0,2,"market_stall","nature/food"},
        {4,5,558,636,462,530,0,1,"onion","nature/food"},
        {4,6,672,734,462,530,0,1,"garlic","nature/food"},
        {4,7,774,838,462,530,0,1,"misc_carrot","nature/food"},
        {4,8,865,956,462,530,0,1,"wheat_sheaf","nature/food"},
        {4,9,1017,1096,462,530,0,1,"pepper","nature/food"},
        {4,10,1120,1184,462,530,0,1,"eggplant","nature/food"},
        {4,11,1229,1280,462,530,0,1,"radish","nature/food"},
        {4,12,1340,1389,462,530,0,1,"turnip","nature/food"},
        {5,0,31,87,557,619,0,1,"misc_sword","items/tools"},
        {5,1,137,193,557,619,0,1,"misc_axe","items/tools"},
        {5,2,242,298,557,619,0,1,"misc_pickaxe","items/tools"},
        {5,3,346,403,557,619,0,1,"misc_hoe","items/tools"},
        {5,4,454,515,557,619,0,1,"scythe","items/tools"},
        {5,5,557,632,557,619,0,1,"misc_hammer","items/tools"},
        {5,6,671,734,557,619,0,1,"misc_shovel","items/tools"},
        {5,7,791,840,557,619,0,1,"fishing_rod","items/tools"},
        {5,8,903,958,557,619,0,1,"misc_watering_can","items/tools"},
        {5,9,1016,1097,557,619,0,1,"bucket","items/tools"},
        {5,10,1119,1186,557,619,0,1,"torch_item","items/tools"},
        {5,11,1228,1282,557,619,0,1,"key","items/tools"},
        {5,12,1339,1391,557,619,0,1,"gem","items/tools"},
        {6,0,26,85,640,712,0,1,"potion_red","items/misc"},
        {6,1,131,192,640,712,0,1,"potion_blue","items/misc"},
        {6,2,223,284,640,712,0,1,"misc_meat","items/misc"},
        {6,3,330,462,640,712,0,2,"misc_hide","items/misc"},
        {6,4,499,554,640,712,0,1,"gold_pile","items/misc"},
        {6,5,591,641,640,712,0,1,"cheese","items/misc"},
        {6,6,682,735,640,712,0,1,"bread","items/misc"},
        {6,7,774,826,640,712,0,1,"fish","items/misc"},
        {6,8,865,913,640,712,0,1,"egg","items/misc"},
        {6,9,958,1007,640,712,0,1,"apple","items/misc"},
        {6,10,1051,1097,640,712,0,1,"misc_mushroom","items/misc"},
        {6,11,1137,1185,640,712,0,1,"herb","items/misc"},
        {6,12,1238,1281,640,712,0,1,"scroll","items/misc"},
        {6,13,1346,1499,640,712,0,2,"treasure_chest","items/misc"},
    };

    public static void main(String[] args) throws Exception {
        BufferedImage sheet = ImageIO.read(new File(SHEET));
        int written = 0;
        for (Object[] c : CELLS) {
            int x0 = (int) c[2], x1 = (int) c[3], y0 = (int) c[4], y1 = (int) c[5];
            String name = (String) c[8];
            String cat  = (String) c[9];
            BufferedImage crop = cropCell(sheet, x0, y0, x1, y1);
            if (crop == null) {
                System.err.println("SKIP empty cell: " + name);
                continue;
            }
            File dir = new File(OUT_ROOT + cat + "/" + name);
            dir.mkdirs();
            ImageIO.write(crop, "png", new File(dir, name + ".png"));
            written++;
        }
        System.out.println("Sliced " + written + " / " + CELLS.length + " cells.");
    }

    /** Bright-magenta chroma key: R and B both high, G low. */
    private static boolean isBackground(int argb) {
        int a = (argb >>> 24) & 0xff;
        if (a < 16) return true;
        int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff, b = argb & 0xff;
        return r > 90 && b > 90 && g < 90 && Math.abs(r - b) < 95;
    }

    /**
     * Chroma-key the cell rectangle to transparency, then tightly crop to the
     * opaque content with a small transparent pad. Returns null if the cell has
     * no foreground pixels.
     */
    private static BufferedImage cropCell(BufferedImage sheet, int x0, int y0, int x1, int y1) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                if (x < 0 || y < 0 || x >= sheet.getWidth() || y >= sheet.getHeight()) continue;
                if (!isBackground(sheet.getRGB(x, y))) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return null;
        int w = maxX - minX + 1, h = maxY - minY + 1;
        BufferedImage out = new BufferedImage(w + PAD * 2, h + PAD * 2, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = sheet.getRGB(minX + x, minY + y);
                out.setRGB(x + PAD, y + PAD, isBackground(argb) ? 0x00000000 : argb);
            }
        }
        return out;
    }
}
