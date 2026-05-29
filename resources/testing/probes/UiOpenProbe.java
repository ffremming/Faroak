package resources.testing.probes;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import resources.app.GamePanel;
import resources.domain.object.Chest;
import resources.domain.object.CraftingTable;
import resources.geometry.HitBox;
import resources.testing.TestHarness;

/**
 * Headless smoke test that drives the REAL input layer (Mouse/Keys) to find
 * where the chest / crafting / escape-menu opening breaks. Each feature runs
 * in its OWN harness so leftover entities don't cross-contaminate the
 * distance-sorted interact() pick.
 */
public final class UiOpenProbe {

    public static void main(String[] args) {
        String which = args.length > 0 ? args[0] : "all";
        switch (which) {
            case "starter":  testStarterChestVisible(); break;
            case "chest":    testChestClick();          break;
            case "crafting": testCraftingSpace();       break;
            case "escape":   testEscape();              break;
            case "inventory": testInventoryToggle();    break;
            default:
                // Single-JVM full run will hit the Registry global-state guard
                // after the first harness; run one feature per JVM instead.
                testStarterChestVisible();
        }
        System.out.println("\nDONE");
        System.exit(0);
    }

    /** Does the chest GenerationManager spawns at start actually appear? */
    private static void testStarterChestVisible() {
        try (TestHarness h = new TestHarness()) {
            GamePanel panel = h.panel();
            int px = (int) panel.player.getWorldX();
            int py = (int) panel.player.getWorldY();
            panel.world.update(new Point(px, py));
            long n = panel.world.getEntities().stream().filter(e -> e instanceof Chest).count();
            System.out.println("\n[STARTER] Chests visible in index after update = " + n
                + (n > 0 ? "  OK" : "  FAIL — starter chest never surfaces"));
        }
    }

    private static void testChestClick() {
        try (TestHarness h = new TestHarness()) {
            GamePanel panel = h.panel();
            int px = (int) panel.player.getWorldX();
            int py = (int) panel.player.getWorldY();
            Chest chest = new Chest(panel, px + 32, py + 16);
            panel.world.placeEntity(chest);
            panel.world.update(new Point(px, py));

            int screenX = (int) (chest.getWorldX() - panel.camera.getWorldX()) + 16;
            int screenY = (int) (chest.getWorldY() - panel.camera.getWorldY()) + 16;
            int before = panel.userInterface.debugContentCount();
            panel.mouse.mousePressed(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
                0L, 0, screenX, screenY, 1, false, MouseEvent.BUTTON1));
            int after = panel.userInterface.debugContentCount();
            System.out.println("\n[CHEST] open click: content " + before + " -> " + after
                + (after > before ? "  OK" : "  FAIL"));

            // After opening, the UI must be modal so slot clicks route to it.
            System.out.println("[CHEST] isModalUIOpen after open = "
                + panel.userInterface.isModalUIOpen()
                + (panel.userInterface.isModalUIOpen() ? "  OK" : "  FAIL — slot clicks would leak to world"));

            // A click anywhere now must go to the UI, not re-trigger the world
            // (which previously could open a SECOND chest / harvest underneath).
            int beforeClick = panel.userInterface.debugContentCount();
            panel.mouse.mousePressed(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
                0L, 0, 300, 280, 1, false, MouseEvent.BUTTON1));
            int afterClick = panel.userInterface.debugContentCount();
            System.out.println("[CHEST] in-UI click did NOT spawn world UI: "
                + (afterClick == beforeClick ? "OK" : "FAIL (content changed " + beforeClick + "->" + afterClick + ")"));

            // Escape must close the overlay (and NOT open the pause menu).
            boolean closed = panel.userInterface.closeTopOverlay();
            System.out.println("[CHEST] Escape closes overlay = " + closed
                + " modalNowOpen=" + panel.userInterface.isModalUIOpen()
                + " menuOpen=" + panel.userInterface.isMenuOpen()
                + ((closed && !panel.userInterface.isModalUIOpen() && !panel.userInterface.isMenuOpen()) ? "  OK" : "  FAIL"));
        }
    }

    private static void testCraftingSpace() {
        try (TestHarness h = new TestHarness()) {
            GamePanel panel = h.panel();
            int px = (int) panel.player.getWorldX();
            int py = (int) panel.player.getWorldY();
            CraftingTable table = new CraftingTable(panel, px - 8, py + 8);
            boolean placed = panel.world.placeEntity(table);
            panel.world.update(new Point(px, py));
            HitBox reach = panel.player.getInteractionHitBox();
            reach.updateCoords();
            table.getHitBox().updateCoords();
            System.out.println("\n[CRAFTING] placed=" + placed
                + " inIndex=" + panel.world.getEntities().contains(table)
                + " intersectsReach=" + table.getHitBox().intersects(reach));
            int before = panel.userInterface.debugContentCount();
            panel.keys.keyPressed(new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0L, 0,
                KeyEvent.VK_SPACE, ' '));
            int after = panel.userInterface.debugContentCount();
            System.out.println("[CRAFTING] SPACE: content " + before + " -> " + after
                + (after > before ? "  OK" : "  FAIL"));

            // Also try interact() directly to localise the failure.
            int b2 = panel.userInterface.debugContentCount();
            panel.player.interact();
            int a2 = panel.userInterface.debugContentCount();
            System.out.println("[CRAFTING] direct interact(): content " + b2 + " -> " + a2
                + (a2 > b2 ? "  OK" : "  FAIL"));
        }
    }

    /** E opens the player inventory, and E again closes it (regression guard
     *  for the Keys.java refactor that moved E ahead of the modal gate). */
    private static void testInventoryToggle() {
        try (TestHarness h = new TestHarness()) {
            GamePanel panel = h.panel();
            KeyEvent e = new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_E, 'e');
            panel.keys.keyPressed(e);
            boolean openAfter1 = panel.userInterface.isModalUIOpen();
            panel.keys.keyPressed(e);
            boolean openAfter2 = panel.userInterface.isModalUIOpen();
            System.out.println("\n[INV] E open=" + openAfter1 + " E-again close=" + (!openAfter2)
                + ((openAfter1 && !openAfter2) ? "  OK" : "  FAIL"));
        }
    }

    private static void testEscape() {
        try (TestHarness h = new TestHarness()) {
            GamePanel panel = h.panel();
            boolean b = panel.userInterface.isMenuOpen();
            panel.userInterface.toggleMenu();
            boolean a = panel.userInterface.isMenuOpen();
            System.out.println("\n[ESCAPE] toggleMenu: " + b + " -> " + a
                + (a ? "  OK" : "  FAIL"));
        }
    }

    private UiOpenProbe() {}
}
