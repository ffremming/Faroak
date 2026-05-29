package resources.world.placement;

import java.util.function.Function;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;

/**
 * Immutable description of a placeable item: how to build it, where it's
 * allowed, whether the cursor snaps to the tile grid, what happens on click,
 * and how the ghost preview should behave.
 *
 * One {@code PlacementSpec} per item name; registered in
 * {@link PlacementRegistry}.
 */
public final class PlacementSpec {

    public enum SnapPolicy { TILE, FREE }

    public final String itemName;
    public final Function<GamePanel, BaseEntity> factory;
    public final SurfaceRule surface;
    public final SnapPolicy snap;
    public final PlacementAction action;
    public final String requiredTool;   // nullable — reserved for future tool-gated placements
    public final boolean showGhost;

    public PlacementSpec(String itemName,
                         Function<GamePanel, BaseEntity> factory,
                         SurfaceRule surface,
                         SnapPolicy snap,
                         PlacementAction action,
                         String requiredTool,
                         boolean showGhost) {
        this.itemName     = itemName;
        this.factory      = factory;
        this.surface      = surface;
        this.snap         = snap;
        this.action       = action;
        this.requiredTool = requiredTool;
        this.showGhost    = showGhost;
    }
}
