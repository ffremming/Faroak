package resources.testing;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.domain.object.Boat;

/**
 * Focused probe for the online "can't board / interactions don't work" bug.
 *
 * Boots a HOST client (embedded loopback server), finds the nearest replicated
 * Boat, and reports the EXACT facts that decide whether boarding works:
 *   - is the clicked boat a SERVER-replicated entity (has an entityId > 0)?
 *   - what entityId does the client resolve for it?
 *   - does riding engage after the board click?
 *
 * Run: java -cp out resources.testing.BoatBoardProbe
 */
public final class BoatBoardProbe {

    public static void main(String[] args) throws Exception {
        System.setProperty("game.multiplayer.mode", "host");
        System.setProperty("game.multiplayer.backend", "loopback");
        System.setProperty("game.multiplayer.serverActionRange", "768.0");

        JFrame frame = new JFrame();
        GamePanel panel = new GamePanel(frame, true);
        step(panel, 120);
        System.out.println("[Boat] joined=" + panel.multiplayer().isJoined());

        // Find nearest Boat in the client world.
        double ppx = panel.player().getWorldX(), ppy = panel.player().getWorldY();
        Boat nearest = null;
        double bestD = Double.MAX_VALUE;
        int totalBoats = 0;
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof Boat) {
                totalBoats++;
                double d = Math.hypot(e.getWorldX() - ppx, e.getWorldY() - ppy);
                if (d < bestD) { bestD = d; nearest = (Boat) e; }
            }
        }
        System.out.println("[Boat] total client-side boats=" + totalBoats);
        if (nearest == null) { System.out.println("[Boat] no boat to test"); panel.stopGameThread(); System.exit(0); }

        // Dump EVERY client boat: its resolved server id + server state. This reveals
        // which boats the server considers occupied and whether ids map at all.
        System.out.println("[Boat] --- all client boats ---");
        for (Entity e : panel.world().getEntities()) {
            if (!(e instanceof Boat)) continue;
            long id = panel.multiplayer().debugEntityId(e);
            System.out.println("[Boat]   name=" + e.getName()
                + " pos=(" + (int) e.getWorldX() + "," + (int) e.getWorldY() + ")"
                + " serverId=" + id + " | " + panel.multiplayer().debugEntityState(id));
        }

        long serverId = panel.multiplayer().debugEntityId(nearest);
        nearest.getHitBox().updateCoords();
        java.awt.Rectangle hb = nearest.getHitBox();
        System.out.println("[Boat] nearest boat: name=" + nearest.getName()
            + " kind=" + (nearest.kind() == null ? "?" : nearest.kind())
            + " serverEntityId=" + serverId + (serverId <= 0 ? "  <-- CLIENT-ONLY, server can't see it" : "")
            + " dist=" + (int) bestD + "px"
            + " hitboxCenter=(" + (int) hb.getCenterX() + "," + (int) hb.getCenterY() + ")");

        // Try to board it.
        panel.player().getInventory().setIndex(0);
        System.out.println("[Boat] server state: " + panel.multiplayer().debugEntityState(serverId));
        System.out.println("[Boat] riding before=" + panel.multiplayer().localPlayerRiding());
        boolean submitted = panel.multiplayer().submitWorldClick(hb.getCenterX(), hb.getCenterY());
        System.out.println("[Boat] board click submitted=" + submitted);
        step(panel, 60);
        boolean riding = panel.multiplayer().localPlayerRiding();
        System.out.println("[Boat] riding after=" + riding);
        System.out.println("[Boat] last command reason=" + panel.multiplayer().debugLastCommandReason());

        if (!riding) {
            System.err.println("FAIL: not riding after board click; reason="
                + panel.multiplayer().debugLastCommandReason());
            panel.stopGameThread();
            System.exit(1);
        }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static void step(GamePanel panel, int frames) throws Exception {
        for (int i = 0; i < frames; i++) {
            long t0 = System.nanoTime();
            panel.update(1.0);
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            if (16L - ms > 0) Thread.sleep(16L - ms);
        }
    }
}
