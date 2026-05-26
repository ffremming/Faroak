package resources.presentation.ui;

import java.util.IdentityHashMap;
import java.util.Map;

import resources.app.GamePanel;
import resources.domain.object.Barrel;

/**
 * Glue between a {@link Barrel} and its on-screen {@link BarrelContainerUI}.
 *
 * Keeps a per-barrel mapping so {@link #closeBarrel(GamePanel, Barrel)} can
 * locate and remove the exact UI instance attached on open. Toggles UI focus
 * via {@code UserInterface.enable/disable} so movement input is suspended
 * while the container is open.
 */
public final class BarrelUIBridge {

    /** Default screen position for a freshly opened barrel UI. */
    private static final int DEFAULT_X = 360;
    private static final int DEFAULT_Y = 260;

    private static final Map<Barrel, BarrelContainerUI> OPEN = new IdentityHashMap<>();

    private BarrelUIBridge() {}

    /**
     * Open a UI for {@code barrel} on {@code panel}. Idempotent: a second
     * open call on an already-open barrel is a no-op.
     */
    public static void openBarrel(GamePanel panel, Barrel barrel) {
        if (panel == null || barrel == null) return;
        if (OPEN.containsKey(barrel)) return;

        BarrelContainerUI ui = new BarrelContainerUI(
            panel, barrel.getBarrelInventory(), DEFAULT_X, DEFAULT_Y);
        ui.visible = true;
        ui.enable();

        panel.userInterface.add(ui);
        panel.userInterface.enable();
        OPEN.put(barrel, ui);
    }

    /**
     * Close the UI previously opened for {@code barrel}, if any. Safe to call
     * even when no UI is open.
     */
    public static void closeBarrel(GamePanel panel, Barrel barrel) {
        if (panel == null || barrel == null) return;
        BarrelContainerUI ui = OPEN.remove(barrel);
        if (ui == null) return;
        ui.visible = false;
        ui.disable();
        panel.userInterface.remove(ui);
    }
}
