package resources.testing;

import java.awt.event.MouseEvent;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.entity.Entity;

/**
 * Reproduces the user's "can't interact / can't place boats" bug in the SAME setup
 * the real Host Game uses: websocket backend (not loopback). Crucially it drives a
 * REAL mouse click through {@link resources.input.Mouse#mousePressed} (the path the
 * user's clicks take), not submitWorldClick() directly — so any break between the
 * mouse and the network command is exercised.
 *
 * Run: java -cp out resources.testing.InteractionDiagnostic
 */
public final class InteractionDiagnostic {

    public static void main(String[] args) throws Exception {
        System.setProperty("game.multiplayer.mode", "host");
        System.setProperty("game.multiplayer.backend", "websocket");
        System.setProperty("game.multiplayer.gateway.enabled", "true");
        System.setProperty("game.multiplayer.gatewayPort", "7811");
        System.setProperty("game.multiplayer.serverUrl", "ws://127.0.0.1:7811/ws");
        System.setProperty("game.multiplayer.serverActionRange", "768.0");
        // Fresh world each run (don't accumulate placed objects from prior runs).
        System.setProperty("game.multiplayer.persistence", "memory");

        JFrame frame = new JFrame();
        GamePanel panel = new GamePanel(frame, true);
        frame.add(panel);
        frame.pack();

        // Let the embedded websocket server start + client connect + join.
        for (int i = 0; i < 200; i++) { panel.update(1.0); Thread.sleep(8); }
        boolean joined = panel.multiplayer() != null && panel.multiplayer().isJoined();
        System.out.println("[Int] joined=" + joined
            + " online=" + (panel.multiplayer() != null && panel.multiplayer().isOnline())
            + " mode=" + (panel.multiplayer() == null ? "?" : panel.multiplayer().mode()));
        if (!joined) {
            System.out.println("[Int] *** NOT JOINED — every interaction (submitWorldClick) returns false early. This alone breaks all clicking. ***");
        }

        // HYPOTHESIS TEST: nudge the player so publishMovement sends the position,
        // syncing the server session to the client (client-authoritative). Comment
        // this out to reproduce the "never moved -> server position stale" bug.
        panel.inputHandlingSystem.setRight(true);
        for (int i = 0; i < 4; i++) { panel.update(1.0); Thread.sleep(8); }
        panel.inputHandlingSystem.setRight(false);
        for (int i = 0; i < 8; i++) { panel.update(1.0); Thread.sleep(8); }

        double px = panel.player().getWorldX(), py = panel.player().getWorldY();
        System.out.println("[Int] player=(" + (int) px + "," + (int) py + ") camera=("
            + (int) panel.camera().getWorldX() + "," + (int) panel.camera().getWorldY() + ")");

        // Count placeable starter objects available (server-replicated inventory).
        // Select the fence hotbar slot and place a fence directly via the REAL mouse
        // path: position the mouse over a screen point and fire mousePressed.
        panel.player().getInventory().setIndex(3); // fence
        int fencesBefore = count(panel, "fence");

        // Click adjacent to the player (one tile right of the player's feet). The
        // player is standing on land, so a tile next to them should be placeable.
        int screenX = (int) (px + 64 - panel.camera().getWorldX());
        int screenY = (int) (py + 80 - panel.camera().getWorldY());
        System.out.println("[Int] simulating LEFT mouse click at screen=(" + screenX + "," + screenY
            + ") -> world=(" + (int) (panel.camera().getWorldX() + screenX) + "," + (int) (panel.camera().getWorldY() + screenY) + ")");
        // Move the engine mouse to that screen coord, then fire a real press.
        panel.mouse.mouseMoved(fakeMove(panel, screenX, screenY));
        panel.mouse.mousePressed(fakePress(panel, screenX, screenY));
        for (int i = 0; i < 40; i++) { panel.update(1.0); Thread.sleep(8); }
        int fencesAfter = count(panel, "fence");
        System.out.println("[Int] fences before=" + fencesBefore + " after=" + fencesAfter
            + "  -> placement via real mouse " + (fencesAfter > fencesBefore ? "WORKED" : "FAILED"));

        // BOAT: place on nearest water, then board it (both via the real mouse path).
        panel.player().getInventory().setIndex(6); // boat
        int[] water = nearestWater(panel, (int) panel.player().getWorldX(), (int) panel.player().getWorldY());
        if (water == null) {
            System.out.println("[Int] BOAT: no water found near player");
        } else {
            int wsx = (int) (water[0] - panel.camera().getWorldX());
            int wsy = (int) (water[1] - panel.camera().getWorldY());
            int boatsBefore = countBoats(panel);
            panel.mouse.mouseMoved(fakeMove(panel, wsx, wsy));
            panel.mouse.mousePressed(fakePress(panel, wsx, wsy));
            for (int i = 0; i < 40; i++) { panel.update(1.0); Thread.sleep(8); }
            int boatsAfter = countBoats(panel);
            System.out.println("[Int] BOAT placed: before=" + boatsBefore + " after=" + boatsAfter
                + "  -> " + (boatsAfter > boatsBefore ? "WORKED" : "FAILED"));

            // Board: switch to a non-boat item and click the nearest boat's hitbox center.
            resources.domain.object.Boat boat = nearestBoat(panel);
            if (boat != null) {
                panel.player().getInventory().setIndex(0);
                boat.getHitBox().updateCoords();
                java.awt.Rectangle hb = boat.getHitBox();
                int bsx = (int) (hb.getCenterX() - panel.camera().getWorldX());
                int bsy = (int) (hb.getCenterY() - panel.camera().getWorldY());
                boolean ridingBefore = panel.multiplayer().localPlayerRiding();
                panel.mouse.mouseMoved(fakeMove(panel, bsx, bsy));
                panel.mouse.mousePressed(fakePress(panel, bsx, bsy));
                for (int i = 0; i < 40; i++) { panel.update(1.0); Thread.sleep(8); }
                System.out.println("[Int] BOAT ride: before=" + ridingBefore
                    + " after=" + panel.multiplayer().localPlayerRiding()
                    + "  -> " + (panel.multiplayer().localPlayerRiding() ? "WORKED" : "FAILED"));
            }
        }

        panel.stopGameThread();
        System.exit(0);
    }

