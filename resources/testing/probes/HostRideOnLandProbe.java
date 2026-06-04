package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.domain.object.Boat;
import resources.world.placement.TileRules;

/**
 * Reproduces the HOST riding path online (the host boards a real boat like offline, then
 * steers via keyboard input through Boat.update()->steerByInput). Drives the boat hard in
 * all directions and checks whether it ever sits on a non-water tile — i.e. "ride on land".
 *
 * Boots a real HOST client with the hostauth lobby (the shipping default), so the full
 * frame loop (world.simulate + multiplayer.update + snapshots) runs exactly as in play.
 * Run: java -cp out resources.testing.probes.HostRideOnLandProbe
 */
public final class HostRideOnLandProbe {

    public static void main(String[] a) throws Exception {
        System.setProperty("game.multiplayer.mode", "host");
        System.setProperty("game.multiplayer.backend", "loopback");

        GamePanel panel = new GamePanel(new JFrame(), true);
        step(panel, 120); // connect + baseline

        // Find the nearest boat; if none nearby, spawn one on the nearest water tile.
        Boat boat = nearestBoat(panel);
        if (boat == null) {
            int[] w = nearestWater(panel, (int) panel.player().getWorldX(), (int) panel.player().getWorldY());
            if (w == null) { System.err.println("FAIL: no water near host to place a boat"); panel.stopGameThread(); System.exit(1); }
            boat = new Boat(panel, w[0], w[1]);
            boolean placed = panel.world().placeShipOnWater(boat);
            System.out.println("[HostRide] spawned boat on water at (" + w[0] + "," + w[1] + ") placed=" + placed);
            step(panel, 5);
        }
        boat.getHitBox().updateCoords();
        // Teleport the host player onto the boat and board via click.
        panel.player().setWorldX(boat.getWorldX());
        panel.player().setWorldY(boat.getWorldY());
        panel.player().getHitBox().updateCoords();
        boolean boarded = boat.tryBoardFromClick(panel.player());
        System.out.println("[HostRide] boarded=" + boarded + " ridingOnline=" + panel.multiplayer().localPlayerRiding());

        // Steer in every direction for a while; record whether the boat ever lands.
        boolean everOnLand = false;
        int landFrames = 0;
        int[][] dirs = { {1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1} };
        for (int[] d : dirs) {
            panel.inputHandlingSystem.setRight(d[0] > 0);
            panel.inputHandlingSystem.setLeft(d[0] < 0);
            panel.inputHandlingSystem.setDown(d[1] > 0);
            panel.inputHandlingSystem.setUp(d[1] < 0);
            for (int i = 0; i < 40; i++) {
                panel.update(1.0);
                boat.getHitBox().updateCoords();
                String tile = tileAt(panel, (int) boat.getHitBox().getCenterX(), (int) boat.getHitBox().getCenterY());
                if (!TileRules.isWater(tile)) { everOnLand = true; landFrames++; }
            }
        }
        panel.inputHandlingSystem.setRight(false); panel.inputHandlingSystem.setLeft(false);
        panel.inputHandlingSystem.setDown(false); panel.inputHandlingSystem.setUp(false);

        System.out.println("[HostRide] everOnLand=" + everOnLand + " landFrames=" + landFrames
            + " finalTile=" + tileAt(panel, (int) boat.getHitBox().getCenterX(), (int) boat.getHitBox().getCenterY()));
        // We EXPECT everOnLand=false for correct behavior. Report regardless (diagnostic).
        System.out.println(everOnLand ? "RIDES-ON-LAND (bug reproduced)" : "stays-on-water");
        panel.stopGameThread();
        System.exit(0);
    }

    private static Boat nearestBoat(GamePanel panel) {
        Boat best = null; double bd = Double.MAX_VALUE;
        double px = panel.player().getWorldX(), py = panel.player().getWorldY();
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof Boat) {
                double d = Math.hypot(e.getWorldX() - px, e.getWorldY() - py);
                if (d < bd) { bd = d; best = (Boat) e; }
            }
        }
        return best;
    }

    /** Scan outward (tile grid) for the nearest water tile center; {x,y} or null. */
    private static int[] nearestWater(GamePanel panel, int px, int py) {
        int ts = 64;
        for (int r = 1; r <= 30; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    int x = px + dx * ts, y = py + dy * ts;
                    if (TileRules.isWater(tileAt(panel, x, y))) return new int[] { x, y };
                }
            }
        }
        return null;
    }

    private static String tileAt(GamePanel panel, int x, int y) {
        var t = panel.world().getTile(new java.awt.Point(x, y));
        return (t == null || t.getName() == null) ? "" : t.getName();
    }

    private static void step(GamePanel panel, int frames) throws Exception {
        for (int i = 0; i < frames; i++) { panel.update(1.0); Thread.sleep(2); }
    }
}
