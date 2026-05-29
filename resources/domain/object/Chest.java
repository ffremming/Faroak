package resources.domain.object;

import resources.app.GamePanel;
import resources.domain.player.Playable;
import resources.presentation.ui.ChestUIBridge;

/**
 * Solid 64x64 placeable container with a 27-slot {@link ChestInventory}.
 * Same lifecycle/persistence pattern as {@link Barrel}: instance lives in
 * the chunk entity list so contents survive unload/reload via the existing
 * chunk snapshot serializer.
 */
public class Chest extends GameObject {

    private static final int TILE = 64;

    private ChestInventory chestInventory;

    public Chest(GamePanel panel, int worldX, int worldY) {
        super(panel, "chest", worldX, worldY,
              TILE, TILE,
              TILE, TILE,
              0, 0,
              true);
    }

    public ChestInventory getChestInventory() {
        if (chestInventory == null) chestInventory = new ChestInventory(panel);
        return chestInventory;
    }

    @Override
    public void interact(Playable playable) {
        ChestUIBridge.open(playable.panel, this);
    }

    @Override
    public GameObject placementCandidate(GamePanel targetPanel) {
        return new Chest(targetPanel, (int) getWorldX(), (int) getWorldY());
    }
}
