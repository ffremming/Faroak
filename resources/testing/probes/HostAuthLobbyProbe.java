package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.hostauth.HostAuthoritativeLobby;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.codec.SnapshotCodec;

import java.util.List;

/**
 * Verifies HostAuthoritativeLobby: a guest that JOINs receives a WELCOME and then a
 * BASELINE_SNAPSHOT whose decoded entity list is populated from the real host engine.
 * Run: java -cp out resources.testing.probes.HostAuthLobbyProbe
 */
public final class HostAuthLobbyProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        SnapshotCodec codec = new DefaultSnapshotCodec();
        HostAuthoritativeLobby lobby = new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());

        String guest = "guest-1";
        int v = config.protocolVersion();
        lobby.receive(new ProtocolEnvelope(v, guest, 0L, 0L, 0L, ProtocolMessageType.JOIN, new byte[0]));
        lobby.tick();             // process JOIN -> WELCOME queued
        lobby.produceSnapshots(); // host-frame: build + queue baseline

        List<ProtocolEnvelope> out = lobby.drainFor(guest);
        boolean sawWelcome = false, sawBaseline = false;
        int entities = -1;
        for (ProtocolEnvelope e : out) {
            if (ProtocolMessageType.WELCOME.equals(e.messageType())) sawWelcome = true;
            if (ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())) {
                sawBaseline = true;
                ProtocolPayloads.Snapshot snap = codec.decode(e.payload());
                entities = snap.entities == null ? 0 : snap.entities.size();
            }
        }
        System.out.println("[HostLobby] welcome=" + sawWelcome
            + " baseline=" + sawBaseline + " entities=" + entities);

        boolean ok = sawWelcome && sawBaseline && entities > 0;
        if (!ok) { System.err.println("FAIL"); panel.stopGameThread(); System.exit(1); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }
}
