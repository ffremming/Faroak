package resources.presentation.ui;

import resources.app.GamePanel;

/**
 * Open/close glue for the creative {@link ItemCatalogUI}. One catalog at a time.
 * {@link #toggle} flips it; the panel registers as a modal overlay so Escape and
 * the input layer treat it like the chest/barrel panels.
 */
public final class ItemCatalogUIBridge {

    private static ItemCatalogUI open;

    private ItemCatalogUIBridge() {}

    public static void toggle(GamePanel panel) {
        if (panel == null) return;
        if (open != null) { close(panel); return; }

        int w = 8 * 64;
        int cx = panel.screenWidth / 2 - w / 2;
        int cy = 60;
        ItemCatalogUI ui = new ItemCatalogUI(panel, cx, cy);
        ui.visible = true;
        ui.enable();
        panel.userInterface().add(ui);
        panel.userInterface().enable();
        panel.userInterface().openOverlay(ui, () -> close(panel));
        open = ui;
    }

    public static void close(GamePanel panel) {
        if (open == null) return;
        ItemCatalogUI ui = open;
        open = null;
        ui.visible = false;
        ui.disable();
        panel.userInterface().remove(ui);
        panel.userInterface().closeOverlay(ui);
    }
}
