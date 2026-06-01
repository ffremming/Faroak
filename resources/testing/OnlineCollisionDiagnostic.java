package resources.testing;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.inventory.Inventory;

/**
 * Reproduces the SINGLE-PLAYER HOST bugs the user reports:
 *   1. "too far away" when placing — does the server think the player is somewhere
 *      other than where the client shows them?
 *   2. teleport when walking into a placed wall.
 *
 * Boots a real HOST client (which also runs the embedded loopback server in-process),
 * moves the player, then places a wall in front and walks into it — logging the
 * client player position and any placement rejection.
 *
 * Run: java -cp out resources.testing.OnlineCollisionDiagnostic
 */
public final class OnlineCollisionDiagnostic {

    public static void main(String[] args) throws Exception {
        System.setProperty("game.multiplayer.mode", "host");
        System.setProperty("game.multiplayer.backend", "loopback");
        System.setProperty("game.multiplayer.reconcileLocal", "true");
        System.setProperty("game.multiplayer.serverActionRange", "768.0");
        System.setProperty("game.multiplayer.serverMoveSpeedPerTick", "20.0");

        JFrame frame = new JFrame();
        GamePanel panel = new GamePanel(frame, true);

        // Connect + receive baseline.
        step(panel, 120);
        System.out.println("[Diag] joined=" + panel.multiplayer().isJoined());

        // OBJECT VISIBILITY: how many replicated objects exist, and do they have a
        // sprite to render? (A type the client can't render shows as nothing.)
        java.util.Map<String, int[]> byType = new java.util.TreeMap<>();
        for (resources.domain.entity.Entity e : panel.world().getEntities()) {
            if (e == null || e == panel.player()) continue;
            String nm = e.getName() == null ? "?" : e.getName();
            boolean hasImg = e.getImages() != null && !e.getImages().isEmpty();
            int[] c = byType.computeIfAbsent(nm, k -> new int[2]);
            c[0]++; if (hasImg) c[1]++;
        }
        System.out.println("[Diag] replicated objects by type (count / with-sprite):");
        for (var en : byType.entrySet()) {
            System.out.println("[Diag]   " + en.getKey() + ": " + en.getValue()[0]
                + " / " + en.getValue()[1] + (en.getValue()[1] == 0 ? "   <-- INVISIBLE (no sprite)" : ""));
        }

        double sx = panel.player().getWorldX();
        double sy = panel.player().getWorldY();
        System.out.println("[Diag] spawn=(" + (int) sx + "," + (int) sy + ")");

        // Walk RIGHT for ~1 second of real frames so the player moves a few tiles.
        panel.inputHandlingSystem.setRight(true);
        step(panel, 60);
        panel.inputHandlingSystem.setRight(false);
        step(panel, 30);

        double mx = panel.player().getWorldX();
        double my = panel.player().getWorldY();
        System.out.println("[Diag] after walking right: client player=(" + (int) mx + "," + (int) my + ")");

        // SCENARIO A: place a fence DIRECTLY in the walking path, then walk straight
        // into it and watch for teleport.
        Inventory inv = panel.player().getInventory();
        inv.setIndex(3); // fence
        double fenceX = mx + 128.0;  // 2 tiles directly right, in the walk path
        double fenceY = my;
        System.out.println("[Diag][A] place fence at (" + (int) fenceX + "," + (int) fenceY + ") in walk path");
        panel.multiplayer().submitWorldClick(fenceX, fenceY);
        step(panel, 30);
        System.out.println("[Diag][A] fences now=" + countType(panel, "fence"));
        for (resources.domain.entity.Entity e : panel.world().getEntities()) {
            if (e == null || e == panel.player()) continue;
            String nm = e.getName() == null ? "" : e.getName().toLowerCase();
            if (nm.contains("fence")) {
                System.out.println("[Diag][A] fence entity: name=" + e.getName()
                    + " pos=(" + (int) e.getWorldX() + "," + (int) e.getWorldY() + ")"
                    + " solid=" + e.isSolid()
                    + " hitbox=" + e.getHitBox());
            }
        }

        System.out.println("[Diag][A] --- walking straight into the fence, watching for teleport ---");
        panel.inputHandlingSystem.setRight(true);
        double prev = panel.player().getWorldX();
        double maxJump = 0.0;
        for (int i = 0; i < 120; i++) {
            long t0 = System.nanoTime();
            panel.update(1.0);
            double x = panel.player().getWorldX();
            double jump = Math.abs(x - prev);
            if (jump > maxJump) maxJump = jump;
            if (jump > 30.0) {
                System.out.println("[Diag][A]   TELEPORT f" + i + " x " + (int) prev + " -> " + (int) x
                    + " (" + (int) (x - prev) + "px)  [fenceX=" + (int) fenceX + "]");
            }
            prev = x;
            sleepToFrame(t0);
        }
        panel.inputHandlingSystem.setRight(false);
        System.out.println("[Diag][A] largest jump = " + String.format("%.0f", maxJump) + "px; final x=" + (int) panel.player().getWorldX());

        // SCENARIO B: place a BOAT on the nearest water (this is the "ship" the user
        // can't place). Search outward for a water tile and try to place there.
        step(panel, 10);
        double bx = panel.player().getWorldX(), by = panel.player().getWorldY();
        inv.setIndex(6); // boat
        int[] best = nearestWater(panel, (int) bx, (int) by);
        if (best == null) {
            System.out.println("[Diag][B] no water found near player within search radius");
        } else {
            double dist = Math.hypot(best[0] - bx, best[1] - by);
            System.out.println("[Diag][B] nearest water at (" + best[0] + "," + best[1] + ") dist=" + (int) dist
                + "px from player (" + (int) bx + "," + (int) by + ")");
            panel.multiplayer().submitWorldClick(best[0], best[1]);
            step(panel, 60);
            int ships = countType(panel, "boat") + countType(panel, "sloop")
                + countType(panel, "ship") + countType(panel, "brig") + countType(panel, "galleon");
            System.out.println("[Diag][B] ships/boats placed=" + ships
                + " (>0 = placement works, no 'too far away')");

            // Find the NEAREST placed boat to the player and board it.
            resources.domain.object.Boat placedBoat = null;
            double bestD = Double.MAX_VALUE;
            double ppx = panel.player().getWorldX(), ppy = panel.player().getWorldY();
            for (resources.domain.entity.Entity e : panel.world().getEntities()) {
                if (e instanceof resources.domain.object.Boat) {
                    double d = Math.hypot(e.getWorldX() - ppx, e.getWorldY() - ppy);
                    if (d < bestD) { bestD = d; placedBoat = (resources.domain.object.Boat) e; }
                }
            }
            if (placedBoat == null) {
                System.out.println("[Diag][B] no Boat entity found to board");
            } else {
                // Click the boat's actual hitbox center (what entityAt collides against).
                placedBoat.getHitBox().updateCoords();
                java.awt.Rectangle hb = placedBoat.getHitBox();
                double bcx = hb.getCenterX(), bcy = hb.getCenterY();
                System.out.println("[Diag][B] nearest boat hitbox-center=(" + (int) bcx + "," + (int) bcy
                    + ") dist=" + (int) bestD + "px; riding before=" + panel.multiplayer().localPlayerRiding());
                inv.setIndex(0); // empty-ish hand so the click is a board, not a place
                panel.multiplayer().submitWorldClick(bcx, bcy);
                step(panel, 30);
                System.out.println("[Diag][B] riding after boarding click=" + panel.multiplayer().localPlayerRiding());
            }
        }

        panel.stopGameThread();
        System.exit(0);
    }

