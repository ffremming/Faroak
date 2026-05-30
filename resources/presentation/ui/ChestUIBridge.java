package resources.presentation.ui;

import java.util.IdentityHashMap;
import java.util.Map;

import resources.app.GamePanel;
import resources.domain.object.Chest;

/**
 * Glue between a {@link Chest} and its on-screen {@link ChestContainerUI}.
 * Mirrors {@link BarrelUIBridge}: per-chest mapping for clean close, toggles
 * UI focus so movement input is suspended while the chest is open.
 */
public final class ChestUIBridge {

    private static final Map<Chest, ChestContainerUI> OPEN = new IdentityHashMap<>();

    private ChestUIBridge() {}

    public static void open(GamePanel panel, Chest chest) {
        if (panel == null || chest == null) return;
        if (OPEN.containsKey(chest)) return;

        // Place the chest grid centered horizontally and high enough that it sits
        // ABOVE the player inventory, which renders centered on screen — the two
        // panels stack without overlapping. Width of a 9-col grid is 9*50+10*8=530.
        int chestX = panel.screenWidth / 2 - 265;
        int chestY = panel.screenHeight / 2 - 320;
        ChestContainerUI ui = new ChestContainerUI(
            panel, chest.getChestInventory(), chestX, chestY);
        ui.visible = true;
        ui.enable();

        panel.userInterface().add(ui);
        panel.userInterface().enable();
        OPEN.put(chest, ui);
        // Show the player inventory (its normal E-view, centered below the chest)
        // so items can be dragged between the two grids while the chest is open.
        panel.userInterface().openInventoryPaired(ui);
        // Register as a modal overlay so clicks route to the UI (not the world)
        // and Escape can close it.
        panel.userInterface().openOverlay(ui, () -> close(panel, chest));
    }

    public static void close(GamePanel panel, Chest chest) {
        if (panel == null || chest == null) return;
        ChestContainerUI ui = OPEN.remove(chest);
        if (ui == null) return;
        ui.visible = false;
        ui.disable();
        panel.userInterface().remove(ui);
        panel.userInterface().closeOverlay(ui);
        // Hide the player inventory that was shown alongside the chest.
        panel.userInterface().closeInventoryPaired();
    }
}
