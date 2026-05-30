package resources.domain.tile;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * Per-tile-name substitution for the overlay sprite family drawn ON the
     * lower tile. Lets beach neighbours render as "wetBeach<side>" on the
     * adjacent ocean tile so the transition shares the water animation cadence.
     */
    private static final Map<String, String> OVERLAY_FAMILY = buildOverlayFamilyMap();

    /**
     * Optional override keyed by "&lt;host&gt;|&lt;neighbour&gt;". Lets a
     * specific pair use a different sprite family than the default. Used so
     * tidalSand draws a foam-less wetBeach edge (since both sides of that
     * boundary are sand — no water foam belongs there), while shallowWater
     * still gets the foamed crescent on the rare occasion it borders wetBeach
     * directly.
     */
    private static final Map<String, String> HOST_OVERLAY_FAMILY = buildHostOverlayFamilyMap();

    private static Map<String, String> buildOverlayFamilyMap() {
        Map<String, String> m = new HashMap<>();
        m.put("riverbank", "beach");
        return m;
    }

    private static Map<String, String> buildHostOverlayFamilyMap() {
        Map<String, String> m = new HashMap<>();
        m.put("tidalSand|wetBeach", "wetBeachDry");
        // Foam crescents where water meets sand and at internal depth boundaries.
        m.put("shallowWater|tidalSand", "oceanFoam");
        m.put("shallowWater|wetBeach", "oceanFoam");
        m.put("mediumWater|shallowWater", "oceanFoam");
        m.put("ocean|mediumWater", "oceanFoam");
        return m;
    }

    private final Tile tile;
    private final ImageContainer images;

    public TileBorderResolver(Tile tile, ImageContainer images) {
        this.tile   = tile;
        this.images = images;
    }

    private boolean isSeabedRevealing() {
        String n = tile.getName();
        return "shallowWater".equals(n) || "mediumWater".equals(n);
    }

    private boolean isWaterTile() {
        String n = tile.getName();
        return "ocean".equals(n) || "river".equals(n)
            || "mediumWater".equals(n) || "shallowWater".equals(n);
    }

    /**
     * Family used for the soil-plot edge drawn around a tilled {@link
     * resources.domain.farming.FarmTile}. Reuses the procedurally generated
     * {@code mudB1/mudC0} shapes (see {@code TileMaskGenerator}); the tile
     * loader derives the other sides/corners by rotation.
     */
    private static final String FARM_BORDER_FAMILY = "mud";

    /** Replace {@code tile.images} with the full stack for the given animation frame. */
    public void resolveInto(ArrayList<BufferedImage> sink, int frame) {
        sink.clear();
        // Shallow/medium water are semi-transparent: lay an opaque seabed sprite
        // underneath (hash-picked per cell) so the bottom shows through and the
        // water reads bright rather than murky.
        if (isSeabedRevealing()) {
            sink.add(images.getTileImage(
                SeabedPicker.seabedFor((int) tile.worldX, (int) tile.worldY)));
        }
        sink.add(baseFrame(frame));
        // Tilled soil draws its own plot edge against any non-farm neighbour,
        // independent of elevation. Other tiles keep the higher-neighbour rule.
        boolean[] borders = tile instanceof resources.domain.farming.FarmTile
                ? addFarmBorders(sink, frame)
                : addBorders(sink, frame);
        addCorners(sink, frame, borders);
        // Sparse surface details (bubbles/sparkles/ripples) on top of any water.
        if (isWaterTile()) {
            String detail = SeabedPicker.detailFor((int) tile.worldX, (int) tile.worldY);
            if (detail != null) sink.add(images.getTileImage(detail));
        }
    }

    /**
     * Border pass for a {@link resources.domain.farming.FarmTile}: draw the
     * {@link #FARM_BORDER_FAMILY} edge on every side whose neighbour is not also
     * a FarmTile (or is unloaded), so a tilled plot reads as soil framed against
     * the surrounding terrain and contiguous tilled tiles merge into one bed.
     */
    private boolean[] addFarmBorders(ArrayList<BufferedImage> sink, int frame) {
        boolean[] borders = new boolean[4];
        Tile[] n = tile.getNeighbors();
        for (int side = 0; side < 4; side++) {
            if (n[side] instanceof resources.domain.farming.FarmTile) continue;
            sink.add(images.getTileImage(borderKey(FARM_BORDER_FAMILY, frame, side)));
            borders[side] = true;
        }
        return borders;
    }

    private BufferedImage baseFrame(int frame) {
        String frameKey = tile.getName() + frame;
        if (ImageContainer.doesPNGFileExist("tile/" + frameKey)) {
            return images.getTileImage(frameKey);
        }
        return images.getTileImage(tile.getName());
    }

    private boolean[] addBorders(ArrayList<BufferedImage> sink, int frame) {
        boolean[] borders = new boolean[4];
        for (int side = 0; side < 4; side++) {
            Tile neighbor = tile.getNeighbors()[side];
            if (neighbor == null || !tile.isLowerThan(neighbor)) continue;
            sink.add(images.getTileImage(borderKey(overlayFamily(tile.getName(), neighbor.getName()), frame, side)));
            borders[side] = true;
        }
        return borders;
    }

    private static String overlayFamily(String hostName, String neighborName) {
        String hostScoped = HOST_OVERLAY_FAMILY.get(hostName + "|" + neighborName);
        if (hostScoped != null) return hostScoped;
        String mapped = OVERLAY_FAMILY.get(neighborName);
        return mapped != null ? mapped : neighborName;
    }

    private void addCorners(ArrayList<BufferedImage> sink, int frame, boolean[] borders) {
        if (tile instanceof resources.domain.farming.FarmTile) {
            addFarmCorners(sink, frame, borders);
            return;
        }
        Tile[] n = tile.getNeighbors();
        addCornerIfMatch(sink, frame, borders, n, 0, 1, 1);
        addCornerIfMatch(sink, frame, borders, n, 1, 2, 2);
        addCornerIfMatch(sink, frame, borders, n, 2, 3, 3);
        addCornerIfMatch(sink, frame, borders, n, 3, 0, 4);
    }

    /**
     * Corner pass for a tilled tile: wherever two adjacent plot edges meet, add
     * the matching {@link #FARM_BORDER_FAMILY} corner so the outline turns
     * cleanly. No same-neighbour check (unlike elevation borders) — the soil
     * edge is keyed on "not farm", not on the neighbour's biome.
     */
    private void addFarmCorners(ArrayList<BufferedImage> sink, int frame, boolean[] borders) {
        int[][] pairs = { {0, 1, 1}, {1, 2, 2}, {2, 3, 3}, {3, 0, 4} };
        for (int[] p : pairs) {
            if (borders[p[0]] && borders[p[1]]) {
                sink.add(images.getTileImage(cornerKey(FARM_BORDER_FAMILY, frame, p[2])));
            }
        }
    }

    private void addCornerIfMatch(ArrayList<BufferedImage> sink, int frame, boolean[] borders,
                                  Tile[] n, int sideA, int sideB, int corner) {
        if (!(borders[sideA] && borders[sideB])) return;
        if (!n[sideA].getName().equals(n[sideB].getName())) return;
        sink.add(images.getTileImage(cornerKey(overlayFamily(tile.getName(), n[sideA].getName()), frame, corner)));
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