    /** Scan outward for the nearest water tile center; returns {x,y} or null. */
    private static int[] nearestWater(GamePanel panel, int px, int py) {
        int ts = 64;
        for (int r = 1; r <= 20; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = px + dx * ts;
                    int y = py + dy * ts;
                    resources.domain.tile.Tile t = panel.world().getTile(new java.awt.Point(x, y));
                    String nm = (t == null || t.getName() == null) ? "" : t.getName().toLowerCase();
                    if (nm.contains("water") || nm.contains("ocean")) return new int[] { x, y };
                }
            }
        }
        return null;
    }

    private static int countType(GamePanel panel, String namePart) {
        int n = 0;
        for (resources.domain.entity.Entity e : panel.world().getEntities()) {
            if (e == null || e == panel.player()) continue;
            String nm = e.getName() == null ? "" : e.getName().toLowerCase();
            if (nm.contains(namePart)) n++;
        }
        return n;
    }

    private static void step(GamePanel panel, int frames) throws Exception {
        for (int i = 0; i < frames; i++) {
            long t0 = System.nanoTime();
            panel.update(1.0);
            sleepToFrame(t0);
        }
    }

    private static void sleepToFrame(long t0) throws Exception {
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
        long sleep = 16L - elapsedMs;
        if (sleep > 0) Thread.sleep(sleep);
    }
}