    private static int countBoats(GamePanel panel) {
        int n = 0;
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof resources.domain.object.Boat) n++;
        }
        return n;
    }

    private static resources.domain.object.Boat nearestBoat(GamePanel panel) {
        resources.domain.object.Boat best = null; double bd = Double.MAX_VALUE;
        double px = panel.player().getWorldX(), py = panel.player().getWorldY();
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof resources.domain.object.Boat) {
                double d = Math.hypot(e.getWorldX() - px, e.getWorldY() - py);
                if (d < bd) { bd = d; best = (resources.domain.object.Boat) e; }
            }
        }
        return best;
    }

    private static int[] nearestWater(GamePanel panel, int px, int py) {
        int ts = 64;
        for (int r = 1; r <= 12; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = px + dx * ts, y = py + dy * ts;
                    resources.domain.tile.Tile t = panel.world().getTile(new java.awt.Point(x, y));
                    String nm = (t == null || t.getName() == null) ? "" : t.getName().toLowerCase();
                    if (nm.contains("water") || nm.contains("ocean")) return new int[] { x, y };
                }
            }
        }
        return null;
    }

    private static int count(GamePanel panel, String namePart) {
        int n = 0;
        for (Entity e : panel.world().getEntities()) {
            if (e == null || e == panel.player()) continue;
            String nm = e.getName() == null ? "" : e.getName().toLowerCase();
            if (nm.contains(namePart)) n++;
        }
        return n;
    }

    private static MouseEvent fakeMove(GamePanel panel, int x, int y) {
        return new MouseEvent(panel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false);
    }

    private static MouseEvent fakePress(GamePanel panel, int x, int y) {
        return new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1);
    }
}
