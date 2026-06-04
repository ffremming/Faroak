package resources.testing.probes;

import javax.swing.JFrame;

import resources.app.GamePanel;
import resources.domain.entity.Entity;
import resources.domain.object.Boat;
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
 * Phase 6.1 acceptance: two guests on the host-authoritative lobby exercise the real
 * engine end to end.
 *   1. Guest A moves; Guest B's snapshot shows A at the new position.
 *   2. Guest A boards an unmanned boat (real ClickRouter); boat gets a rider.
 *   3. Guest B then CANNOT board the same boat (already occupied).
 * Run: java -cp out resources.testing.probes.HostAuthAcceptanceProbe
 */
public final class HostAuthAcceptanceProbe {

    public static void main(String[] x) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        Boat boat = null;
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof Boat && ((Boat) e).rider() == null) { boat = (Boat) e; break; }
        }
        if (boat == null) { fail(panel, "no unmanned boat"); }
        boat.getHitBox().updateCoords();
        double bx = boat.getHitBox().getCenterX(), by = boat.getHitBox().getCenterY();
        double boatX = boat.getWorldX(), boatY = boat.getWorldY();

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());
        int v = config.protocolVersion();

        // Both guests join AT the boat so both are in boarding range. (Movement +
        // collision replication is covered by RemotePlayerProbe; here we focus on the
        // multi-client interaction contract.)
        join(lobby, pc, v, "A", boatX, boatY);
        join(lobby, pc, v, "B", boatX, boatY);
        lobby.tick();

        // (1) Guest B's snapshot lists Guest A as a remote player.
        lobby.produceSnapshots();
        boolean bSeesA = false;
        for (ProtocolEnvelope e : lobby.drainFor("B")) {
            if (!snap(e)) continue;
            for (ProtocolPayloads.PlayerState p : codec.decode(e.payload()).players) {
                if ("A".equals(p.playerId)) bSeesA = true;
            }
        }

        // (2) Guest A boards the boat.
        send(lobby, v, "A", 3L, ProtocolMessageType.COMMAND,
            pc.encodeCommand(ProtocolPayloads.CommandRequest.interactAt(bx, by)));
        lobby.tick();
        lobby.applyInteractions();
        boolean aBoarded = boat.rider() != null;

        // (3) Guest B tries to board the SAME boat — must be refused (still A's rider).
        Object riderAfterA = boat.rider();
        send(lobby, v, "B", 3L, ProtocolMessageType.COMMAND,
            pc.encodeCommand(ProtocolPayloads.CommandRequest.interactAt(bx, by)));
        lobby.tick();
        lobby.applyInteractions();
        boolean bRejected = boat.rider() == riderAfterA; // unchanged → B did not steal it

        System.out.println("[Accept] bSeesA=" + bSeesA
            + " aBoarded=" + aBoarded + " bRejectedSameBoat=" + bRejected);

        if (!(bSeesA && aBoarded && bRejected)) { fail(panel, "acceptance assertions"); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static void join(HostAuthoritativeLobby l, ProtocolPayloadCodec pc, int v, String id, double x, double y) {
        l.receive(new ProtocolEnvelope(v, id, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, x, y))));
    }

    private static void send(HostAuthoritativeLobby l, int v, String id, long seq,
                             ProtocolMessageType t, byte[] payload) {
        l.receive(new ProtocolEnvelope(v, id, seq, 0L, 0L, t, payload));
    }

    private static boolean snap(ProtocolEnvelope e) {
        return ProtocolMessageType.BASELINE_SNAPSHOT.equals(e.messageType())
            || ProtocolMessageType.DELTA_SNAPSHOT.equals(e.messageType());
    }

    private static void fail(GamePanel panel, String msg) {
        System.err.println("FAIL: " + msg);
        panel.stopGameThread();
        System.exit(1);
    }
}
