package resources.domain.object;

import resources.app.GamePanel;
import resources.domain.player.Playable;
import resources.presentation.ui.BarrelUIBridge;

/**
 * Solid 64x64 placeable container. Holds a {@link BarrelInventory} (9 slots)
 * and opens a barrel UI on interact.
 */
public class Barrel extends GameObject {

    private static final int TILE = 64;

    private BarrelInventory barrelInventory;

    public Barrel(GamePanel panel, int worldX, int worldY) {
        super(panel, "barrel", worldX, worldY,
              TILE, TILE,
              TILE, TILE,
              0, 0,
              true);
        // Lazy: ItemManager constructs Barrel as a physical-representation
        // template before the panel.itemM field is published, so we can't
        // build the inventory eagerly. Initialised on first access.
    }

    public BarrelInventory getBarrelInventory() {
        if (barrelInventory == null) barrelInventory = new BarrelInventory(panel);
        return barrelInventory;
    }

    @Override
    public void interact(Playable playable) {
        BarrelUIBridge.openBarrel(playable.panel, this);
    }

    @Override
    public GameObject placementCandidate(GamePanel targetPanel) {
        return new Barrel(targetPanel, (int) getWorldX(), (int) getWorldY());
    }
}
