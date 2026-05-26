package resources.domain.tile;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import resources.presentation.image.ImageContainer;

/**
 * Resolves the image stack a tile should draw: base sprite + per-side "B"
 * border overlays for higher neighbours + per-corner "C" overlays where two
 * adjacent borders share the same neighbour.
 *
 * Owning this here keeps Tile small and makes the border policy swappable
 * (e.g. for auto-connecting fences/walls later — they reuse the same B/C
 * machinery with different rules).
 *
 * Sprite-name convention:
 *   - "&lt;tile&gt;"          – base sprite
 *   - "&lt;tile&gt;&lt;frame&gt;"   – animated base sprite (frame 0/1/2…)
 *   - "&lt;neighbour&gt;B&lt;side&gt;" – higher-neighbour border on a given side
 *   - "&lt;neighbour&gt;C&lt;corner&gt;" – corner where two borders meet
 */
public final class TileBorderResolver {

    private final Tile tile;
    private final ImageContainer images;

    public TileBorderResolver(Tile tile, ImageContainer images) {
        this.tile   = tile;
        this.images = images;
    }

    /** Replace {@code tile.images} with the full stack for the given animation frame. */
    public void resolveInto(ArrayList<BufferedImage> sink, int frame) {
        sink.clear();
        sink.add(baseFrame(frame));
        boolean[] borders = addBorders(sink, frame);
        addCorners(sink, frame, borders);
    }

    private BufferedImage baseFrame(int frame) {
        BufferedImage animated = images.getTileImage(tile.getName() + frame);
        return animated != null ? animated : images.getTileImage(tile.getName());
    }

    private boolean[] addBorders(ArrayList<BufferedImage> sink, int frame) {
        boolean[] borders = new boolean[4];
        for (int side = 0; side < 4; side++) {
            Tile neighbor = tile.getNeighbors()[side];
            if (neighbor == null || !tile.isLowerThan(neighbor)) continue;
            sink.add(images.getTileImage(borderKey(neighbor.getName(), frame, side)));
            borders[side] = true;
        }
        return borders;
    }

    private void addCorners(ArrayList<BufferedImage> sink, int frame, boolean[] borders) {
        Tile[] n = tile.getNeighbors();
        addCornerIfMatch(sink, frame, borders, n, 0, 1, 1);
        addCornerIfMatch(sink, frame, borders, n, 1, 2, 2);
        addCornerIfMatch(sink, frame, borders, n, 2, 3, 3);
        addCornerIfMatch(sink, frame, borders, n, 3, 0, 4);
    }

    private void addCornerIfMatch(ArrayList<BufferedImage> sink, int frame, boolean[] borders,
                                  Tile[] n, int sideA, int sideB, int corner) {
        if (!(borders[sideA] && borders[sideB])) return;
        if (!n[sideA].getName().equals(n[sideB].getName())) return;
        sink.add(images.getTileImage(cornerKey(n[sideA].getName(), frame, corner)));
    }

    private String borderKey(String neighbor, int frame, int side) {
        if (frame == 0) return neighbor + "B" + side;
        if (!ImageContainer.doesPNGFileExist("tile/" + neighbor + frame + "B1")) {
            return neighbor + "B" + side;
        }
        return neighbor + frame + "B" + side;
    }

    private String cornerKey(String neighbor, int frame, int corner) {
        if (frame == 0) return neighbor + "C" + corner;
        if (!ImageContainer.doesPNGFileExist("tile/" + neighbor + "1C0")) {
            return neighbor + "C" + corner;
        }
        return neighbor + frame + "C" + corner;
    }
}
