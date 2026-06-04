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
 * Phase 5.1: a remote guest's interaction is routed through the REAL engine. The guest
 * joins next to a boat and sends an interact command at it; the host must board the boat
 * on the guest's behalf (boat.rider() becomes non-null) via the real ClickRouter.
 * Run: java -cp out resources.testing.probes.RemoteInteractionProbe
 */
public final class RemoteInteractionProbe {

    public static void main(String[] a) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        // Find an unmanned boat in the host world.
        Boat boat = null;
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof Boat && ((Boat) e).rider() == null) { boat = (Boat) e; break; }
        }
        if (boat == null) { System.err.println("FAIL: no unmanned boat in host world"); panel.stopGameThread(); System.exit(1); }
        boat.getHitBox().updateCoords();
        double bx = boat.getHitBox().getCenterX();
        double by = boat.getHitBox().getCenterY();

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        SnapshotCodec codec = new DefaultSnapshotCodec();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, codec, new StableEntityIds());
        int v = config.protocolVersion();

        // Guest joins AT the boat (corner position) so it's within boarding range.
        double gx = boat.getWorldX();
        double gy = boat.getWorldY();
        byte[] join = pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, gx, gy));
        lobby.receive(new ProtocolEnvelope(v, "guest-A", 0L, 0L, 0L, ProtocolMessageType.JOIN, join));
        lobby.tick();

        System.out.println("[Interact] boat riderBefore=" + (boat.rider() == null ? "null" : "set")
            + " at (" + (int) bx + "," + (int) by + ")");

        // Guest clicks the boat to board it.
        ProtocolPayloads.CommandRequest cmd = ProtocolPayloads.CommandRequest.interactAt(bx, by);
        lobby.receive(new ProtocolEnvelope(v, "guest-A", 1L, 0L, 0L,
            ProtocolMessageType.COMMAND, pc.encodeCommand(cmd)));
        lobby.tick();
        lobby.applyInteractions();

        boolean boarded = boat.rider() != null;
        System.out.println("[Interact] boat riderAfter=" + (boarded ? "set (boarded)" : "null"));

        if (!boarded) { System.err.println("FAIL: boat not boarded by remote guest"); panel.stopGameThread(); System.exit(1); }
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }
}
