package resources.presentation.ui;

import java.util.IdentityHashMap;
import java.util.Map;

import resources.app.GamePanel;
import resources.domain.object.CraftingTable;

/**
 * Glue between a {@link CraftingTable} and its on-screen {@link CraftingTableUI}.
 * Mirrors {@link BarrelUIBridge}: per-table mapping for clean close, toggles
 * UI focus so movement input is suspended while crafting.
 */
public final class CraftingUIBridge {

    private static final int DEFAULT_X = 320;
    private static final int DEFAULT_Y = 220;

    private static final Map<CraftingTable, CraftingTableUI> OPEN = new IdentityHashMap<>();

    private CraftingUIBridge() {}

    public static void open(GamePanel panel, CraftingTable table) {
        if (panel == null || table == null) return;
        if (OPEN.containsKey(table)) return;

        CraftingTableUI ui = new CraftingTableUI(
            panel, table.getService(), DEFAULT_X, DEFAULT_Y);
        ui.visible = true;
        ui.enable();

        panel.userInterface().add(ui);
        panel.userInterface().enable();
        OPEN.put(table, ui);
        // Register as a modal overlay so clicks route to the UI (not the world)
        // and Escape can close it.
        panel.userInterface().openOverlay(ui, () -> close(panel, table));
    }

    public static void close(GamePanel panel, CraftingTable table) {
        if (panel == null || table == null) return;
        CraftingTableUI ui = OPEN.remove(table);
        if (ui == null) return;
        ui.visible = false;
        ui.disable();
        panel.userInterface().remove(ui);
        panel.userInterface().closeOverlay(ui);
    }
}
