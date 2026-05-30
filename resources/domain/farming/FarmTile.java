package resources.domain.farming;

import java.awt.Point;

import resources.app.GamePanel;
import resources.domain.tile.Tile;

/**
 * A tilled ground cell. Unlike the old {@link Farmland} GameObject (which was
 * layered on top of a grass tile), a FarmTile <em>is</em> the terrain tile:
 * hoeing swaps the grass {@link Tile} in the chunk grid for a FarmTile, so the
 * change lives on the tile layer and reads as "the ground itself changed."
 *
 * Farming state (watered + the planted crop) lives here, the single source of
 * truth for a farmed cell. The planted {@link Crop} is still a separate
 * world entity (so it can grow + be harvested through the normal component
 * pipeline), but this tile owns the reference and the one-crop-per-tile rule.
 *
 * Sprite name flips between {@link #SOIL_SPRITE} and {@link #WATERED_SPRITE};
 * both are registered into the tile image cache at bootstrap (see
 * {@link FarmingRegistry#registerTileSprites}) so the name resolves to the
 * existing farmland art with no new assets.
 */
public final class FarmTile extends Tile {

    public static final String SOIL_SPRITE    = "farmland";
    public static final String WATERED_SPRITE = "farmland_watered";

    private boolean watered;
    private Crop crop;

    public FarmTile(GamePanel panel, int worldX, int worldY, int altitude) {
        super(panel, SOIL_SPRITE, worldX, worldY, altitude);
    }

    /** Build a FarmTile that inherits the terrain attributes of the tile it
     *  replaces, so altitude/floor/cliff stay consistent with the surroundings. */
    public static FarmTile from(Tile source) {
        FarmTile ft = new FarmTile(source.panel,
            (int) source.getWorldX(), (int) source.getWorldY(), source.getAltitude());
        return ft;
    }

    public boolean isWatered() { return watered; }
    public boolean isPlanted() { return crop != null; }
    public Crop crop()         { return crop; }

    public void water() {
        if (watered) return;
        watered = true;
        retexture(WATERED_SPRITE);
    }

    /**
     * Spawn a {@link Crop} on this tile and adopt it. Returns true if planting
     * succeeded; false if already planted or the world rejected the crop.
     */
    public boolean plant(String cropName) {
        if (isPlanted()) return false;
        Crop c = new Crop(panel, cropName, (int) getWorldX(), (int) getWorldY());
        if (!panel.world.placeEntity(c)) return false;
        crop = c;
        c.onRemoved(this::clearCrop); // free the tile when the crop is harvested
        return true;
    }

    /** Clear the crop reference once it has been harvested/removed, freeing the
     *  tile for replanting. The crop entity itself is removed via the normal
     *  harvest removal queue. */
    public void clearCrop() {
        crop = null;
    }
}
