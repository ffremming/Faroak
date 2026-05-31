package resources.presentation.ui;

import java.util.IdentityHashMap;
import java.util.Map;

import resources.app.GamePanel;
import resources.domain.entity.BaseEntity;
import resources.domain.inventory.Inventory;

/**
 * Opens a container UI backed by replicated authoritative inventories.
 */
public final class NetworkContainerUIBridge {

    private static final Map<BaseEntity, Container[]> OPEN = new IdentityHashMap<>();

    private NetworkContainerUIBridge() {}

    public static void open(
            GamePanel panel,
            BaseEntity owner,
            String inventoryType,
            Inventory containerInventory,
            Inventory playerInventory) {
        if (panel == null || owner == null || containerInventory == null || playerInventory == null) return;
        if (OPEN.containsKey(owner)) return;

        int containerX = panel.screenWidth / 2 - 265;
        int containerY = panel.screenHeight / 2 - 320;
        Container containerUi = containerUi(panel, inventoryType, containerInventory, containerX, containerY);
        containerUi.visible = true;
        containerUi.enable();

        PlayerInventory playerUi = new PlayerInventory(
            panel, Math.max(1, playerInventory.getSize() / 9), 9, 400, 300, playerInventory);
        playerUi.center(panel.userInterface().getCenter());
        playerUi.visible = true;
        playerUi.enable();

        panel.userInterface().add(containerUi);
        panel.userInterface().add(playerUi);
        panel.userInterface().enable();
        OPEN.put(owner, new Container[] { containerUi, playerUi });
        panel.userInterface().openOverlay(containerUi, () -> close(panel, owner));
    }

    public static void close(GamePanel panel, BaseEntity owner) {
        if (panel == null || owner == null) return;
        Container[] pair = OPEN.remove(owner);
        if (pair == null) return;
        for (Container ui : pair) {
            if (ui == null) continue;
            ui.visible = false;
            ui.disable();
            panel.userInterface().remove(ui);
            panel.userInterface().closeOverlay(ui);
        }
    }

    private static Container containerUi(
            GamePanel panel,
            String inventoryType,
            Inventory inventory,
            int x,
            int y) {
        String type = inventoryType == null ? "" : inventoryType.trim().toLowerCase();
        if ("barrel".equals(type)) return new BarrelContainerUI(panel, inventory, x, y);
        return new ChestContainerUI(panel, inventory, x, y);
    }
}
