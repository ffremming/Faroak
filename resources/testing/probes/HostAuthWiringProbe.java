package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;

/**
 * Verifies the lobby flag wiring end to end on the loopback host path.
 *
 * With -Dgame.multiplayer.lobby=hostauth the host must (a) build a
 * HostAuthoritativeLobby, (b) join, and (c) still render its own locally-generated
 * world (no regression / no wipe). With the flag absent (legacy) the host must NOT
 * build a HostAuthoritativeLobby.
 *
 * Run hostauth: java -Dgame.multiplayer.mode=host -Dgame.multiplayer.backend=loopback \
 *               -Dgame.multiplayer.lobby=hostauth -cp out resources.testing.probes.HostAuthWiringProbe
 * Run legacy:   (same without the lobby property)
 */
public final class HostAuthWiringProbe {

    public static void main(String[] a) throws Exception {
        boolean wantHostAuth = "hostauth".equalsIgnoreCase(System.getProperty("game.multiplayer.lobby", ""));

        JFrame frame = new JFrame();
        GamePanel panel = new GamePanel(frame, true);
        for (int i = 0; i < 150; i++) panel.update(1.0);

        boolean joined = panel.multiplayer() != null && panel.multiplayer().isJoined();
        boolean hostLobbyBuilt = panel.multiplayer() != null && panel.multiplayer().debugHostAuthoritative();
        int worldEntities = panel.world() == null ? -1 : panel.world().getEntities().size();

        System.out.println("[Wiring] wantHostAuth=" + wantHostAuth
            + " joined=" + joined
            + " hostLobbyBuilt=" + hostLobbyBuilt
            + " worldEntities=" + worldEntities);

        boolean ok;
        if (wantHostAuth) {
            ok = joined && hostLobbyBuilt && worldEntities > 0;
        } else {
            ok = joined && !hostLobbyBuilt && worldEntities > 0;
        }
        if (!ok) { System.err.println("FAIL"); panel.stopGameThread(); System.exit(1); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }
}
