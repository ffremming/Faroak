package resources.domain.farming;

import resources.app.GamePanel;
import resources.domain.inventory.Stack;
import resources.domain.object.GameObject;
import resources.domain.player.Playable;

/**
 * A tile-sized non-solid soil patch where {@link Crop}s may be planted.
 *
 * State:
 *   - {@code watered}: a flag advertised to crops/growth modifiers.
 *   - {@code planted}: whether a crop already occupies this tile (one-per-tile
 *                      to keep gameplay simple and rendering legible).
 *
 * Interaction rules (see {@link #interact(Playable)}):
 *   - If the player has a "watering_can" equipped, the tile is watered.
 *   - If the player has a seed item equipped ({@code crop_*} item name), a
 *     {@link Crop} is spawned on top and one seed is consumed.
 *
 * The actual tool/seed naming is data-driven via {@link CropRegistry} — no
 * hard-coded crop names beyond the "crop_" prefix used as a seed marker.
 */
public final class Farmland extends GameObject {

    private static final String FARMLAND_SPRITE  = "farmland";
    private static final String WATERED_SPRITE   = "farmland_watered";
    private static final String WATERING_CAN     = "watering_can";
    private static final String SEED_PREFIX      = "crop_";

    private boolean watered;
    private boolean planted;

    public Farmland(GamePanel panel, int worldX, int worldY) {
        super(panel, FARMLAND_SPRITE, worldX, worldY,
            panel.tileSize, panel.tileSize,
            panel.tileSize, panel.tileSize, 0, 0, false);
    }

    public boolean isWatered() { return watered; }
    public boolean isPlanted() { return planted; }

    public void water() {
        if (watered) return;
        watered = true;
        this.name = WATERED_SPRITE;
        getImage();
    }

    /**
     * Spawn a {@link Crop} on top of this farmland and mark it planted.
     * @return true if planting succeeded.
     */
    public boolean plant(String cropName) {
        if (planted) return false;
        Crop crop = new Crop(panel, cropName, (int) getWorldX(), (int) getWorldY());
        if (!panel.world.placeEntity(crop)) return false;
        planted = true;
        return true;
    }

    @Override
    public void interact(Playable playable) {
        Stack equipped = playable.getEquipped();
        if (equipped == null || equipped.isEmpty()) return;
        String itemName = equipped.getName();
        if (itemName == null) return;

        if (WATERING_CAN.equals(itemName)) {
            water();
            return;
        }
        if (itemName.startsWith(SEED_PREFIX) && !planted) {
            if (plant(itemName)) equipped.removeOneItem();
        }
    }
}
