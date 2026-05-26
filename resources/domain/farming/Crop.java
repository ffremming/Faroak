package resources.domain.farming;

import java.awt.image.BufferedImage;

import resources.app.GamePanel;
import resources.domain.entity.component.GrowableComponent;
import resources.domain.entity.component.HarvestableComponent;
import resources.domain.inventory.DropSpec;
import resources.domain.inventory.DropTable;
import resources.domain.object.GameObject;

/**
 * A planted crop. Composes:
 *   - {@link GrowableComponent} to advance through stages over time, and
 *   - {@link HarvestableComponent} which only "lands" once the crop is mature.
 *
 * The sprite name is derived per-stage ({@code cropName + "_stage" + stage})
 * so the image system (incl. fallback-swatch variation) shows visible growth
 * without any new PNG assets. Non-solid so the player can walk through.
 */
public final class Crop extends GameObject {

    /** Default stages + ticks-per-stage; tuning lives here, not in callers. */
    private static final int  STAGES         = 4;
    private static final long TICKS_PER_STAGE = 600L;
    private static final int  HITBOX_SIZE     = 48;

    private final String baseName;
    private int currentStage = 0;

    public Crop(GamePanel panel, String cropName, int worldX, int worldY) {
        super(panel,
            cropName + "_stage0",
            worldX, worldY,
            panel.tileSize, panel.tileSize,
            HITBOX_SIZE, HITBOX_SIZE,
            (panel.tileSize - HITBOX_SIZE) / 2,
            (panel.tileSize - HITBOX_SIZE) / 2,
            false);
        this.baseName = cropName;

        CropRegistry.Entry entry = CropRegistry.get(cropName);
        String produce      = entry != null ? entry.produceName  : cropName;
        String requiredTool = entry != null ? entry.requiredTool : null;

        GrowableComponent growth = GrowableComponent.perTicks(panel.clock(), STAGES, TICKS_PER_STAGE);
        addComponent(growth);
        addComponent(new HarvestableComponent(
            requiredTool, 1, DropTable.of(new DropSpec(produce, 1, 3))));

        growth.onStageChanged(this::onStageChanged);
    }

    private void onStageChanged(int newStage) {
        currentStage = newStage;
        setName(baseName + "_stage" + newStage);
        getImage(); // refresh sprite stack from the image container
    }

    /** Current stage (0..stageCount-1). */
    public int stage() { return currentStage; }

    /** True once the crop has reached its final growth stage. */
    public boolean isMature() {
        GrowableComponent g = getComponent(GrowableComponent.class);
        return g != null && g.isMature();
    }

    @Override
    public String getName() {
        return baseName + "_stage" + currentStage;
    }

    @Override
    public BufferedImage getImage() {
        // Re-fetch using the stage-stamped name so sprite tracks growth.
        this.name = getName();
        return super.getImage();
    }
}
