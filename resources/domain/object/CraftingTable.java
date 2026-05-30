package resources.domain.object;

import resources.app.GamePanel;
import resources.domain.crafting.CraftingGrid;
import resources.domain.crafting.CraftingService;
import resources.domain.player.Playable;
import resources.presentation.ui.CraftingUIBridge;

/**
 * Solid 64x64 placeable workbench. Holds a {@link CraftingGrid} + matching
 * {@link CraftingService} and opens the crafting UI on interact.
 *
 * Mirrors the {@link Barrel} pattern so the placement / persistence /
 * interact paths in the rest of the engine work without special-casing:
 *   - lazy initialisation of the service (the panel.items() template path
 *     constructs the entity before any player exists)
 *   - {@link #placementCandidate(GamePanel)} returns a fresh table when the
 *     player places one, so each placed table has its own grid state
 *   - because the table instance lives in the chunk's entity list, its grid
 *     state survives chunk unload/reload via the existing memento serializer
 */
public class CraftingTable extends GameObject {

    private static final int TILE = 64;

    private CraftingService service;

    public CraftingTable(GamePanel panel, int worldX, int worldY) {
        super(panel, "crafting_table", worldX, worldY,
              TILE, TILE,
              TILE, TILE,
              0, 0,
              true);
    }

    public CraftingService getService() {
        if (service == null) {
            service = new CraftingService(panel, new CraftingGrid(panel));
        }
        return service;
    }

    @Override
    public void interact(Playable playable) {
        CraftingUIBridge.open(playable.panel, this);
    }

    @Override
    public GameObject placementCandidate(GamePanel targetPanel) {
        return new CraftingTable(targetPanel, (int) getWorldX(), (int) getWorldY());
    }
}
