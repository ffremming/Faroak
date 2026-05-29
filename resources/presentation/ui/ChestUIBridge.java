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

    private static final int DEFAULT_X = 280;
    private static final int DEFAULT_Y = 260;

    private static final Map<Chest, ChestContainerUI> OPEN = new IdentityHashMap<>();

    private ChestUIBridge() {}

    public static void open(GamePanel panel, Chest chest) {
        if (panel == null || chest == null) return;
        if (OPEN.containsKey(chest)) return;

        ChestContainerUI ui = new ChestContainerUI(
            panel, chest.getChestInventory(), DEFAULT_X, DEFAULT_Y);
        ui.visible = true;
        ui.enable();

        panel.userInterface.add(ui);
        panel.userInterface.enable();
        OPEN.put(chest, ui);
    }

    public static void close(GamePanel panel, Chest chest) {
        if (panel == null || chest == null) return;
        ChestContainerUI ui = OPEN.remove(chest);
        if (ui == null) return;
        ui.visible = false;
        ui.disable();
        panel.userInterface.remove(ui);
    }
}
