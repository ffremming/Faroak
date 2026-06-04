package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.hostauth.HostAuthoritativeLobby;
import resources.net.multiplayer.hostauth.StableEntityIds;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.codec.SnapshotCodec;

/**
 * Phase 4: two guests join the host-authoritative lobby. Guest A reports movement;
 * the host adopts it (collision-checked) and Guest B's snapshot must list Guest A as
 * a remote player at the moved position.
 * Run: java -cp out resources.testing.probes.RemotePlayerProbe
 */
public final class RemotePlayerProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());
        int v = config.protocolVersion();

        // Spawn both guests on the host's known-good land position.
        double sx = panel.player().getWorldX();
        double sy = panel.player().getWorldY();
        join(lobby, pc, v, "guest-A", sx, sy);
        join(lobby, pc, v, "guest-B", sx, sy);
        lobby.tick();

        // Guest A moves right by 32px (reports its own position, client-authoritative).
        ProtocolPayloads.InputState mv = new ProtocolPayloads.InputState(
            false, false, false, true, true, sx + 32.0, sy);
        lobby.receive(new ProtocolEnvelope(v, "guest-A", 1L, 0L, 0L,
            ProtocolMessageType.INPUT_STATE, pc.encodeInputState(mv)));
        lobby.tick();
        // INPUT_STATE is buffered on tick() (server thread) and applied on the host frame
        // thread in applyInteractions() to avoid racing world.simulate(). Drive that here.
        lobby.applyInteractions();
        lobby.produceSnapshots();

        // Inspect what Guest B receives.
        ProtocolPayloads.PlayerState seenA = null;
        for (ProtocolEnvelope e : lobby.drainFor("guest-B")) {
            if (!isSnapshot(e)) continue;
            ProtocolPayloads.Snapshot snap = codec.decode(e.payload());
            for (ProtocolPayloads.PlayerState p : snap.players) {
                if ("guest-A".equals(p.playerId)) seenA = p;
            }
        }

        boolean present = seenA != null;
        boolean moved = present && Math.abs(seenA.worldX - (sx + 32.0)) < 1.0;
        System.out.println("[Remote] guestA seen by B=" + present
            + (present ? " at x=" + (int) seenA.worldX + " (want " + (int) (sx + 32) + ")" : "")
            + " moved=" + moved);

        boolean ok = present && moved;
        if (!ok) { System.err.println("FAIL"); panel.stopGameThread(); System.exit(1); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static void join(HostAuthoritativeLobby lobby, ProtocolPayloadCodec pc, int v,
                             String id, double x, double y) {
        byte[] join = pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, x, y));
        lobby.receive(new ProtocolEnvelope(v, id, 0L, 0L, 0L, ProtocolMessageType.JOIN, join));
    }

    private static boolean isSnapshot(ProtocolEnvelope e) {
        return ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
            || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType());
    }
}
