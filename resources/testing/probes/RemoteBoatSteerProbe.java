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
import resources.world.placement.TileRules;

/**
 * A remote guest boards a boat and steers it through the host engine. Asserts the boat
 * (1) actually moves, (2) stays on water (never lands), and (3) drags the rider with it
 * (the rider is not left behind). This is the host-authoritative fix for the online ship.
 * Run: java -cp out resources.testing.probes.RemoteBoatSteerProbe
 */
public final class RemoteBoatSteerProbe {

    public static void main(String[] xs) throws Exception {
        GamePanel panel = new GamePanel(new JFrame(), true);
        for (int i = 0; i < 30; i++) panel.update(1.0);

        Boat boat = null;
        for (Entity e : panel.world().getEntities()) {
            if (e instanceof Boat && ((Boat) e).rider() == null) { boat = (Boat) e; break; }
        }
        if (boat == null) fail(panel, "no unmanned boat in host world");

        MultiplayerConfig config = MultiplayerConfig.fromSystemProperties();
        ProtocolPayloadCodec pc = new ProtocolPayloadCodec();
        HostAuthoritativeLobby lobby =
            new HostAuthoritativeLobby(panel, config, new DefaultSnapshotCodec(), new StableEntityIds());
        int v = config.protocolVersion();

        double bx0 = boat.getWorldX(), by0 = boat.getWorldY();
        // Guest joins at the boat and boards it.
        lobby.receive(new ProtocolEnvelope(v, "A", 0L, 0L, 0L, ProtocolMessageType.JOIN,
            pc.encodeJoinRequest(new ProtocolPayloads.JoinRequest(true, bx0, by0))));
        lobby.tick();
        boat.getHitBox().updateCoords();
        lobby.receive(new ProtocolEnvelope(v, "A", 1L, 0L, 0L, ProtocolMessageType.COMMAND,
            pc.encodeCommand(ProtocolPayloads.CommandRequest.interactAt(
                boat.getHitBox().getCenterX(), boat.getHitBox().getCenterY()))));
        lobby.tick();
        lobby.applyInteractions();
        boolean boarded = boat.rider() != null;
        if (!boarded) fail(panel, "guest did not board");

        // Steer in several directions for many ticks; record boat path + that it stays on water,
        // and that the rider stays glued to the boat.
        boolean everMoved = false, everOnLand = false, riderEverDetached = false;
        int[][] dirs = { {1,0}, {0,1}, {-1,0}, {0,-1}, {1,1}, {-1,-1} };
        long seq = 2;
        for (int[] d : dirs) {
            for (int step = 0; step < 20; step++) {
                double px = boat.getWorldX(), py = boat.getWorldY();
                ProtocolPayloads.InputState in = new ProtocolPayloads.InputState(
                    d[1] < 0, d[0] < 0, d[1] > 0, d[0] > 0, false, 0, 0); // up,left,down,right
                lobby.receive(new ProtocolEnvelope(v, "A", ++seq, 0L, 0L,
                    ProtocolMessageType.INPUT_STATE, pc.encodeInputState(in)));
                lobby.tick();
                lobby.applyInteractions();
                if (boat.getWorldX() != px || boat.getWorldY() != py) everMoved = true;
                // Boat-on-water check: sample the boat hitbox center tile.
                boat.getHitBox().updateCoords();
                var t = panel.world().getTile(new java.awt.Point(
                    (int) boat.getHitBox().getCenterX(), (int) boat.getHitBox().getCenterY()));
                String tile = (t == null || t.getName() == null) ? "" : t.getName();
                if (!TileRules.isWater(tile)) everOnLand = true;
                // Rider-follow check: rider must stay within the boat footprint.
                var rider = boat.rider();
                if (rider != null) {
                    double rdx = (rider.getWorldX() + rider.getWidth()/2.0) - boat.getHitBox().getCenterX();
                    double rdy = (rider.getWorldY() + rider.getHeight()/2.0) - boat.getHitBox().getCenterY();
                    if (Math.hypot(rdx, rdy) > Math.max(boat.getWidth(), boat.getHeight())) riderEverDetached = true;
                }
            }
        }

        double moved = Math.hypot(boat.getWorldX() - bx0, boat.getWorldY() - by0);
        System.out.println("[BoatSteer] boarded=" + boarded + " everMoved=" + everMoved
            + " netMovedPx=" + (int) moved + " everOnLand=" + everOnLand
            + " riderEverDetached=" + riderEverDetached);

        boolean ok = boarded && everMoved && !everOnLand && !riderEverDetached;
        if (!ok) fail(panel, "boat steer/collision/rider-follow assertions");
        System.out.println("PASS");
        panel.stopGameThread();
        System.exit(0);
    }

    private static void fail(GamePanel panel, String msg) {
        System.err.println("FAIL: " + msg);
        panel.stopGameThread();
        System.exit(1);
    }
}
