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
 * Regression probe for the "hardcoded-host-rider" bug. When a guest boards a boat through
 * the host-authoritative engine, the snapshot's {@code rider} component must carry that
 * guest's playerId, not the literal "host". Otherwise every client sees rider="host" and
 * cannot recognize which player is aboard (ReplicatedWorldState.riderOf /
 * ridingEntityIdFor key off the playerId).
 *
 * <p>Scenario: an unmanned boat reports rider="" (empty). Guest A then boards it; the
 * snapshot delivered to Guest B must report rider="guest-A". Before the fix the rider was
 * always serialized as "host", so the guest-A assertion failed.
 *
 * Run: java -cp out resources.testing.probes.RemoteBoatRiderProbe
 */
public final class RemoteBoatRiderProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        Boat boat = null;
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof Boat && ((Boat) e).rider() == null) { boat = (Boat) e; break; }
        }
        if (boat == null) { fail(panel, "no unmanned boat in host world"); }
        boat.getHitBox().updateCoords();
        double bx = boat.getHitBox().getCenterX();
        double by = boat.getHitBox().getCenterY();

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());
        int v = config.protocolVersion();

        // Both guests join AT the boat corner so the boarder is within boarding range.
        join(lobby, pc, v, "guest-A", boat.getWorldX(), boat.getWorldY());
        join(lobby, pc, v, "guest-B", boat.getWorldX(), boat.getWorldY());
        lobby.tick();

        // Baseline: the unmanned boat must report no rider.
        lobby.produceSnapshots();
        String riderUnmanned = riderOfBoat(lobby, codec, "guest-B");
        System.out.println("[BoatRider] unmanned boat rider seen by B='" + riderUnmanned + "'");
        if (riderUnmanned != null && !riderUnmanned.isEmpty()) {
            fail(panel, "unmanned boat reported rider='" + riderUnmanned + "'");
        }

        // Guest A boards through the real engine.
        interact(lobby, pc, v, "guest-A", 1L, bx, by);
        lobby.tick();
        lobby.applyInteractions();
        if (boat.rider() == null) fail(panel, "boat not boarded by guest-A");

        // The snapshot to Guest B must name the actual boarder (guest-A), not "host".
        lobby.produceSnapshots();
        String riderSeenByB = riderOfBoat(lobby, codec, "guest-B");
        System.out.println("[BoatRider] after A boards, rider seen by B='" + riderSeenByB + "'");
        if ("host".equals(riderSeenByB)) {
            fail(panel, "rider serialized as 'host' instead of the boarding guest's id");
        }
        if (!"guest-A".equals(riderSeenByB)) {
            fail(panel, "expected rider=guest-A but snapshot reported '" + riderSeenByB + "'");
        }

        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    /** The rider component value for the (sole) boat entity in the snapshot sent to recipient,
     *  or null if no boat entity was present in any drained snapshot. */
    private static String riderOfBoat(HostAuthoritativeLobby lobby, SnapshotCodec codec, String recipient) {
        String rider = null;
        boolean sawBoat = false;
        for (ProtocolEnvelope e : lobby.drainFor(recipient)) {
            if (!isSnapshot(e)) continue;
            ProtocolPayloads.Snapshot snap = codec.decode(e.payload());
            for (ProtocolPayloads.EntityStatePayload ent : snap.entities) {
                if (ent == null || ent.removed) continue;
                boolean isBoat = false;
                String r = "";
                for (ProtocolPayloads.ComponentStatePayload c : ent.components) {
                    if (c == null) continue;
                    if ("movement".equals(c.key) && "water_only".equals(c.value)) isBoat = true;
                    if ("rider".equals(c.key)) r = c.value;
                }
                if (isBoat) { sawBoat = true; rider = r; }
            }
        }
        return sawBoat ? rider : null;
    }

    private static void interact(HostAuthoritativeLobby lobby, ProtocolPayloadCodec pc, int v,
                                 String id, long seq, double x, double y) {
        ProtocolPayloads.CommandRequest cmd = ProtocolPayloads.CommandRequest.interactAt(x, y);
        lobby.receive(new ProtocolEnvelope(v, id, seq, 0L, 0L,
            ProtocolMessageType.COMMAND, pc.encodeCommand(cmd)));
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

    private static void fail(GamePanel panel, String why) {
        System.err.println("FAIL: " + why);
        panel.stopGameThread();
        System.exit(1);
    }
}
