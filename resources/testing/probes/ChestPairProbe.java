package resources.testing.probes;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;
import resources.domain.inventory.Stack;
import resources.domain.object.Chest;
import resources.presentation.ui.ChestUIBridge;
import resources.testing.TestHarness;

/**
 * Verifies the chest UI opens paired with the player inventory (same wood look)
 * and that an item can be moved from the chest grid into the player inventory
 * grid via two real clicks (pick up from chest slot, drop into inventory slot).
 */
public final class ChestPairProbe {

    public static void main(String[] args) throws Exception {
        try (TestHarness h = new TestHarness()) {
            GamePanel panel = h.panel();
            int px = (int) panel.player.getWorldX();
            int py = (int) panel.player.getWorldY();
            panel.world.update(new Point(px, py));

            Chest chest = new Chest(panel, px + 64, py);
            panel.world.placeEntity(chest);
            panel.world.update(new Point(px, py));

            // Seed chest slot 0 with a real (non-empty) stack; clear player slot 0.
            Inventory chestInv = chest.getChestInventory();
            Stack seed = new Stack(panel, "wood");
            seed.addItem(new resources.domain.inventory.Item(panel, "wood"));
            seed.addItem(new resources.domain.inventory.Item(panel, "wood"));
            chestInv.setStack(0, seed);
            Inventory playerInv = panel.player.getInventory();
            playerInv.setStack(0, new Stack(panel, "empty"));

            ChestUIBridge.open(panel, chest);
            System.out.println("[OPEN] content=" + panel.userInterface.debugContentCount()
                + " modal=" + panel.userInterface.isModalUIOpen());

            // Render once so slot bounds get laid out, and capture the paired look.
            render(panel, "/tmp/chest_paired.png");

            // Read the real on-screen slot rectangles via reflection (the bounds
            // are package-private int fields set during layout) and click centers.
            Point chestSlot0 = slotCenter(panel, "chest", 0);
            Point invSlot0   = slotCenter(panel, "inventory", 0);
            System.out.println("[GEOM] chestSlot0=" + chestSlot0 + " invSlot0=" + invSlot0);

            // Direct dispatch test: does the chest overlay's own mousePressed
            // reach the slot? Isolate from the UserInterface routing.
            java.lang.reflect.Field overlaysF = panel.userInterface.getClass().getDeclaredField("overlays");
            overlaysF.setAccessible(true);
            java.util.List<?> ovs = (java.util.List<?>) overlaysF.get(panel.userInterface);
            Object chestUI = ovs.get(0);
            System.out.println("[BOUNDS] chestUI x=" + rectInt(chestUI, "x") + " y=" + rectInt(chestUI, "y")
                + " w=" + rectInt(chestUI, "width") + " h=" + rectInt(chestUI, "height"));

            click(panel, chestSlot0.x, chestSlot0.y);
            Stack hand = panel.player.getTempInHand();
            System.out.println("[PICKUP] hand=" + (hand == null ? "null" : hand.getName())
                + " chestSlot0=" + chestInv.getStack(0).getName());

            click(panel, invSlot0.x, invSlot0.y);
            Stack handAfter = panel.player.getTempInHand();
            System.out.println("[DROP] hand=" + (handAfter == null ? "null" : handAfter.getName())
                + " playerSlot0=" + playerInv.getStack(0).getName());

            render(panel, "/tmp/chest_paired_after.png");
            boolean moved = "wood".equals(playerInv.getStack(0).getName())
                && "empty".equals(chestInv.getStack(0).getName());
            System.out.println("[RESULT] item moved chest -> inventory = " + moved);

            // E should close the chest overlay AND hide the paired inventory.
            panel.keys.keyPressed(new java.awt.event.KeyEvent(panel,
                java.awt.event.KeyEvent.KEY_PRESSED, 0L, 0, java.awt.event.KeyEvent.VK_E, 'e'));
            System.out.println("[E-CLOSE] hasOverlay=" + panel.userInterface.hasOpenOverlay()
                + " modal=" + panel.userInterface.isModalUIOpen()
                + " | " + (!panel.userInterface.hasOpenOverlay() ? "CHEST CLOSED" : "STILL OPEN"));
        }
        System.exit(0);
    }

    /**
     * Reflectively read the on-screen center of slot {@code index} in either the
     * "chest" overlay or the player "inventory" grid. Bounds are package-private
     * int fields (x,y,width,height) on the slot components, set during layout.
     */
    private static Point slotCenter(GamePanel panel, String which, int index) throws Exception {
        Object uiObj;
        if ("chest".equals(which)) {
            java.lang.reflect.Field overlaysF = panel.userInterface.getClass().getDeclaredField("overlays");
            overlaysF.setAccessible(true);
            java.util.List<?> overlays = (java.util.List<?>) overlaysF.get(panel.userInterface);
            uiObj = overlays.get(0);
        } else {
            java.lang.reflect.Field invF = panel.userInterface.getClass().getDeclaredField("inventoryUI");
            invF.setAccessible(true);
            uiObj = invF.get(panel.userInterface);
        }
        // content list lives on Container.
        java.lang.reflect.Field contentF = findField(uiObj.getClass(), "content");
        contentF.setAccessible(true);
        java.util.List<?> content = (java.util.List<?>) contentF.get(uiObj);
        Object slot = content.get(index);
        int sx = rectInt(slot, "x"), sy = rectInt(slot, "y");
        int sw = rectInt(slot, "width"), sh = rectInt(slot, "height");
        return new Point(sx + sw / 2, sy + sh / 2);
    }

    private static java.lang.reflect.Field findField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static int rectInt(Object o, String name) throws Exception {
        java.lang.reflect.Field f = findField(o.getClass(), name);
        f.setAccessible(true);
        return f.getInt(o);
    }

    private static void click(GamePanel panel, int x, int y) {
        panel.mouse.mousePressed(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
            0L, 0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    private static void render(GamePanel panel, String path) throws Exception {
        BufferedImage img = new BufferedImage(panel.screenWidth, panel.screenHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        panel.paintComponent(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
    }

    private ChestPairProbe() {}
}
